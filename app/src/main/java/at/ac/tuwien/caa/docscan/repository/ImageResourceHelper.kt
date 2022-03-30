package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Performs an image operation:
 * - [preOperation] is used to load the file and the page.
 * - [imageOperation] runs the operation on the file of the image and returns a result that should be persisted in the page object.
 * - [postOperation] runs after the [imageOperation] to perform DB updates, but it is always performed to cleanup possible errors.
 */
suspend fun <T> pageImageOperation(
    pageId: UUID,
    preOperation: suspend () -> Resource<Pair<Page, File>>,
    imageOperation: suspend (page: Page, file: File) -> Resource<T>,
    postOperation: suspend (page: UUID, operationResource: Resource<T>) -> Unit
): Resource<Unit> {
    return withContext(NonCancellable) {
        val input = when (val pre = preOperation.invoke()) {
            is Failure -> {
                // call the post operation on an error too to cleanup states/resources
                postOperation.invoke(pageId, Failure(pre.exception))
                return@withContext Failure(pre.exception)
            }
            is Success -> {
                pre.data
            }
        }
        val result = imageOperation.invoke(input.first, input.second)
        postOperation.invoke(pageId, result)

        when (result) {
            is Failure -> {
                return@withContext Failure<Unit>(result.exception)
            }
            is Success -> {
                return@withContext Success(Unit)
            }
        }
    }
}
