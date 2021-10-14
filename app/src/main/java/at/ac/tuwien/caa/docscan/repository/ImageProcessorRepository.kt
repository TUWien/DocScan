package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector
import at.ac.tuwien.caa.docscan.db.dao.PageDao
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.db.model.setSinglePageBoundary
import at.ac.tuwien.caa.docscan.db.model.state.PostProcessingState
import at.ac.tuwien.caa.docscan.logic.FileHandler
import at.ac.tuwien.caa.docscan.logic.applyRotation
import at.ac.tuwien.caa.docscan.logic.removeRotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImageProcessorRepository(
    val pageDao: PageDao,
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
}
