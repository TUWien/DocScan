package at.ac.tuwien.caa.docscan.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.logic.Event
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.parcelize.Parcelize
import org.koin.androidx.viewmodel.ext.android.getViewModel
import timber.log.Timber

/**
 * An utility class for handling simple positive/negative dialogs.
 *
 * @author Matej Bartalsky
 */
class ADialog : AppCompatDialogFragment() {

    private val viewModel: DialogViewModel by lazy {
        // if parent fragment is null, i.e. viewModel was attached to the activity's scope.
        if (parentFragment == null) {
            requireActivity().getViewModel()
        } else {
            // otherwise, it was attached in a fragment scope.
            requireParentFragment().getViewModel()
        }
    }

    companion object {
        private val TAG = "ADialog"
        const val EMPTY_RES_PLACEHOLDER = 0
        const val EXTRA_DIALOG_MODEL = "EXTRA_DIALOG_MODEL"

        fun newInstance(dialogModel: DialogModel): ADialog {
            return ADialog().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_DIALOG_MODEL, dialogModel)
                }
            }
        }
    }

    enum class DialogAction(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @StringRes val positiveBtn: Int = R.string.button_ok,
        @StringRes val negativeBtn: Int = EMPTY_RES_PLACEHOLDER,
        @StringRes val neutralBtn: Int = EMPTY_RES_PLACEHOLDER,
        @DrawableRes val icon: Int = EMPTY_RES_PLACEHOLDER,
        val isCancellable: Boolean = true
    ) {
        GENERIC(R.string.generic_error_title, R.string.generic_error_text),
        UPLOAD_WARNING_DOC_ALREADY_UPLOADED(
            R.string.viewer_document_uploaded_title,
            R.string.viewer_document_uploaded_text,
            positiveBtn = R.string.dialog_btn_upload_again
        ),

        // TODO: UPLOAD_CONSTRAINT: Check these messages again!
        DOCUMENT_PAGE_DELETED_DURING_UPLOAD(
            R.string.sync_file_deleted_title,
            R.string.sync_file_deleted_text
        ),

        // TODO: UPLOAD_CONSTRAINT: Check these messages again!
        GENERIC_UPLOAD_ERROR(R.string.sync_file_deleted_title, R.string.sync_file_deleted_text),

        // TODO: UPLOAD_CONSTRAINT: Check these messages again!
        UPLOAD_FAILED_NO_LOGIN(
            R.string.viewer_not_logged_in_title,
            R.string.sync_not_logged_in_text,
            negativeBtn = R.string.dialog_cancel_text
        ),
        UPLOAD_FAILED_NO_INTERNET_CONNECTION(
            R.string.viewer_offline_title,
            R.string.viewer_offline_text
        ),

        // TODO: Necessary to show? (images that are already cropped would be simply ignored)
        HINT_DOCUMENT_ALREADY_CROPPED(
            R.string.viewer_all_cropped_title,
            R.string.viewer_all_cropped_text
        ),
        UPLOAD_WARNING_IMAGE_CROP_MISSING(
            R.string.viewer_not_cropped_confirm_title,
            R.string.viewer_not_cropped_upload_confirm_text,
            positiveBtn = R.string.dialog_btn_proceed,
            negativeBtn = R.string.dialog_cancel_text,
        ),
        EXPORT_WARNING_IMAGE_CROP_MISSING(
            R.string.viewer_not_cropped_confirm_title,
            R.string.viewer_not_cropped_export_confirm_text,
            positiveBtn = R.string.dialog_btn_proceed,
            negativeBtn = R.string.dialog_cancel_text,
        ),

        // title is dynamic
        CONFIRM_DOCUMENT_CROP_OPERATION(
            R.string.viewer_crop_confirm_title,
            R.string.viewer_crop_confirm_text,
            positiveBtn = R.string.dialog_btn_crop,
            negativeBtn = R.string.dialog_cancel_text
        ),

        // title is dynamic
        CONFIRM_DELETE_DOCUMENT(
            R.string.sync_confirm_delete_title,
            R.string.sync_confirm_delete_doc_prefix_text,
            positiveBtn = R.string.dialog_btn_delete,
            negativeBtn = R.string.dialog_cancel_text
        ),
        CONFIRM_DELETE_PAGE(
            R.string.dialog_delete_image_title_singular,
            R.string.dialog_delete_image_text,
            positiveBtn = R.string.dialog_btn_delete,
            negativeBtn = R.string.dialog_cancel_text
        ),
        CONFIRM_DELETE_PAGES(
            R.string.dialog_delete_image_title_singular,
            R.string.dialog_delete_image_text,
            positiveBtn = R.string.dialog_btn_delete,
            negativeBtn = R.string.dialog_cancel_text
        ),
        CONFIRM_RETAKE_IMAGE(
            R.string.page_slide_fragment_retake_image_dialog_title,
            R.string.page_slide_fragment_retake_image_dialog_text,
            positiveBtn = R.string.page_slide_fragment_retake_image_dialog_confirm_text,
            negativeBtn = R.string.dialog_cancel_text
        ),

        // TODO: This can no longer be tracked that easily
        HINT_PDF_EXPORT_PERMISSION_GIVEN(
            R.string.viewer_document_dir_set_title,
            R.string.viewer_document_dir_set_text
        ),
        EXPORT_WARNING_OCR_NOT_AVAILABLE(
            R.string.gallery_confirm_no_ocr_available_title,
            R.string.gallery_confirm_no_ocr_available_text,
            positiveBtn = R.string.dialog_yes_text,
            negativeBtn = R.string.dialog_cancel_text
        ),
        CONFIRM_OCR_SCAN(
            R.string.gallery_confirm_ocr_title,
            R.string.gallery_confirm_ocr_text,
            positiveBtn = R.string.dialog_yes_text,
            negativeBtn = R.string.dialog_no_text,
            neutralBtn = R.string.dialog_cancel_text
        ),
        CONFIRM_UPLOAD(
            R.string.document_viewer_confirm_upload_title,
            R.string.document_viewer_confirm_upload_text,
            positiveBtn = R.string.dialog_btn_upload,
            negativeBtn = R.string.dialog_cancel_text,
        ),
        CONFIRM_CANCEL_UPLOAD(
            R.string.document_viewer_confirm_cancel_upload_title,
            R.string.document_viewer_confirm_cancel_upload_text,
            positiveBtn = R.string.dialog_btn_cancel_upload,
            negativeBtn = R.string.dialog_no_text,
        ),
        UPLOADS_CANCELLED_DUE_CONSTRAINTS_CHANGE(
            R.string.upload_warning_unmetered_constraint_changed_title,
            R.string.upload_warning_unmetered_constraint_changed_text
        ),
        LOGIN_SUCCESS(
            R.string.login_dialog_success_title,
            R.string.login_dialog_success_text,
            isCancellable = false
        ),
        ACTIVITY_NOT_FOUND(R.string.activity_not_found_title, R.string.activity_not_found_text),
        ACTIVITY_NOT_FOUND_EMAIL(R.string.activity_not_found_email_title, R.string.activity_not_found_email_text),
        CREATE_DOCUMENT_DUPLICATE(
            R.string.dialog_create_document_error_duplicate_title,
            R.string.dialog_create_document_error_duplicate_text
        ),
        CREATE_DOCUMENT_INVALID_URL(
            R.string.document_invalid_url_title,
            R.string.document_invalid_url_message
        ),
        CREATE_DOCUMENT_INVALID_QR_CODE(
            R.string.document_qr_parse_error_title,
            R.string.document_qr_parse_error_message
        ),
        DELETE_PDF(
            R.string.viewer_delete_pdf_title,
            R.string.viewer_delete_pdf_text,
            positiveBtn = R.string.sync_confirm_delete_button_text,
            negativeBtn = R.string.sync_cancel_delete_button_text
        ),
        EXPORT_FOLDER_PERMISSION(
            R.string.pdf_fragment_persisted_permission_title,
            R.string.pdf_fragment_persisted_permission_text,
            negativeBtn = R.string.sync_cancel_delete_button_text
        ),
        RATIONALE_CAMERA_PERMISSION(
            R.string.start_permission_title,
            R.string.start_permission_camera_text,
            negativeBtn = R.string.dialog_no_text
        ),
        MIGRATION_FAILED(
            R.string.generic_error_title,
            R.string.generic_error_text
        ),
        CUSTOM(0, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val model = arguments?.getParcelable<DialogModel>(EXTRA_DIALOG_MODEL)
            ?: throw IllegalArgumentException("No arguments passed!")
        val action = model.dialogAction

        val isCancelable = model.customIsCancellable ?: action.isCancellable

        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(model.customTitle ?: getString(action.title))
            .setMessage(model.customMessage ?: getString(action.message))
            .setCancelable(isCancelable)

        // cancelable setting needs to be set for the fragment as well
        setCancelable(isCancelable)

        builder.setPositiveButton(model.customPosButton ?: getString(action.positiveBtn)) { _, _ ->
            viewModel.select(action, DialogButton.POSITIVE, model.arguments)
        }

        // only add negative button if it's specified
        if (model.customNegButton != null || action.negativeBtn != 0) {
            builder.setNegativeButton(
                model.customNegButton ?: getString(action.negativeBtn)
            ) { _, _ ->
                viewModel.select(action, DialogButton.NEGATIVE, model.arguments)
            }
        }

        // only add neutral button if it's specified
        if (model.customNeuButton != null || action.neutralBtn != 0) {
            builder.setNeutralButton(
                model.customNeuButton ?: getString(action.neutralBtn)
            ) { _, _ ->
                viewModel.select(action, DialogButton.NEUTRAL, model.arguments)
            }
        }
        return builder.create()
    }

    fun show(fragmentManager: FragmentManager) {
        val fragment = fragmentManager.findFragmentByTag(TAG)
        if (fragment == null ||
            fragment is ADialog &&
            fragment.dialog?.isShowing != true
        ) {
            fragmentManager.beginTransaction().add(this, TAG).commitAllowingStateLoss()
        } else {
            Timber.d("Dialog already shown")
        }
    }
}

@Parcelize
data class DialogModel(
    val dialogAction: ADialog.DialogAction,
    val customTitle: String? = null,
    val customMessage: String? = null,
    val customPosButton: String? = null,
    val customNegButton: String? = null,
    val customNeuButton: String? = null,
    @DrawableRes val customTitleIcon: Int? = null,
    val customIsCancellable: Boolean? = null,
    val arguments: Bundle = Bundle()
) : Parcelable

enum class DialogButton {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}

data class DialogResult(
    val dialogAction: ADialog.DialogAction,
    val pressedAction: DialogButton,
    val arguments: Bundle
)

class DialogViewModel : ViewModel() {

    val observableDialogAction = MutableLiveData<Event<DialogResult>>()

    internal fun select(
        dialogAction: ADialog.DialogAction,
        dialogButton: DialogButton,
        arguments: Bundle
    ) {
        observableDialogAction.value = Event(DialogResult(dialogAction, dialogButton, arguments))
    }
}

fun ADialog.DialogAction.with(
    customTitle: String? = null,
    customMessage: String? = null,
    arguments: Bundle = Bundle()
): DialogModel {
    return DialogModel(
        this,
        customTitle = customTitle,
        customMessage = customMessage,
        arguments = arguments
    )
}

fun ADialog.DialogAction.show(
    fragmentManager: FragmentManager
) = ADialog.newInstance(DialogModel(this)).show(fragmentManager)

fun DialogModel.show(
    fragmentManager: FragmentManager
) {
    ADialog.newInstance(this).show(fragmentManager)
}

fun DialogResult.isPositive() = pressedAction == DialogButton.POSITIVE
fun DialogResult.isNegative() = pressedAction == DialogButton.NEGATIVE
