package at.ac.tuwien.caa.docscan.logic

import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode

class DocScanException(val docScanError: DocScanError) : Exception() {
    override val cause: Throwable?
        get() {
            return when (docScanError) {
                is DocScanError.DBError -> {
                    super.cause
                }
                is DocScanError.IOError -> {
                    docScanError.throwable?.cause ?: super.cause
                }
                is DocScanError.TranskribusRestError.HttpError -> {
                    super.cause
                }
                is DocScanError.TranskribusRestError.IOError -> {
                    docScanError.throwable.cause
                }
            }
        }

    override val message: String
        get() {
            return when (docScanError) {
                is DocScanError.DBError -> {
                    "DocScanError.DBError: Error code: ${docScanError.code.name}"
                }
                is DocScanError.IOError -> {
                    "DocScanError.IOError: ${docScanError.ioErrorCode.name}  ${docScanError.throwable?.message ?: "Error trace unknown!"}"
                }
                is DocScanError.TranskribusRestError.HttpError -> {
                    "DocScanError.TranskribusRestError.HttpError: HTTP Code ${docScanError.httpStatusCode}\n Error response: ${docScanError.jsonErrorResponse}"
                }
                is DocScanError.TranskribusRestError.IOError -> {
                    docScanError.throwable.message
                        ?: "DocScanError.TranskribusRestError.IOError: Error unknown!"
                }
            }
        }
}

sealed class DocScanError {

    /**
     * Represents errors that occur while using the transkribus REST API.
     */
    sealed class TranskribusRestError : DocScanError() {

        data class HttpError(
            val httpStatusCode: Int,
            val jsonErrorResponse: String?
        ) : TranskribusRestError()

        data class IOError(
            val throwable: Throwable
        ) : TranskribusRestError()
    }

    /**
     * A database error that may happen internally if constraints are violated.
     */
    data class DBError(val code: DBErrorCode) : DocScanError()

    /**
     * An IO error that may happen when performing internal IO operations.
     */
    data class IOError(val ioErrorCode: IOErrorCode, val throwable: Throwable? = null) :
        DocScanError()
}
