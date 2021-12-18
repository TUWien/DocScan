package at.ac.tuwien.caa.docscan.repository

import androidx.annotation.IntRange
import at.ac.tuwien.caa.docscan.logic.Failure
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.sync.transkribus.DocScanError
import at.ac.tuwien.caa.docscan.sync.transkribus.DocScanException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import java.net.HttpURLConnection

suspend fun <ApiType, ResourceType> transkribusResource(
    forceNetwork: () -> Boolean = { false },
    loadFromDisk: (suspend () -> ApiType?)? = null,
    apiCall: suspend () -> Response<ApiType>,
    persistNetworkResponse: (response: ApiType) -> Unit = { },
    transformToResourceType: (result: Resource<ApiType>) -> Resource<ResourceType> = { result ->
        @Suppress("UNCHECKED_CAST")
        result as Resource<ResourceType> // is assumed in default case when no transformation function is provided
    },
): Resource<ResourceType> {
    return withContext(Dispatchers.IO) {
        // a network operation is retried once only if the expected error has a 401 status
        retryResourceRequest(
            maxRetries = 1,
            block = {
                val resource = fetchResource(
                    forceNetwork,
                    loadFromDisk,
                    apiCall
                )
                if(resource is Success) {
                    persistNetworkResponse(resource.data)
                }
                transformToResourceType(resource)
            },
            predicate = { e ->
                e.is401().also {
                    if (it) Timber.d("Suspend network call is retried after http status 401")
                }
            }
        )
    }
}

fun Throwable?.is401(): Boolean {
    return isHttpCode(HttpURLConnection.HTTP_UNAUTHORIZED)
}

fun Throwable?.is403(): Boolean {
    return isHttpCode(HttpURLConnection.HTTP_FORBIDDEN)
}

private fun Throwable?.isHttpCode(httpCode: Int): Boolean {
    return when (val doscScanError = (this as? DocScanException)?.docScanError) {
        is DocScanError.TranskribusRestError.HttpError -> {
            return doscScanError.httpStatusCode == httpCode
        }
        else -> {
            false
        }
    }
}

/**
 * fetch resource from remote or disk
 */
private suspend fun <ApiType> fetchResource(
    forceNetwork: () -> Boolean,
    loadFromDisk: (suspend () -> ApiType?)?,
    apiCall: suspend () -> Response<ApiType>
): Resource<ApiType> {
    if (!forceNetwork.invoke()) {
        loadFromDisk?.invoke()?.let {
            return Success(it)
        }
    }

    return try {
        apiCall.invoke().convertToResource()
    } catch (e: Exception) {
        Failure(
            DocScanException(DocScanError.TranskribusRestError.IOError(e))
        )
    }
}

/**
 * Converts a retrofit [Response] into a [Resource].
 */
fun <T> Response<T>.convertToResource(): Resource<T> {
    return if (isSuccessful) {
        val body = body()
        if (body == null || code() == HttpURLConnection.HTTP_NO_CONTENT) {
            // empty responses are handled as error responses
            Failure(
                DocScanException(
                    DocScanError.TranskribusRestError.HttpError(
                        HttpURLConnection.HTTP_NO_CONTENT,
                        null
                    )
                )
            )
        } else {
            Success(body)
        }
    } else {
        Failure(
            DocScanException(
                DocScanError.TranskribusRestError.HttpError(
                    httpStatusCode = code(),
                    null
                )
            )
        )
    }
}

/**
 * A retry function for [Resource] requests. If the predicate should not hold, the function is
 * not retried and the resource is returned without re-throwing the exception again.
 *
 * @param maxRetries max retry attempts for the given suspend block
 */
suspend fun <T> retryResourceRequest(
    @IntRange(from = 0) maxRetries: Int = 1,
    block: suspend () -> Resource<T>,
    predicate: suspend (cause: Throwable) -> Boolean = { true }
): Resource<T> {
    repeat(maxRetries) {
        when (val resource = block()) {
            is Failure -> {
                if (predicate(resource.exception)) {
                    Timber.d(
                        resource.exception,
                        "suspend function failed - retries left: %s",
                        maxRetries - 1 - it
                    )
                } else {
                    return resource
                }
            }
            is Success -> {
                return resource
            }
        }
    }
    return block() // last attempt
}
