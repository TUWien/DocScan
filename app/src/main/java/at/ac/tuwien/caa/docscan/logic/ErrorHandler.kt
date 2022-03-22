package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.error.DBErrorCode
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.ui.base.BaseActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import okhttp3.internal.closeQuietly
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.net.ConnectException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val preferencesHandler: PreferencesHandler by KoinJavaComponent.inject(PreferencesHandler::class.java)

/**
 * A default error handler for all errors which are not explicitly handled in the views itself.
 */
fun Throwable.handleError(activity: BaseActivity, logAsWarning: Boolean = false) {
    val dialogModel = DialogModel(
        dialogAction = ADialog.DialogAction.CUSTOM,
        customTitle = getTitle(
            activity
        ),
        customMessage = getMessage(
            activity,
            showExtendedDebugMessages = preferencesHandler.showExtendedDebugErrorMessages
        )
    )
    activity.showDialog(dialogModel)
    if (logAsWarning) {
        // logs the current exception, also by adding the stacktrace of a new throwable, this used to
        // determine the caller.
        val sw = StringWriter()
        Throwable().printStackTrace(PrintWriter(sw))
        val stackTrace: String = sw.toString()
        sw.closeQuietly()
        Timber.w(
            this,
            "Error has occurred for caller:\n ${stackTrace}\n with throwable:"
        )
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

fun Throwable.getTitle(context: Context): String {
    return when (this) {
        is DocScanException -> {
            when (val error = this.docScanError) {
                is DocScanError.TranskribusRestError.HttpError -> {
                    context.getString(getTitleByHttpCode(error.httpStatusCode))
                }
                is DocScanError.TranskribusRestError.IOError -> {
                    context.getString(getTitleInternal(error.throwable))
                }
                is DocScanError.DBError -> {
                    context.getString(getTitleByDBErrorCode(error.code))
                }
                is DocScanError.IOError -> {
                    context.getString(getTitleByIOErrorCode(error.ioErrorCode, error.throwable))
                }
            }
        }
        else -> {
            context.getString(getTitleInternal(this))
        }
    }
}

fun Throwable.getMessage(context: Context, showExtendedDebugMessages: Boolean = false): String {
    return when (this) {
        is DocScanException -> {
            when (val error = this.docScanError) {
                is DocScanError.TranskribusRestError.HttpError -> {
                    context.getString(getMessageByHttpCode(error.httpStatusCode))
                        .prependIfEnabled(showExtendedDebugMessages) {
                            "HTTP Error code: ${error.httpStatusCode}\n"
                        }
                }
                is DocScanError.TranskribusRestError.IOError -> {
                    context.getString(getMessageInternal(error.throwable))
                        .appendIfEnabled(showExtendedDebugMessages) {
                            "\nTranskribusRestError.IOError: ${error.throwable.message}"
                        }
                }
                is DocScanError.DBError -> {
                    context.getString(getMessageByDBErrorCode(error.code))
                        .prependIfEnabled(showExtendedDebugMessages) {
                            "DBErrorCode: ${error.code.name}\n"
                        }
                }
                is DocScanError.IOError -> {
                    context.getString(getMessageIOErrorCode(error.ioErrorCode, error.throwable))
                        .appendIfEnabled(showExtendedDebugMessages) {
                            "\nDocScanError.IOError: ${error.ioErrorCode.name} exception: ${error.throwable?.message}"
                        }
                }
            }
        }
        else -> {
            context.getString(getMessageInternal(this))
        }
    }
}

private fun getTitleByHttpCode(httpCode: Int): Int {
    return when (httpCode) {
        HTTP_UNAUTHORIZED -> {
            R.string.sync_not_logged_in_title
        }
        else -> {
            R.string.generic_error_title
        }
    }
}

private fun getMessageByHttpCode(httpCode: Int): Int {
    return when (httpCode) {
        HTTP_UNAUTHORIZED -> {
            R.string.sync_not_logged_in_text
        }
        else -> {
            R.string.generic_server_error_text
        }
    }
}

private fun getTitleByDBErrorCode(dbErrorCode: DBErrorCode): Int {
    return dbErrorCode.titleStringResId
}

private fun getMessageByDBErrorCode(dbErrorCode: DBErrorCode): Int {
    return dbErrorCode.textStringResId
}

private fun getTitleByIOErrorCode(ioErrorCode: IOErrorCode, throwable: Throwable?): Int {
    if (ioErrorCode.needsInvestigation) {
        throwable ?: return R.string.generic_error_title
        return getMessageInternal(throwable)
    }
    return ioErrorCode.titleStringResId
}

private fun getMessageIOErrorCode(ioErrorCode: IOErrorCode, throwable: Throwable?): Int {
    if (ioErrorCode.needsInvestigation) {
        throwable ?: return R.string.generic_error_text
        return getMessageInternal(throwable)
    }
    return ioErrorCode.textStringResId
}

private fun getTitleInternal(throwable: Throwable): Int {
    return when (throwable) {
        is UnknownHostException, is ConnectException -> R.string.no_connection_error_title
        is SocketTimeoutException -> R.string.timeout_error_title
        else -> R.string.generic_error_title
    }
}

private fun getMessageInternal(throwable: Throwable): Int {
    return when (throwable) {
        is UnknownHostException, is ConnectException -> R.string.no_connection_error_text
        is SocketTimeoutException -> R.string.timeout_error_text
        else -> R.string.generic_error_text
    }
}

private fun String.prependIfEnabled(enabled: Boolean, prepend: () -> String): String {
    if (enabled) {
        return prepend() + this
    }
    return this
}

private fun String.appendIfEnabled(enabled: Boolean, append: () -> String): String {
    if (enabled) {
        return this + append()
    }
    return this
}
