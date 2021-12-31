package at.ac.tuwien.caa.docscan.logic

import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.api.transkribus.model.error.TranskribusApiError

class DocScanException(val docScanError: DocScanError) : Exception()

sealed class DocScanError {

    /**
     * Represents errors that occur while using the transkribus REST API.
     */
    sealed class TranskribusRestError : DocScanError() {

        data class HttpError(
            val httpStatusCode: Int,
            val transkribusApiError: TranskribusApiError?
        ) : DocScanError()

        data class IOError(
            val throwable: Throwable
        ) : DocScanError()
    }

    /**
     * A database error that may happen internally if constraints are violated.
     */
    data class DBError(val code: DBErrorCode) : DocScanError()

    /**
     * An IO error that may happen when performing internal IO operations.
     */
    data class IOError(val throwable: Throwable) : DocScanError()
}
