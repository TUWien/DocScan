package at.ac.tuwien.caa.docscan.logic

import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.api.transkribus.model.uploads.UploadStatusResponse
import java.net.HttpURLConnection

sealed class Resource<T>
data class Success<T>(val data: T) : Resource<T>()
data class Failure<T>(val exception: Throwable) : Resource<T>()

fun <T> DBErrorCode.asFailure(): Failure<T> {
    return Failure(DocScanException(DocScanError.DBError(this)))
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
 * TODO: Double check the conditions!
 */
fun Resource<UploadStatusResponse>.isRecoverable(): Boolean {
    when (this) {
        is Failure -> {
            when (exception) {
                is DocScanException -> {
                    return when (exception.docScanError) {
                        is DocScanError.DBError -> {
                            false
                        }
                        is DocScanError.TranskribusRestError.HttpError -> {
                            false
                        }
                        is DocScanError.TranskribusRestError.IOError -> {
                            true
                        }
                        is DocScanError.IOError -> {
                            true
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
    return true
}
