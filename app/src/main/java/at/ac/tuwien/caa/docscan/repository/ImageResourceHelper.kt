package at.ac.tuwien.caa.docscan.repository

import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

suspend fun pageImageOperation(
    preOperation: suspend () -> Resource<Pair<Page, File>>,
    imageOperation: suspend (page: Page, file: File) -> Resource<Unit>,
    postOperation: suspend (page: Page, operationResource: Resource<Unit>) -> Unit
): Resource<Unit> {
    return withContext(NonCancellable) {
        val input = when (val pre = preOperation.invoke()) {
            is Failure -> {
                return@withContext Failure(pre.exception)
            }
            is Success -> {
                pre.data
            }
        }
        val result = imageOperation.invoke(input.first, input.second)
        postOperation.invoke(input.first, result)
        return@withContext result
    }
}
