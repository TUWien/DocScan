package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.model.DocumentWithPages
import at.ac.tuwien.caa.docscan.db.model.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ImageProcessorRepository(val documentRepository: DocumentRepository) {
    // TODO: use own scope to launch work on images
    // TODO: Check how to maybe use a global scope to perform this stuff or if a work manager is necessary.

    private val scope = CoroutineScope(Dispatchers.IO)

    fun rotatePage(page: Page) {
        scope.launch {

        }
    }

    // TODO: Define the type of detection
    // TODO: define if a single/double page detection should be performed.
    fun spawnPageDetection(page: Page) {

    }

    fun createDocument(document: DocumentWithPages) {

    }
}
