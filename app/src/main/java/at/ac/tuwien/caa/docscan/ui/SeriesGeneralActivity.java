package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.rest.User;

/**
 * Created by fabian on 05.10.2017.
 */

public class SeriesGeneralActivity extends BaseNoNavigationActivity {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_general);
        super.initToolbarTitle(R.string.series_general_title);

        mContext = this;

        showSeriesTitle();

        Button startButton = (Button) findViewById(R.id.series_general_start_button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //                Taken from: https://stackoverflow.com/questions/35861081/custom-popup-dialog-with-input-field

                LayoutInflater li = LayoutInflater.from(mContext);
                View promptsView = li.inflate(R.layout.create_document_view, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

                // set prompts.xml to alertdialog builder
                alertDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView.findViewById(R.id.document_name_edit);

                userInput.setFilters(getInputFilters());

                // set dialog message
                alertDialogBuilder
                        .setTitle(R.string.document_create_folder_title)
                        .setCancelable(true)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        onCreateDirResult(userInput.getText().toString());
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
//                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();


            }
        });

        Button selectButton = (Button) findViewById(R.id.series_general_select_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DocumentActivity.class);
                startActivity(intent);
            }
        });

    }

    private void showSeriesTitle() {

        // Show the first and last name of the user:
        TextView userTextView = (TextView) findViewById(R.id.series_general_current_series_textview);
        String userText = User.getInstance().getDocumentName();
        userTextView.setText(userText);

    }

    private InputFilter[] getInputFilters() {

        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (!Character.isLetterOrDigit(c) && !Character.isSpaceChar(c)) {
                        return "";
                    }
                }
                return null;
            }
        };

        InputFilter[] filters = {filter};

        return filters;

    }

    private void onCreateDirResult(String result) {

        File mediaStorageDir = Helper.getMediaStorageDir(mContext.getResources().getString(R.string.app_name));
        File subDir = new File(mediaStorageDir.getAbsolutePath(), result);
        boolean dirCreated = false;
        if (subDir != null) {
            dirCreated = subDir.mkdir();

        }

        if (!dirCreated)
            showNoDirCreatedAlert();
        else {
            User.getInstance().setDocumentName(subDir.getName());
            finish(); // close the activity
        }

    }

    private void showNoDirCreatedAlert() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

        // set dialog message
        alertDialogBuilder
                .setTitle(R.string.document_no_dir_created_title)
                .setCancelable(true)
                .setPositiveButton("OK", null)
                .setMessage(R.string.document_no_dir_created_message);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }



}
