package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.worker.UploadWorker
import at.ac.tuwien.caa.docscan.api.transkribus.TranskribusAPIService
import at.ac.tuwien.caa.docscan.api.transkribus.mapToMultiPartBody
import at.ac.tuwien.caa.docscan.api.transkribus.model.collection.CollectionResponse
import at.ac.tuwien.caa.docscan.api.transkribus.model.collection.DocResponse
import at.ac.tuwien.caa.docscan.api.transkribus.model.uploads.*
import at.ac.tuwien.caa.docscan.db.model.*
import at.ac.tuwien.caa.docscan.db.model.Page
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*

/**
 * An upload repository dealing with the upload of the document and pages.
 *
 * TODO: UPLOAD_CONSTRAINT: A worker might be scheduled, but not running (e.g. if the is running on mobile data, but has not opted in for that) - is a scheduled state necessary?
 * TODO: UPLOAD_CONSTRAINT: If scheduled state is not added to the DB, then this cannot be tracked in the app, the upload could suddenly start if the user isn't aware of that.
 * TODO: UPLOAD_CONSTRAINT: The current checksum is sent for each image, so if an upload should suddenly, then it would fail if one of the images has been changed/added/removed.
 * TODO: UPLOAD_CONSTRAINT: Should the document be locked if it's just scheduled? (currently it's locked when the upload is started, but then it's unlocked, if the worker is retried then the doc is not locked anymore!
 */
class UploadRepository(
    private val api: TranskribusAPIService,
    private val preferencesHandler: PreferencesHandler,
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    private val fileHandler: FileHandler
) {

    /**
     * Uploads a document with all its pages, call this function only from the [UploadWorker].
     *
     * Pre-Conditions:
     * - The doc has to be entirely locked to prevent any unwanted manipulations.
     *
     * Post-Conditions:
     * - The doc will be only unlocked, if this entire operation is successful or there is an
     * unrecoverable error.
     */
    suspend fun uploadDocument(documentId: UUID): Resource<UploadStatusResponse> {
        try {
            val resource = uploadDocumentInternal(documentId)
            when (resource) {
                is Failure -> {
                    withContext(NonCancellable) {
                        tearDownUpload(
                            documentId,
                            if (resource.isUploadRecoverable()) UploadResourceState.RECOVERABLE_FAILURE else UploadResourceState.FAILURE
                        )
                    }
                }
                is Success -> {
                    // ignore
                }
            }
            return resource
        } catch (e: CancellationException) {
            Timber.i("The upload job has been cancelled - tearing down upload!")
            withContext(NonCancellable) {
                tearDownUpload(documentId, UploadResourceState.FAILURE)
            }
            throw e
        }
    }

    private suspend fun uploadDocumentInternal(documentId: UUID): Resource<UploadStatusResponse> {
        Timber.d("Starting upload for $documentId")
        // 1. Retrieve the current document with its pages.
        val documentWithPages = documentDao.getDocumentWithPages(documentId) ?: kotlin.run {
            return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        }
        // 2. obtain the collection id, where the document will be added.
        val collectionId = run {
            when (val resource = getCollectionId()) {
                is Success -> {
                    resource.data
                }
                is Failure -> {
                    return Failure(resource.exception)
                }
            }
        }
        val uploadCreateResource =
            prepareForUpload(collectionId, documentWithPages)
        // 3. check current upload id
        val uploadStatusResponse =
            when (uploadCreateResource) {
                is Failure -> {
                    val docScanError =
                        (uploadCreateResource.exception as? DocScanException)?.docScanError
                            ?: return Failure(uploadCreateResource.exception)
                    return when (docScanError) {
                        is DocScanError.DBError -> {
                            Failure(uploadCreateResource.exception)
                        }
                        else -> {
                            Failure(uploadCreateResource.exception)
                        }
                    }
                }
                is Success -> {
                    uploadCreateResource.data
                }
            }

        // set all pages to uploading
        documentWithPages.pages.forEach {
            pageDao.updateUploadState(it.id, UploadState.UPLOAD_IN_PROGRESS)
        }

        val pagesToUpload = when (val expectationCheckResult =
            checkUploadExpectations(documentId, uploadStatusResponse)) {
            is Failure -> {
                return Failure(expectationCheckResult.exception)
            }
            is Success -> {
                // update the uploadId in the doc
                documentDao.updateUploadIdForDoc(
                    documentId = documentWithPages.document.id,
                    uploadStatusResponse.uploadId
                )
                expectationCheckResult.data
            }
        }
        var lastUploadStatusResponse = uploadStatusResponse

        // update the upload state for already uploaded pages
        pagesToUpload.filter { page -> page.uploadStatus.pageUploaded }.forEach {
            pageDao.updateUploadState(it.page.id, UploadState.UPLOADED)
        }

        for (page in pagesToUpload) {
            if (page.uploadStatus.pageUploaded) {
                continue
            }
            val uploadResource: Resource<UploadStatusResponse> =
                transkribusResource(apiCall =
                {
                    api.uploadFile(
                        uploadStatusResponse.uploadId,
                        mapToMultiPartBody(page.uploadStatus.fileName, page.file)
                    )
                })

            when (uploadResource) {
                is Failure -> {
                    return Failure(uploadResource.exception)
                }
                is Success -> {
                    pageDao.updateUploadState(page.page.id, UploadState.UPLOADED)
                    lastUploadStatusResponse = uploadResource.data
                }
            }
        }

        // tear down the upload
        tearDownUpload(documentId, UploadResourceState.SUCCESS)
        return Success(lastUploadStatusResponse)
    }

    private data class UploadPageWrapper(
        val page: Page,
        val file: File,
        val uploadStatus: UploadStatusPage
    )

    /**
     * Prepares the pages for the upload. Since the upload needs to be registered first
     */
    private suspend fun prepareForUpload(
        collectionId: Int,
        documentWithPages: DocumentWithPages
    ): Resource<UploadStatusResponse> {
        documentWithPages.document.uploadId?.let {
            val resource: Resource<UploadStatusResponse> = transkribusResource(apiCall = {
                api.getUploadStatus(it)
            })
            when (resource) {
                is Success -> {
                    return resource
                }
                is Failure -> {
                    // ignore
                }
            }
            // in case of an error, check if the document has not been already completely uploaded
            val docResource: Resource<DocResponse> = transkribusResource(apiCall = {
                api.getDocById(collectionId, it)
            })
            when (docResource) {
                is Success -> {
                    return DBErrorCode.DOCUMENT_ALREADY_UPLOADED.asFailure()
                }
                is Failure -> {
                    // if the failure is a 404, then this is an expected error, since we have ensured
                    // that the doc is neither fully uploaded, nor partially uploaded.
                    if (!docResource.exception.is404()) {
                        return Failure(docResource.exception)
                    }
                }
            }
        }

        // prepare pages for upload
        val uploadPages = mutableListOf<UploadPage>()
        for ((index, page) in documentWithPages.pages.sortedBy { it.index }.withIndex()) {
            val pageNr = index + 1
            val checkSum = if (page.fileHash.isNotEmpty()) page.fileHash else null
            val fileName = documentWithPages.document.getFileName(pageNr, page.fileType)

            // add to the list
            uploadPages.add(
                UploadPage(
                    fileName = fileName,
                    pageNr = pageNr,
                    imgChecksum = checkSum
                )
            )

            // save the file name (the upload state doesn't really matter at this place
            val upload = Upload(state = UploadState.UPLOAD_IN_PROGRESS, uploadFileName = fileName)
            page.transkribusUpload = upload
            pageDao.insertPage(page)
        }

        // create the meta data upload object
        val uploadMetaData =
            documentWithPages.document.metaData?.toUploadMetaData(documentWithPages.document.title)
                ?: UploadMetaData(title = documentWithPages.document.title)

        // create a new upload page on the transkribus backend
        val resource: Resource<UploadStatusResponse> = transkribusResource(apiCall = {
            api.createUpload(
                collectionId,
                CreateUploadRequestBody(
                    uploadMetaData,
                    UploadPageList(pages = uploadPages)
                )
            )
        })
        return resource
    }

    /**
     * @return a resource with the collection id where the documents should be assigned.
     * TODO: log the collection operation failures
     * Every document must belong to a collection, however, from the client's perspective, there is
     * just a single collection called [TranskribusAPIService.TRANSKRIBUS_UPLOAD_COLLECTION_NAME], so
     * this logic will try to get the collectionId and only create a new collection if necessary.
     */
    private suspend fun getCollectionId(): Resource<Int> {
        preferencesHandler.collectionId?.let {
            return Success(it)
        }
        val collections: Resource<List<CollectionResponse>> = transkribusResource(apiCall = {
            api.getCollections()
        })
        when (collections) {
            is Success -> {
                // select highest id with the matching name
                collections.data.sortedBy { it.id }.lastOrNull {
                    it.name == TranskribusAPIService.TRANSKRIBUS_UPLOAD_COLLECTION_NAME
                }?.let {
                    return Success(it.id)
                }
            }
            is Failure -> {
                return Failure(collections.exception)
            }
        }
        // if the collectionId cannot be found, then a new one is created.
        val newCollection: Resource<String> = transkribusResource(apiCall = {
            api.createCollection(TranskribusAPIService.TRANSKRIBUS_UPLOAD_COLLECTION_NAME)
        })
        return when (newCollection) {
            is Success -> {
                Success(newCollection.data.toInt())
            }
            is Failure -> {
                Failure(newCollection.exception)
            }
        }
    }

    /**
     * Checks upload expectations, every upload has to be created first before files are going to be
     * uploaded. The BE returns a status of all pages, this needs to correspond with our internal
     * structure, otherwise, the document's pages have been manipulated.
     *
     * The checks are performed on the:
     * - Unique file name of the page.
     * - The page number.
     */
    private suspend fun checkUploadExpectations(
        documentId: UUID,
        statusResponse: UploadStatusResponse
    ): Resource<List<UploadPageWrapper>> {
        val docWithPages = documentDao.getDocumentWithPages(documentId)
            ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        val pagesToUpload = mutableListOf<UploadPageWrapper>()
        for ((index, page) in docWithPages.pages.sortedBy { it.index }.withIndex()) {
            val uploadStatusPage = statusResponse.pageList.pages.firstOrNull { uploadStatusPage ->
                uploadStatusPage.fileName == page.transkribusUpload.uploadFileName &&
                        uploadStatusPage.pageNr == (index + 1) &&
                        // if the checksum is returned by the backend, then its checked with our local checksum to detect inconsistencies,
                        // if it's null, then we haven't sent the checksum during init
                        (if (uploadStatusPage.imgChecksum != null) uploadStatusPage.imgChecksum == page.fileHash else true)
            } ?: kotlin.run {
                Timber.e("Inconsistencies in the upload expectations!")
                return DBErrorCode.DOCUMENT_DIFFERENT_UPLOAD_EXPECTATIONS.asFailure()
            }
            val file = fileHandler.getFileByPage(page)
            if (file == null || !file.exists()) {
                Timber.e("File for page:${page.id} cannot be found!")
                return DBErrorCode.DOCUMENT_PAGE_FILE_FOR_UPLOAD_MISSING.asFailure()
            }
            pagesToUpload.add(UploadPageWrapper(page, file, uploadStatusPage))
        }
        return Success(pagesToUpload)
    }

    /**
     * Tears down an upload for a document.
     * @param uploadNonRecoverableFailure if true, then this tears down operation is performed in a way
     * so that the entire association with the upload job is removed.
     */
    private suspend fun tearDownUpload(
        documentId: UUID,
        uploadResourceState: UploadResourceState
    ) {
        Timber.d("Tearing down upload!")
        pageDao.updateUploadStateForDocument(
            documentId,
            if (uploadResourceState == UploadResourceState.SUCCESS) UploadState.UPLOADED else UploadState.NONE
        )
        unLockDocAfterLongRunningOperation(documentId)

        // delete the associated upload if something goes terribly wrong
        if (uploadResourceState == UploadResourceState.FAILURE) {
            // load the document again, since this may be an old reference
            documentDao.getDocument(documentId)?.uploadId?.let {
                Timber.d("Trying to delete uploadId $it on transkribus server!")
                when (val result = transkribusResource<Void, Void>(apiCall = {
                    api.deleteUpload(it)
                })) {
                    is Failure -> {
                        Timber.d("Deleting upload id $it has failed!")
                    }
                    is Success -> {
                        Timber.d("Successfully deleted upload id $it!")
                    }
                }
            }
        }
        // if the failure is recoverable, then we want to keep the uploadId and the file names
        if (uploadResourceState != UploadResourceState.RECOVERABLE_FAILURE) {
            // clear the uploadId from the doc
            documentDao.updateUploadIdForDoc(documentId, null)
            // clear the upload file names
            pageDao.clearDocumentPagesUploadFileNames(documentId)
        }
        Timber.d("Tearing down upload ended!")
    }

    enum class UploadResourceState {
        SUCCESS,
        RECOVERABLE_FAILURE,
        FAILURE
    }
}
