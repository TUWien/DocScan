package at.ac.tuwien.caa.docscan.repository

import androidx.room.withTransaction
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.Mapper
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.*
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary.Companion.getDefault
import at.ac.tuwien.caa.docscan.db.model.boundary.asClockwiseList
import at.ac.tuwien.caa.docscan.db.model.boundary.asPoint
import at.ac.tuwien.caa.docscan.db.model.boundary.rotateBy90
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.util.*

class ImageProcessorRepository(
    private val pageDao: PageDao,
    private val documentDao: DocumentDao,
    private val fileHandler: FileHandler,
    private val appDatabase: AppDatabase
) {

    /**
     * An own image processor scope, on which long running operations are launched
     * to outlive the UI-lifecycle/viewModel scope.
     */
    private val scope = CoroutineScope(Dispatchers.IO)

    fun rotateFile(file: File, rotation: Rotation) {
        applyRotation(file, rotation)
    }

    @Suppress("unused")
    fun removeRotationExif(file: File) {
        removeRotation(file)
    }

    /**
     * Pre-Condition: The page is not locked.
     *
     * Spawns a page detection on a [Page] to estimate cropping points.s
     *
     * Post-Condition: The page will be unlocked.
     */
    fun spawnPageDetection(page: Page) {
        scope.launch {
            pageDao.updatePageProcessingState(page.id, PostProcessingState.PROCESSING)
            pageImageOperation(
                pageId = page.id,
                preOperation = { defaultPrePageOperation(page.id) },
                imageOperation = { _, file ->
                    try {
                        val result = PageDetector.findRectAndFocus(file.absolutePath)
                        if (result.points.size > 3) {
                            Success(result.points.toSinglePageBoundary())
                        } else {
                            IOErrorCode.SINGLE_PAGE_DETECTION_FAILED.asFailure()
                        }
                    } catch (e: Exception) {
                        Timber.w("Page detection has failed!", e)
                        IOErrorCode.SINGLE_PAGE_DETECTION_FAILED.asFailure(e)
                    }
                }, postOperation = { pageId, result ->
                    defaultPagePostOperation(pageId, applyOnPage = { page ->
                        when (result) {
                            is Failure -> {
                                page.singlePageBoundary = getDefault()
                            }
                            is Success -> {
                                page.singlePageBoundary = result.data
                            }
                        }
                    })
                }
            )
            tryToUnlockDoc(page.docId, page.id)
        }
    }

    /**
     * Pre-Condition: The document is not locked.
     *
     * Crops all pages for the given document, by applying cropping with the normed boundaries.
     *
     * Post-Condition: The document is unlocked.
     */
    fun cropDocument(document: Document) {
        scope.launch {
            documentDao.getDocumentWithPages(document.id)?.sortByNumber()?.let { doc ->
                pageDao.updatePageProcessingStateForDocument(
                    doc.document.id,
                    PostProcessingState.PROCESSING
                )
                withContext(Dispatchers.IO) {
                    val deferredJobs = mutableListOf<Deferred<Resource<Unit>>>()
                    doc.pages.forEach { page ->
                        val deferred = async {
                            return@async pageImageOperation(
                                pageId = page.id,
                                preOperation = { defaultPrePageOperation(page.id) },
                                imageOperation = { page, file ->
                                    // skip page cropping the boundary is not available
                                    if (page.singlePageBoundary == null) {
                                        return@pageImageOperation Success(Unit)
                                    }
                                    val points = (page.singlePageBoundary?.asClockwiseList()
                                        ?: getDefault().asClockwiseList()).map { pointF -> pointF.asPoint() }

                                    // create cache file, copy original file to cache file and apply the operation
                                    // on that file first.
                                    val tempFile = fileHandler.createCacheFile(page.id)
                                    file.copyTo(tempFile, overwrite = true)
                                    // only if the operation successful, copy the returned file to the original file

                                    // copy the exif data first, otherwise it would get lost through the operation.
                                    val exifData = getExifInterface(file)
                                    val newFile = Mapper.applyCropping(tempFile, ArrayList(points))
                                    if (newFile == null) {
                                        tempFile.safelyDelete()
                                        return@pageImageOperation IOErrorCode.CROPPING_FAILED.asFailure()
                                    } else {
                                        newFile.copyTo(file, overwrite = true)
                                        // apply the exif data back to the cropped image
                                        exifData?.let {
                                            saveExifAfterCrop(it, file)
                                        }
                                        tempFile.safelyDelete()
                                        return@pageImageOperation Success(Unit)
                                    }
                                },
                                postOperation = { pageId, _ ->
                                    // set the post processing to done
                                    defaultPagePostOperation(
                                        pageId,
                                        PostProcessingState.DONE
                                    ) { page ->
                                        // clear the single page boundary
                                        page.singlePageBoundary = null
                                        // after a crop, the exif orientation is always reset and therefore
                                        // needs to be also set here.
                                        page.rotation = Rotation.ORIENTATION_NORMAL
                                    }
                                })
                        }
                        deferredJobs.add(deferred)
                    }
                    deferredJobs.awaitAll()
                    unLockDocAfterLongRunningOperation(doc.document.id)
                }
            }
        }
    }

    /**
     * Pre-Condition: The document is not locked.
     */
    suspend fun rotatePages90CW(pages: List<Page>) {
        appDatabase.withTransaction {
            pages.forEach { page ->
                pageDao.updatePageProcessingState(page.id, PostProcessingState.PROCESSING)
            }
        }
        pages.forEach { page ->
            val newRotation = page.rotation.rotateBy90Clockwise()
            pageImageOperation(
                pageId = page.id,
                preOperation = { defaultPrePageOperation(page.id) },
                imageOperation = { innerPage, file ->
                    val cache = fileHandler.createCacheFile(innerPage.id)
                    fileHandler.safelyCopyFile(file, cache)
                    when (val resource = applyRotationResource(cache, newRotation)) {
                        is Failure -> {
                            return@pageImageOperation Failure(resource.exception)
                        }
                        is Success -> {
                            when (val copyResource =
                                fileHandler.copyFileResource(cache, file)) {
                                is Failure -> {
                                    return@pageImageOperation Failure(copyResource.exception)
                                }
                                is Success -> {
                                    copyResource
                                }
                            }
                        }
                    }
                }, postOperation = { pageId, resource ->
                    defaultPagePostOperation(pageId, applyOnPage = {
                        if (resource.isSuccessful()) {
                            it.rotation = newRotation
                            it.singlePageBoundary?.rotateBy90()
                        }
                    })
                }
            )
        }
    }

    suspend fun replacePageFileBy(
        pageId: UUID,
        cachedFile: File,
        rotation: Rotation,
        croppingPoints: List<android.graphics.PointF>
    ): Resource<Unit> {
        return pageImageOperation(
            pageId = pageId,
            preOperation = { defaultPrePageOperation(pageId) },
            imageOperation = { _, file ->
                fileHandler.copyFileResource(cachedFile, file)
            }, postOperation = { _, resource ->
                defaultPagePostOperation(pageId, applyOnPage = {
                    if (resource.isSuccessful()) {
                        it.rotation = rotation
                        it.setSinglePageBoundary(croppingPoints)
                    }
                })
            }
        )
    }

    private suspend fun defaultPagePostOperation(
        pageId: UUID,
        postProcessingState: PostProcessingState = PostProcessingState.DRAFT,
        applyOnPage: suspend (page: Page) -> Unit = {},
    ): Resource<Unit> {
        val page = pageDao.getPageById(pageId) ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        page.postProcessingState = postProcessingState
        applyOnPage(page)
        page.computeFileHash(fileHandler)
        pageDao.insertPage(page)
        return Success(Unit)
    }

    private suspend fun defaultPrePageOperation(pageId: UUID): Resource<Pair<Page, File>> {
        val page = pageDao.getPageById(pageId)
            ?: return DBErrorCode.ENTRY_NOT_AVAILABLE.asFailure()
        return when (val fileResource = fileHandler.getFileByPageResource(page)) {
            is Failure -> {
                IOErrorCode.FILE_MISSING.asFailure()
            }
            is Success -> {
                Success(Pair(page, fileResource.data))
            }
        }
    }
}
