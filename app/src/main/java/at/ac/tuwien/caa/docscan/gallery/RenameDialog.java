package at.ac.tuwien.caa.docscan.gallery;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;

import at.ac.tuwien.caa.docscan.R;

public class RenameDialog extends DialogFragment {

    private static final String FILE_NAME_KEY = "FILE_NAME_KEY";
    private String mFileName;

    public static RenameDialog newInstance(String documentName) {

        RenameDialog dialog = new RenameDialog();
//        Hand over the document name:

        Bundle args = new Bundle();
        args.putString(FILE_NAME_KEY, documentName);
        dialog.setArguments(args);

        return dialog;

    }

    @Override
    public void onCreate(Bundle savedInstance) {

        super.onCreate(savedInstance);

        mFileName = getArguments().getString(FILE_NAME_KEY);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_edit_document, container, false);

        final EditText editText = view.findViewById(R.id.edit_edit_text);
        editText.setText(mFileName);

        ImageButton clearButton = view.findViewById(R.id.edit_clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.setText("");
            }
        });

        // Show soft keyboard automatically and request focus to field
        editText.requestFocus();
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return view;

    }

}
