package at.ac.tuwien.caa.docscan.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.File;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Settings;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

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

        initStartButton();

        initSelectButton();

        initKeepButton();

        initHideDialogCheckBox();

    }

    private void initHideDialogCheckBox() {
        CheckBox hideDialogCheckBox = (CheckBox) findViewById(R.id.series_general_hide_dialog_checkbox);

        // Load the setting and set the state of the checkbox:
        boolean hideDialog = Settings.getInstance().loadKey(this, Settings.SettingEnum.HIDE_SERIES_DIALOG_KEY);
        hideDialogCheckBox.setChecked(hideDialog);

        final Activity a = this;

        hideDialogCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Settings.getInstance().saveKey(a, Settings.SettingEnum.HIDE_SERIES_DIALOG_KEY, isChecked);

            }
        });
    }

    private void initStartButton() {

        Button startButton = (Button) findViewById(R.id.series_general_start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), CreateSeriesActivity.class);
                startActivity(intent);
            }
        });

    }

//    private void initStartButton() {
//        Button startButton = (Button) findViewById(R.id.series_general_start_button);
//
//        startButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//
//
//
//                //                Taken from: https://stackoverflow.com/questions/35861081/custom-popup-dialog-with-input-field
//
//                LayoutInflater li = LayoutInflater.from(mContext);
//                View promptsView = li.inflate(R.layout.create_document_view, null);
//
//                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
//
//                // set prompts.xml to alertdialog builder
//                alertDialogBuilder.setView(promptsView);
//
//                final EditText userInput = (EditText) promptsView.findViewById(R.id.document_name_edit);
//
//                userInput.setFilters(getInputFilters());
//
//                // set dialog message
//                alertDialogBuilder
//                        .setTitle(R.string.document_create_folder_title)
//                        .setCancelable(true)
//                        .setPositiveButton("OK",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int id) {
//                                        onCreateDirResult(userInput.getText().toString());
//                                    }
//                                })
//                        .setNegativeButton("Cancel",
//                                new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog,int id) {
////                                        dialog.cancel();
//                                    }
//                                });
//
//                // create alert dialog
//                AlertDialog alertDialog = alertDialogBuilder.create();
////                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
//                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
////                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//
//                // show it
//                alertDialog.show();
//
//
//            }
//        });
//    }

    private void initSelectButton() {
        Button selectButton = (Button) findViewById(R.id.series_general_select_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DocumentActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initKeepButton() {
        Button keepButton = (Button) findViewById(R.id.series_general_keep_series_button);
        keepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // close the activity
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
            UserHandler.saveSeriesName(this);
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
