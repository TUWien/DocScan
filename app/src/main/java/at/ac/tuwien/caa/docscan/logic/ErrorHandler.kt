package at.ac.tuwien.caa.docscan.logic

import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.ui.BaseActivity
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog
import at.ac.tuwien.caa.docscan.ui.dialog.DialogModel
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.handleError(activity: BaseActivity) {
    when (this) {
        is DocScanException -> {
            when (this.docScanError) {
                is DocScanError.TranskribusRestError.HttpError -> {

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
            }
        }
    }
}

//fun Exception.getDialogByDocumentAction(documentAction: DocumentAction) : ADialog.DialogAction {
//    val docException = this as? DocScanException ?: return ADialog.DialogAction.GENERIC
//    when(val error = docException.docScanError){
//        is DocScanError.DBError -> {
//            when(error.code){
//                DBErrorCode.GENERIC -> TODO()
//                DBErrorCode.DOCUMENT_LOCKED -> TODO()
//                DBErrorCode.DOCUMENT_PARTIALLY_LOCKED -> TODO()
//                DBErrorCode.ENTRY_NOT_AVAILABLE -> TODO()
//                DBErrorCode.DUPLICATE -> TODO()
//            }
//        }
//        is DocScanError.TranskribusRestError.HttpError -> {
//            ADialog.DialogAction.GENERIC_UPLOAD_ERROR
//        }
//        is DocScanError.TranskribusRestError.IOError -> {
//            ADialog.DialogAction.UPLOAD_FAILED_NO_INTERNET_CONNECTION
//        }
//    }
//}
//
///**
// * TODO: This is not completed
// * @return the correct dialog based on the error
// */
//fun Exception.getDialogByDocumentAction(documentAction: DocumentAction): ADialog.DialogAction {
//    return when (val exception = this) {
//        is DocScanException -> {
//            when (exception) {
//                is DBDocumentDuplicate -> {
//                    ADialog.DialogAction.GENERIC
//                }
//                is DBDocumentLocked -> {
//                    when (documentAction) {
//                        DocumentAction.DELETE -> {
//                            // TODO: add dialog when user wants to delete but something is ongoing
//                            ADialog.DialogAction.GENERIC
//                        }
//
//                        DocumentAction.CROP -> TODO()
//                        DocumentAction.UPLOAD -> {
//
//                        }
//                        DocumentAction.EXPORT -> TODO()
//                    }
//                    ADialog.DialogAction.GENERIC
//                }
//                is DBWarning -> {
//                    when (documentAction) {
//                        DocumentAction.EXPORT -> {
//                            ADialog.DialogAction.EXPORT_WARNING_IMAGE_CROP_MISSING
//                        }
//                        DocumentAction.UPLOAD -> {
//                            ADialog.DialogAction.UPLOAD_WARNING_IMAGE_CROP_MISSING
//                        }
//                        DocumentAction.CROP -> {
//                            ADialog.DialogAction.HINT_DOCUMENT_ALREADY_CROPPED
//                        }
//                        else -> {
//                            ADialog.DialogAction.GENERIC
//                        }
//                    }
//                }
//                is DBDuplicateAction -> {
//                    when (documentAction) {
//                        DocumentAction.UPLOAD -> {
//                            ADialog.DialogAction.DOCUMENT_ALREADY_UPLOADED
//                        }
//                        else -> {
//                            ADialog.DialogAction.GENERIC
//                        }
//                    }
//                }
//            }
//        }
//        is ApiException -> {
//            when (documentAction) {
//                DocumentAction.UPLOAD -> {
//
//                    //TODO: Distinguish between API errors (UPLOAD_FAILED_NO_LOGIN)
//                    ADialog.DialogAction.GENERIC_UPLOAD_ERROR
//                }
//                else -> {
//                    ADialog.DialogAction.GENERIC_UPLOAD_ERROR
//                }
//            }
//        }
//        is NetworkException -> {
//            when (documentAction) {
//                DocumentAction.UPLOAD -> {
//                    ADialog.DialogAction.UPLOAD_FAILED_NO_INTERNET_CONNECTION
//                }
//                else -> {
//                    // TODO: Add generic network exception
//                    ADialog.DialogAction.GENERIC_UPLOAD_ERROR
//                }
//            }
//        }
//        else -> {
//            ADialog.DialogAction.GENERIC
//        }
//    }
//}
//
//private fun handleGenericThrowable() {
//
//}


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