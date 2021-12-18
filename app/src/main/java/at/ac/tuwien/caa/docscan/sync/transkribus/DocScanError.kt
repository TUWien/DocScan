package at.ac.tuwien.caa.docscan.sync.transkribus

import at.ac.tuwien.caa.docscan.sync.transkribus.model.error.TranskribusApiError

class DocScanException(val docScanError: DocScanError): Exception()

sealed class DocScanError {
    sealed class TranskribusRestError : DocScanError() {

        data class HttpError(
            val httpStatusCode: Int,
            val transkribusApiError: TranskribusApiError?
        ) : DocScanError()

        data class IOError(
            val throwable: Throwable
        ) : DocScanError()
    }
}
