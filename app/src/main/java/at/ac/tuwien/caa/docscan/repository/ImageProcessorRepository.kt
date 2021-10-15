package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.Mapper
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector
import at.ac.tuwien.caa.docscan.db.dao.DocumentDao
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.Document
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.boundary.SinglePageBoundary.Companion.getDefault
import at.ac.tuwien.caa.docscan.db.model.boundary.asClockwiseList
import at.ac.tuwien.caa.docscan.db.model.boundary.asPoint
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.setSinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.applyRotation
import at.ac.tuwien.caa.docscan.logic.removeRotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class ImageProcessorRepository(
    val pageDao: PageDao,
    val documentDao: DocumentDao,
    val fileHandler: FileHandler
) {
    // TODO: use own scope to launch work on images
    // TODO: Check how to maybe use a global scope to perform this stuff or if a work manager is necessary.

    private val scope = CoroutineScope(Dispatchers.IO)

    fun rotateFile(file: File, rotation: Rotation) {
        applyRotation(file, rotation)
    }

    fun removeRotationExif(file: File) {
        removeRotation(file)
    }

    // TODO: Define the type of detection
    // TODO: define if a single/double page detection should be performed.

    /**
     * Pre-Condition: The document is not locked.
     */
    fun spawnPageDetection(page: Page) {
        scope.launch {
            pageDao.updatePageProcessingState(page.id, PostProcessingState.PROCESSING)
            fileHandler.getFileByPage(page)?.let {
                try {
                    val result = PageDetector.findRectAndFocus(it.absolutePath)
                    if (result.points.size > 3) {
                        pageDao.getPageById(page.id)?.let { page ->
                            page.setSinglePageBoundary(result.points)
                            pageDao.insertPage(page)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            // the state is updated to draft, since it doesn't mean that the post processing is done.
            pageDao.updatePageProcessingState(page.id, PostProcessingState.DRAFT)
        }
    }

    /**
     * Pre-Condition: The document is not locked.
     */
    fun cropDocument(document: Document) {
        scope.launch {
            documentDao.getDocumentWithPages(document.id)?.let { doc ->
                pageDao.updatePageProcessingStateForDocument(
                    doc.document.id,
                    PostProcessingState.PROCESSING
                )
                // TODO: This can be performed on multiple threads to speed up the processing.
                // TODO: Perform the processing based on the numbering order.
                doc.pages.forEach { page ->
                    fileHandler.getFileByPage(page)?.let {
                        val points = (page.singlePageBoundary?.asClockwiseList()
                            ?: getDefault().asClockwiseList()).map { pointF -> pointF.asPoint() }
                        try {
                            Mapper.replaceWithMappedImage(it.absolutePath, ArrayList(points))
                        } catch (e: Exception) {
                            Timber.e("Cropping has failed!", e)
                        }
                    }
                    // TODO: The boundaries are sometimes still shown.
                    pageDao.updatePageProcessingState(page.id, PostProcessingState.DONE)
                }
                // TODO: This shouldn't be actually necessary
                pageDao.updatePageProcessingStateForDocument(
                    doc.document.id,
                    PostProcessingState.DONE
                )
            }
        }
    }

    fun uploadDocument(document: Document) {
        // TODO: spawn upload job, check pre-elminaries before
    }
}
