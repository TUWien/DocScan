package at.ac.tuwien.caa.docscan.repository

import androidx.room.withTransaction
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.Mapper
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector
import at.ac.tuwien.caa.docscan.db.AppDatabase
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.*
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.Page
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
import kotlin.collections.ArrayList

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
                preOperation = { defaultPrePageOperation(page.id) },
                imageOperation = { page, file ->
                    try {
                        val result = PageDetector.findRectAndFocus(file.absolutePath)
                        if (result.points.size > 3) {
                            page.setSinglePageBoundary(result.points)
                        }
                        Success(Unit)
                    } catch (e: Exception) {
                        Timber.e("Page detection has failed!", e)
                        page.singlePageBoundary = getDefault()
                        IOErrorCode.SINGLE_PAGE_DETECTION_FAILED.asFailure(e)
                    }
                }, postOperation = { postPage, _ ->
                    defaultPagePostOperation(postPage)
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
                                preOperation = { defaultPrePageOperation(page.id) },
                                imageOperation = { page, file ->
                                    // skip page cropping the boundary is not available or if the boundary
                                    // is just the default one.
                                    if (page.singlePageBoundary == null || page.singlePageBoundary == getDefault()) {
                                        return@pageImageOperation Success(Unit)
                                    }
                                    val points = (page.singlePageBoundary?.asClockwiseList()
                                        ?: getDefault().asClockwiseList()).map { pointF -> pointF.asPoint() }
                                    try {
                                        Mapper.replaceWithMappedImage(
                                            file.absolutePath,
                                            ArrayList(points)
                                        )
                                        return@pageImageOperation Success(Unit)
                                    } catch (e: Exception) {
                                        Timber.e("Cropping has failed!", e)
                                        return@pageImageOperation IOErrorCode.CROPPING_FAILED.asFailure(
                                            e
                                        )
                                    }
                                }, postOperation = { postPage, _ ->
                                    // clear the single page boundary
                                    postPage.singlePageBoundary = null
                                    // set the post processing to done
                                    defaultPagePostOperation(postPage, PostProcessingState.DONE)
                                }
                            )
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
                preOperation = { defaultPrePageOperation(page.id) },
                imageOperation = { page, file ->
                    val cache = fileHandler.createCacheFile(page.id)
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
                }, postOperation = { postPage, resource ->
                    resource.applyOnSuccess {
                        postPage.rotation = newRotation
                        postPage.singlePageBoundary?.rotateBy90()
                    }
                    defaultPagePostOperation(postPage)
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
            preOperation = { defaultPrePageOperation(pageId) },
            imageOperation = { _, file ->
                fileHandler.copyFileResource(cachedFile, file)
            }, postOperation = { page, operationResource ->
                operationResource.applyOnSuccess {
                    page.rotation = rotation
                    page.setSinglePageBoundary(croppingPoints)
                }
                defaultPagePostOperation(page)
            }
        )
    }

    private suspend fun defaultPagePostOperation(
        page: Page,
        postProcessingState: PostProcessingState = PostProcessingState.DRAFT
    ): Resource<Unit> {
        page.computeFileHash(fileHandler)
        page.postProcessingState = postProcessingState
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
