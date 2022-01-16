package at.ac.tuwien.caa.docscan.logic

import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.ui.base.BaseActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * A default error handler for all errors which are not explicitly handled in the views itself.
 */
fun Throwable.handleError(activity: BaseActivity) {
    when (this) {
        is DocScanException -> {
            when (val error = this.docScanError) {
                is DocScanError.TranskribusRestError.HttpError -> {
                    // TODO: Adapt error handling
                    val dialogModel = DialogModel(
                        dialogAction = ADialog.DialogAction.CUSTOM,
                        customTitle = "TranskribusRestError.HttpError",
                        customMessage = "HTTP error code occurred: ${error.httpStatusCode}"
                    )
                    activity.showDialog(dialogModel)
                }
                is DocScanError.TranskribusRestError.IOError -> {
                    val pair = getErrorMessageFromThrowable(this)
                    val dialogModel = DialogModel(
                        dialogAction = ADialog.DialogAction.CUSTOM,
                        customTitle = activity.getString(pair.first),
                        customMessage = activity.getString(pair.second)
                    )
                    activity.showDialog(dialogModel)
                }
                is DocScanError.DBError -> {
                    if (error.code == DBErrorCode.DOCUMENT_ALREADY_UPLOADED) {
                        activity.showDialog(ADialog.DialogAction.UPLOAD_WARNING_DOC_ALREADY_UPLOADED)
                    } else {
                        // TODO: Adapt error handling
                        val dialogModel = DialogModel(
                            dialogAction = ADialog.DialogAction.CUSTOM,
                            customTitle = "DBError",
                            customMessage = "Error code occurred: ${error.code.name}"
                        )
                        activity.showDialog(dialogModel)
                    }
                }
                // TODO: handle EXPORT_FILE_MISSING_PERMISSION explicitely!
                is DocScanError.IOError -> {
                    // TODO: Adapt error handling
                    val dialogModel = DialogModel(
                        dialogAction = ADialog.DialogAction.CUSTOM,
                        customTitle = "DocScanError.IOError",
                        customMessage = "Error code occurred: ${error.ioErrorCode.name} ${error.throwable?.message}"
                    )
                    activity.showDialog(dialogModel)
                }
            }
        }
    }
}

fun Throwable.getDocScanDBError(): DocScanError.DBError? {
    return when (val error = (this as? DocScanException)?.docScanError) {
        is DocScanError.DBError -> error
        else -> {
            null
        }
    }
}

fun Throwable.getDocScanIOError(): DocScanError.IOError? {
    return when (val error = (this as? DocScanException)?.docScanError) {
        is DocScanError.IOError -> error
        else -> {
            null
        }
    }
}

fun Throwable.getDocScanTranskribusRESTError(): DocScanError.TranskribusRestError? {
    return when (val error = (this as? DocScanException)?.docScanError) {
        is DocScanError.TranskribusRestError -> error
        else -> {
            null
        }
    }
}

private fun getErrorMessageFromThrowable(throwable: Throwable): Pair<Int, Int> {
    // TODO: Handle server errors!
    return when (throwable) {
        is UnknownHostException,
        is ConnectException -> Pair(
            R.string.login_no_connection_error_title,
            R.string.login_no_connection_error_text
        )
        is SocketTimeoutException -> Pair(
            R.string.login_timeout_error_title,
            R.string.login_timeout_error_text
        )
        else -> Pair(R.string.login_network_error_title, R.string.login_network_error_text)
    }
}