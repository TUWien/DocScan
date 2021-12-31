package at.ac.tuwien.caa.docscan.ui.document

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.databinding.ActivityCreateDocumentBinding
import at.ac.tuwien.caa.docscan.logic.*
import at.ac.tuwien.caa.docscan.ui.base.BaseNoNavigationActivity
import at.ac.tuwien.caa.docscan.ui.camera.CameraActivity
import com.google.android.material.textfield.TextInputEditText
import me.drakeet.support.toast.ToastCompat
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * Created by fabian on 24.10.2017.
 * TODO: cleanup code
 * TODO: Use viewbinding
 * TODO: split create/edit document activity
 */
open class CreateDocumentActivity : BaseNoNavigationActivity() {

    companion object {
        const val DOCUMENT_QR_TEXT = "DOCUMENT_QR_TEXT"

        fun newInstance(context: Context, qrText: String?): Intent {
            return Intent(context, CreateDocumentActivity::class.java).apply {
                putExtra(DOCUMENT_QR_TEXT, qrText)
            }
        }
    }

    private val viewModel: CreateDocumentViewModel by viewModel()
    private val preferencesHandler: PreferencesHandler by inject(PreferencesHandler::class.java)
    protected lateinit var binding: ActivityCreateDocumentBinding

    private var mTranskribusMetaData: TranskribusMetaData? = null

    //    Time stamp used for construction the exemplar file name:
    private var mTimeStamp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateDocumentBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)
        super.initToolbarTitle(R.string.create_series_title)
        initOkButton()
        initUrlButton()
        initEditField()
        //        Transkribus metadata:
        initMetadataViews()
        // Uncomment for readme2020:
        //        Readme 2020 project - not checked by default:
        initReadme2020Views()

//        Custom naming:
        initCustomNamingFields()

//        Debugging: (if you just want to launch the Activity (without CameraActivity)
//        String qrText = "<root><authority>Universitätsarchiv Greifswald</authority><identifier type=\"hierarchy description\">Universitätsarchiv Greifswald/Altes Rektorat/01. Rechtliche Stellung der Universität - 01.01. Statuten/R 1199</identifier><identifier type=\"uri\">https://ariadne-portal.uni-greifswald.de/?arc=1&type=obj&id=5162222</identifier><title>Entwurf neuer Universitätsstatuten </title><date normal=\"1835010118421231\">1835-1842</date><callNumber>R 1199</callNumber><description>Enthält u.a.: Ausführliche rechtshistorische Begründung des Entwurfs von 1835.</description></root>";
//        processQRCode(qrText);

        // Read the information in the QR Code transmitted via the intent:
        val extras = intent.extras
        if (extras != null) {
            val initString = ""
            val qrText = extras.getString(DOCUMENT_QR_TEXT, initString)
            if (qrText != initString) {
                mTranskribusMetaData = processQRCode(qrText)
                if (mTranskribusMetaData != null) fillViews(mTranskribusMetaData) else showQRCodeErrorAlert()
            }
        }

        observe()
    }

    private fun observe() {
        viewModel.observableResource.observe(this, {
            when (it) {
                is Failure -> {
                    // TODO: Check if the error is sufficient
                    showNoDirCreatedAlert()
                }
                is Success -> {
                    val intent = CameraActivity.newInstance(this)
                    Helper.hideKeyboard(this)
                    startActivity(intent)
                    finish()
                }
            }
        })
    }

    private fun initCustomNamingFields() {
        val namingCheckbox = findViewById<CheckBox>(R.id.create_series_custom_name_checkbox)
        namingCheckbox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            showCustomNameLayout(isChecked)
            updateExampleFileName()
        }
        val input = findViewById<TextInputEditText>(R.id.create_series_custom_name_prefix_input)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                updateExampleFileName()
            }
        })
        //        InputFilter filter = Helper.getDocumentInputFilter();
//        if (filter != null)
//            input.setFilters(new InputFilter[] {filter});
        val filters = Helper.getDocumentInputFilters()
        input.filters = filters
    }

    private fun showCustomNameLayout(isChecked: Boolean) {
        val layout = findViewById<RelativeLayout>(R.id.create_series_custom_name_layout)
        if (isChecked) {
            layout.visibility = View.VISIBLE
            //            Copy the document name to the prefix field:
//            EditText prefix = findViewById(R.id.create_series_custom_name_prefix_edittext);
            val input = findViewById<TextInputEditText>(R.id.create_series_custom_name_prefix_input)
            val documentName = findViewById<EditText>(R.id.create_series_name_edittext)
            input.text = documentName.text
        } else layout.visibility = View.GONE
    }

    private fun updateExampleFileName() {

//        Switch nameSwitch = findViewById(R.id.create_series_custom_name_checkbox);
        val nameCheckbox = findViewById<CheckBox>(R.id.create_series_custom_name_checkbox)
        val prefix: String
        if (nameCheckbox.isChecked) {
            val prefixEdit =
                findViewById<TextInputEditText>(R.id.create_series_custom_name_prefix_input)
            prefix = prefixEdit.text.toString()
            if (prefix.isEmpty()) {
                showEmptyPrefixWarning()
                return
            }
        } else {
            val nameEdit = findViewById<EditText>(R.id.create_series_name_edittext)
            prefix = nameEdit.text.toString()
        }
        val example = Helper.getFileNamePrefix(mTimeStamp, prefix, 1)
        val exampleTextView =
            findViewById<TextView>(R.id.create_series_custom_name_example_textview)
        exampleTextView.text = example
    }

    private fun showEmptyPrefixWarning() {
        val exampleTextView =
            findViewById<TextView>(R.id.create_series_custom_name_example_textview)
        exampleTextView.setText(R.string.create_series_custom_name_example_textview_empty_prefix_text)
    }

    // Uncomment for readme2020:
    private fun initReadme2020Views() {
        val checkBox = findViewById<CheckBox>(R.id.create_series_readme_checkbox)
        val layout = findViewById<RelativeLayout>(R.id.create_series_readme_fields_layout)
        //        No Readme2020 project - per default:
        initExpandableLayout(checkBox, layout, false)
        checkBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            expandLayout(
                isChecked,
                layout
            )
        }
        initLanguageSpinner()
    }

    private fun initMetadataViews() {
        val showMetadata = preferencesHandler.showTranskribusMetaData
        val metadataCheckBox = findViewById<CheckBox>(R.id.create_series_advanced_options_checkbox)
        val metadataLayout = findViewById<RelativeLayout>(R.id.create_series_fields_layout)
        initExpandableLayout(metadataCheckBox, metadataLayout, showMetadata)
        metadataCheckBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            expandLayout(isChecked, metadataLayout)
            // Remember the check state:
            preferencesHandler.showTranskribusMetaData = isChecked
        }
    }

    private fun initUrlButton() {
        val linkButton = findViewById<AppCompatImageButton>(R.id.create_series_link_button)
        linkButton.setOnClickListener {
            val urlEditText = findViewById<EditText>(R.id.create_series_url_edittext)
            val url = urlEditText.text.toString()
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW)
                val uri = Uri.parse(url)
                browserIntent.data = uri
                startActivity(browserIntent)
            } catch (e: ActivityNotFoundException) {
                showUrlNotValidAlert(url)
            }
        }
    }

    private fun initEditField() {
        val editText = findViewById<EditText>(R.id.create_series_name_edittext)
        val filters = Helper.getDocumentInputFilters()
        editText.filters = filters
    }

    public override fun onResume() {
        super.onResume()
        mTimeStamp = Helper.getFileTimeStamp()
    }

    private fun processQRCode(text: String): TranskribusMetaData {

        // Currently the XML has no root defined (malformed) so we add one manually:
        val qrText = "<root>$text</root>"
        Timber.d("parsing document")
        val metaData = parseQRCode(qrText)
        Timber.d("found document: %s", metaData)
        return metaData
    }

    private fun parseQRCode(text: String): TranskribusMetaData {
        Timber.d("QR code text: %s", text)
        return TranskribusMetaData.parseXML(text)
    }

    //        Temporarily deactivate the advanced fields:
    private fun fillViews(qrCodeInfo: TranskribusMetaData?) {
        if (qrCodeInfo == null) return
        val editable = qrCodeInfo.relatedUploadId == null

        // Title:
        val titleEditText = findViewById<EditText>(R.id.create_series_name_edittext)
        if (qrCodeInfo.title != null) {
            titleEditText.setText(qrCodeInfo.title)
            if (!editable) titleEditText.keyListener = null
        }
        val showAdvancedFields = true
        val layout = findViewById<RelativeLayout>(R.id.create_series_fields_layout)
        fillAdvancedFields(qrCodeInfo, editable)
        if (showAdvancedFields) {
//           Show the advanced settings:
            layout.visibility = View.VISIBLE
            //            Hide the link button if we have no link:
            if (qrCodeInfo.link == null) {
                val button = findViewById<AppCompatImageButton>(R.id.create_series_link_button)
                button.visibility = View.GONE
            }
        } else layout.visibility = View.INVISIBLE
    }

    private fun fillAdvancedFields(metaData: TranskribusMetaData?, editable: Boolean) {
        //           // Description:
//           EditText descriptionEditText = findViewById(R.id.create_series_description_edittext);
//           if (document.getTitle() != null)
//               descriptionEditText.setText(document.getDescription());
        if (metaData == null) return

//        Note: The following three fields are not passed via QR code, but we disable them if not
//        editable:
        val authorEditText = findViewById<EditText>(R.id.create_series_author_edittext)
        val writerEditText = findViewById<EditText>(R.id.create_series_writer_edittext)
        val genreEditText = findViewById<EditText>(R.id.create_series_genre_edittext)

        // Signature:
        val signatureEditText = findViewById<EditText>(R.id.create_series_signature_edittext)
        if (metaData.signature != null) signatureEditText.setText(metaData.signature)

        // Authority:
        val authorityEditText = findViewById<EditText>(R.id.create_series_authority_edittext)
        if (metaData.authority != null) authorityEditText.setText(metaData.authority)

        //        URI button:
        val urlEditText = findViewById<EditText>(R.id.create_series_url_edittext)
        if (metaData.url != null) urlEditText.setText(metaData.url)

//        Check if the document is a special archive document created from QR code:
        if (!editable) {
            authorEditText.keyListener = null
            writerEditText.keyListener = null
            genreEditText.keyListener = null
            signatureEditText.keyListener = null
            authorityEditText.keyListener = null
            urlEditText.keyListener = null
        }
    }

    protected open fun initOkButton() {
        val okButton = findViewById<Button>(R.id.create_series_done_button)
        okButton.setOnClickListener {
//                Retrieve the entered text:
            val editText = findViewById<EditText>(R.id.create_series_name_edittext)
            val title = editText.text.toString()

//            Check if custom naming is checked and prefix is given:
            if (!isCustomNamingValid) return@setOnClickListener

//                The error handling is done in validateInput
            if (validateInput(title)) {
                viewModel.createDocument(title, mTranskribusMetaData)
            }
        }
    }

    private fun validateInput(title: String?): Boolean {
        if (title == null) {
            return false
        }

        // Uncomment for readme2020:
//        Check if the readme2020 fields are filled (because they are mandatory):
        if (!isReadme2020FieldsCompleted) return false
        //        Fill the metadata with the field values entered by the user:
        readMetaDataFields()
        //        Get the fields of Readme2020 (if set):
        if (!readReadme2020Fields()) return false

//        Fill the metadata with the field values entered by the user:
        readMetaDataFields()

        return true
    }

    /**
     * Checks if the custom file naming fields are valid. Returns true if no custom naming is used.
     *
     * @return
     */
    protected val isCustomNamingValid: Boolean
        get() {
            val namingCheckbox = findViewById<CheckBox>(R.id.create_series_custom_name_checkbox)
            if (!namingCheckbox.isChecked) return true
            val inputEdit =
                findViewById<TextInputEditText>(R.id.create_series_custom_name_prefix_input)
            val input = inputEdit.text.toString()
            if (input.isEmpty()) {
                inputEdit.error =
                    getString(R.string.create_series_custom_name_prefix_input_empty_text)
                return false
            }
            return true
        }

    //    Uncomment for readme2020:
    val isReadme2020FieldsCompleted: Boolean
        get() {
            val readmeCheckBox = findViewById<CheckBox>(R.id.create_series_readme_checkbox)
            if (readmeCheckBox.isChecked) {
                val radioGroup =
                    findViewById<RadioGroup>(R.id.create_series_readme_public_radio_group)
                val radioTextView = findViewById<TextView>(R.id.create_series_readme_public_label)
                if (radioGroup.checkedRadioButtonId == -1) {
                    radioTextView.error = getString(R.string.create_series_readme_public_error)
                    ToastCompat.makeText(
                        this,
                        R.string.create_series_readme_error_toast_text,
                        Toast.LENGTH_LONG
                    ).show()
                    return false
                } else radioTextView.error = null
                val textView =
                    findViewById<AutoCompleteTextView>(R.id.create_series_readme_language_dropdown)
                val language = textView.text.toString()
                if (language.isEmpty()) {
                    textView.error = getString(R.string.create_series_readme_language_error)
                    ToastCompat.makeText(
                        this,
                        R.string.create_series_readme_error_toast_text,
                        Toast.LENGTH_LONG
                    ).show()
                    return false
                } else textView.error = null
            }
            return true
        }

    //    Uncomment for readme2020:
    private fun readReadme2020Fields(): Boolean {
        val readmeCheckBox = findViewById<CheckBox>(R.id.create_series_readme_checkbox)
        if (readmeCheckBox.isChecked) {
            mTranskribusMetaData = mTranskribusMetaData ?: TranskribusMetaData()
            //            Enable upload in readme2020 collection:
            mTranskribusMetaData?.readme2020 = true
            //            Switch publicSwitch = findViewById(R.id.create_series_readme_public_switch);
//            if (publicSwitch.isChecked())
//                mTranskribusMetaData.setReadme2020Public(true);
            val radioGroup = findViewById<RadioGroup>(R.id.create_series_readme_public_radio_group)
            if (radioGroup.checkedRadioButtonId == R.id.create_series_readme_public_radio_button) mTranskribusMetaData?.readme2020Public =
                true else if (radioGroup.checkedRadioButtonId == R.id.create_series_readme_private_radio_button) mTranskribusMetaData?.readme2020Public =
                false

//        Save the selected language:
            val textView =
                findViewById<AutoCompleteTextView>(R.id.create_series_readme_language_dropdown)
            val language = textView.text.toString()
            if (language.isNotEmpty()) mTranskribusMetaData!!.language = language
        }
        return true
    }

    private fun readMetaDataFields() {
        val authorEditText = findViewById<EditText>(R.id.create_series_author_edittext)
        val author = authorEditText.text.toString()
        val writerEditText = findViewById<EditText>(R.id.create_series_writer_edittext)
        val writer = writerEditText.text.toString()
        val genreEditText = findViewById<EditText>(R.id.create_series_genre_edittext)
        val genre = genreEditText.text.toString()
        val signatureEditText = findViewById<EditText>(R.id.create_series_signature_edittext)
        val signature = signatureEditText.text.toString()
        val authorityEditText = findViewById<EditText>(R.id.create_series_authority_edittext)
        val authority = authorityEditText.text.toString()
        val urlEditText = findViewById<EditText>(R.id.create_series_url_edittext)
        val url = urlEditText.text.toString()

//            We need at least one field that is set to create meta data here:
        if (author.isNotEmpty() || writer.isNotEmpty() || genre.isNotEmpty() ||
            signature.isNotEmpty() || authority.isNotEmpty() || url.isNotEmpty()
        ) {
            if (mTranskribusMetaData == null) mTranskribusMetaData = TranskribusMetaData()
            mTranskribusMetaData!!.author = author
            mTranskribusMetaData!!.writer = writer
            mTranskribusMetaData!!.genre = genre
            mTranskribusMetaData!!.signature = signature
            mTranskribusMetaData!!.authority = authority
            mTranskribusMetaData!!.url = url
        }
    }

    //    initExpandableLayout(findViewById(R.id.create_series_advanced_options_checkbox), findViewById(R.id.create_series_fields_layout), SHOW_TRANSKRIBUS_METADATA_KEY)
    private fun initExpandableLayout(
        showFieldsCheckBox: CheckBox, fieldsLayout: RelativeLayout,
        checked: Boolean
    ) {
        showFieldsCheckBox.isChecked = checked

//        Hide or show the fields:
        expandLayout(checked, fieldsLayout)
    }

    //    Uncomment for readme2020:
    private fun initLanguageSpinner() {
        val languages = resources.getStringArray(R.array.create_document_languages)
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_popup_item,
            languages
        )
        val editTextFilledExposedDropdown =
            findViewById<AutoCompleteTextView>(R.id.create_series_readme_language_dropdown)
        editTextFilledExposedDropdown.inputType = 0
        editTextFilledExposedDropdown.setAdapter(adapter)

//        Spinner spinner = findViewById(R.id.create_series_readme_language_spinner);
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
//                R.array.create_document_languages, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
    }

    private fun expandLayout(isChecked: Boolean, fieldsLayout: RelativeLayout) {
        if (isChecked) fieldsLayout.visibility = View.VISIBLE else fieldsLayout.visibility =
            View.GONE
    }

    protected fun showDirExistingCreatedAlert() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        val msg = "There is already a document with the same name, please choose another one!"
        // set dialog message
        alertDialogBuilder
            .setTitle(R.string.document_no_dir_created_title)
            .setCancelable(true)
            .setPositiveButton("OK", null)
            .setMessage(msg)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()
    }

    private fun showUrlNotValidAlert(url: String) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        val text = resources.getString(R.string.document_invalid_url_message) + " " + url
        // set dialog message
        alertDialogBuilder
            .setTitle(R.string.document_invalid_url_title)
            .setCancelable(true)
            .setPositiveButton("OK", null)
            .setMessage(text)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()
    }

    private fun showQRCodeErrorAlert() {
        val alertDialogBuilder = AlertDialog.Builder(this)

        // set dialog message
        alertDialogBuilder
            .setTitle(R.string.document_qr_parse_error_title)
            .setCancelable(true)
            .setPositiveButton("OK", null)
            .setMessage(R.string.document_qr_parse_error_message)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()
    }

    private fun showNoDirCreatedAlert() {
        val alertDialogBuilder = AlertDialog.Builder(this)

        // set dialog message
        alertDialogBuilder
            .setTitle(R.string.document_no_dir_created_title)
            .setCancelable(true)
            .setPositiveButton("OK", null)
            .setMessage(R.string.document_no_dir_created_message)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()
    }
}
