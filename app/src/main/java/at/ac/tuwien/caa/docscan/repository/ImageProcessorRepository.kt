package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.logic.applyRotation
import at.ac.tuwien.caa.docscan.logic.removeRotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class ImageProcessorRepository(val documentRepository: DocumentRepository) {
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

    }

    fun createDocument(document: DocumentWithPages) {

    }
}
