package at.ac.tuwien.caa.docscan.logic

import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.api.transkribus.model.uploads.UploadStatusResponse
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import java.net.HttpURLConnection

sealed class Resource<T>
data class Success<T>(val data: T) : Resource<T>()
data class Failure<T>(val exception: Throwable) : Resource<T>()

/**
 * A helper function for applying [success] if [Resource] is [Success].
 */
fun <T> Resource<T>.applyOnSuccess(success: (T) -> Unit): Resource<T> {
    return when (this) {
        is Failure -> this
        is Success -> {
            success(this.data)
            this
        }
    }
}

fun <T> Throwable.asResource(): Resource<T> {
    return Failure(this)
}

fun <T> DBErrorCode.asFailure(): Failure<T> {
    return Failure(DocScanException(DocScanError.DBError(this)))
}

fun <T> IOErrorCode.asFailure(exception: Throwable? = null): Failure<T> {
    return Failure(DocScanException(DocScanError.IOError(this, exception)))
}

fun <T> asUnauthorizedFailure(): Failure<T> {
    return Failure(
        DocScanException(
            DocScanError.TranskribusRestError.HttpError(
                HttpURLConnection.HTTP_UNAUTHORIZED,
                null
            )
        )
    )
}

/**
 * Determines if the upload of a document is recoverable or not.
 */
fun Resource<UploadStatusResponse>.isUploadRecoverable(): Boolean {
    when (this) {
        is Failure -> {
            when (exception) {
                is DocScanException -> {
                    return when (exception.docScanError) {
                        is DocScanError.DBError -> {
                            // internal DB errors are not recoverable
                            false
                        }
                        is DocScanError.TranskribusRestError.HttpError -> {
                            // every http error cannot be recovered.
                            false
                        }
                        is DocScanError.TranskribusRestError.IOError -> {
                            // if an IO error happened with the transkribus API, then this is very likely recoverable
                            true
                        }
                        is DocScanError.IOError -> {
                            // if an IO error happened, this could mean that a file for the upload is missing.
                            false
                        }
                    }
                }
            }
        }
        is Success -> {
            // it doesn't matter
            return true
        }
    }
    return false
}
