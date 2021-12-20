package at.ac.tuwien.caa.docscan.repository

import androidx.work.*
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Upload
import at.ac.tuwien.caa.docscan.db.model.isUploadInProgress
import at.ac.tuwien.caa.docscan.db.model.state.UploadState
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.sync.UploadWorker
import at.ac.tuwien.caa.docscan.sync.transkribus.TranskribusAPIService
import at.ac.tuwien.caa.docscan.sync.transkribus.mapToMultiPartBody
import at.ac.tuwien.caa.docscan.sync.transkribus.model.collection.CollectionResponse
import at.ac.tuwien.caa.docscan.sync.transkribus.model.collection.DocResponse
import at.ac.tuwien.caa.docscan.sync.transkribus.model.uploads.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Exception

class UploadRepository(
    private val api: TranskribusAPIService,
    private val preferencesHandler: PreferencesHandler,
    private val documentDao: DocumentDao,
    private val pageDao: PageDao,
    private val fileHandler: FileHandler
) {

    suspend fun initUploadWorker() {

    }

    // TODO: Return error/success only if some of
    suspend fun uploadPendingDocuments(): Resource<Unit> {
        val docsForUpload = documentDao.getAllDocumentWithPages().filter { documentWithPages ->
            documentWithPages.isUploadInProgress()
        }

        docsForUpload.forEach { docWithPages ->
            when(val resource = uploadDocument(docWithPages.document.id)){
                is Failure -> {
                    return Failure(resource.exception)
                }
                is Success -> {
                    // continue with next upload
                }
            }
        }

        return Success(Unit)
    }

    private suspend fun uploadDocument(documentId: UUID): Resource<UploadStatusResponse> {
        // 1. lock the document
        val documentWithPages = documentDao.getDocumentWithPages(documentId) ?: kotlin.run {
            // TODO: Could only occur if the doc has been meanwhile deleted.
            return Failure(Exception())
        }

        val pages = documentWithPages.pages.sortedBy { it.number }

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
        // 3. check current upload ig
        val uploadStatusResponse =
            when (uploadCreateResource) {
                is Failure -> {
                    return Failure(uploadCreateResource.exception)
                }
                is Success -> {
                    uploadCreateResource.data
                }
            }

        var lastUploadStatusResponse: Resource<UploadStatusResponse> = uploadCreateResource
        uploadStatusResponse.pageList.pages.forEach { pageStatus ->

            // TODO: Check for mainly not uploaded files

            // obtain the current page from the DB to get the corresponding file
            val page =
                documentDao.getDocumentWithPages(documentWithPages.document.id)?.pages?.find { page -> page.transkribusUpload.uploadFileName == pageStatus.fileName }
            val file = fileHandler.getFileByPage(page)
            if (file == null) {
                // TODO: Throw an error here
                return Failure(Exception())
            } else {
                val uploadResource: Resource<UploadStatusResponse> =
                    transkribusResource(apiCall =
                    {
                        api.uploadFile(
                            uploadStatusResponse.uploadId,
                            mapToMultiPartBody(pageStatus.fileName, file)
                        )
                    })

                when (uploadResource) {
                    is Failure -> {
                        // TODO: Stop entirely and return error
                        return Failure(uploadResource.exception)
                    }
                    is Success -> {
                        lastUploadStatusResponse = uploadResource
                        // TODO: Update file that has been already uploaded.
                    }
                }
            }
        }

        return lastUploadStatusResponse
    }

    // TODO: Handle the case, when the user wants to start a completely new upload (even if some data has been partially uploaded)
    // TODO: Handle the case, when the server returns a 404 for the uploadId, for which the document has already been uploaded completely, pull the document instead!
    private suspend fun prepareForUpload(
        collectionId: Int,
        documentWithPages: DocumentWithPages
    ): Resource<UploadStatusResponse> {
        // TODO: relatedUploadId in the meta files and the real upload id.
        var uploadId: Int? = null
        uploadId = 123
        uploadId.let {
            val resource: Resource<UploadStatusResponse> = transkribusResource(apiCall = {
                api.getUploadStatus(uploadId)
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
                    // TODO: Add an error here, that the doc with the uploadId is already uploaded, the question is what should be done in this case? (E.g. drop the associated uploadId and start from scratch?)
                    return Failure(Exception())
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
        val docTitle = documentWithPages.document.title.replace(" ", "").lowercase()
        val uploadPages = mutableListOf<UploadPage>()
        for ((index, page) in documentWithPages.pages.sortedBy { it.number }.withIndex()) {
            val pageNr = index + 1
            val checkSum = if (page.fileHash.isNotEmpty()) page.fileHash else null
            val fileName = "${docTitle}_${pageNr}.${FileType.JPEG.extension}"
            uploadPages.add(
                UploadPage(
                    fileName = fileName,
                    pageNr = pageNr,
                    imgChecksum = checkSum
                )
            )

            // update the state
            val upload = Upload(state = UploadState.UPLOAD_IN_PROGRESS, uploadFileName = fileName)
            page.transkribusUpload = upload
            pageDao.insertPage(page)
        }
        // save the values in the DB
        val uploadMetaData =
            documentWithPages.document.metaData?.toUploadMetaData(documentWithPages.document.title)
                ?: UploadMetaData(title = documentWithPages.document.title)

        // create a new upload page
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
}