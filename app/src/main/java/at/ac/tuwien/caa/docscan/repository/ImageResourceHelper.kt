package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import java.io.File
import java.util.*

/**
 * Performs an image operation:
 * - [preOperation] is used to load the file and the page.
 * - [imageOperation] runs the operation on the file of the image and returns a result that should be persisted in the page object.
 * - [postOperation] runs after the [imageOperation] to perform DB updates, but it is always performed to cleanup possible errors.
 */
fun <T> pageImageOperation(
    pageId: UUID,
    preOperation: () -> Resource<Pair<Page, File>>,
    imageOperation: (page: Page, file: File) -> Resource<T>,
    postOperation: (page: UUID, operationResource: Resource<T>) -> Unit
): Resource<Unit> {
    val input = when (val pre = preOperation.invoke()) {
        is Failure -> {
            // call the post operation on an error too to cleanup states/resources
            postOperation.invoke(pageId, Failure(pre.exception))
            return Failure(pre.exception)
        }
        is Success -> {
            pre.data
        }
    }
    val result = imageOperation.invoke(input.first, input.second)
    postOperation.invoke(pageId, result)

    return when (result) {
        is Failure -> {
            Failure(result.exception)
        }
        is Success -> {
            Success(Unit)
        }
    }
}
