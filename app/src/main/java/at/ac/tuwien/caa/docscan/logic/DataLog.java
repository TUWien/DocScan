/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   09. March 2017
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

package at.ac.tuwien.caa.docscan.logic;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.sync.SyncStorage;


public class DataLog {

    private static DataLog mDataLog = null;

    private static final String UPLOAD_LOG_FILE_NAME = "uploadlog.txt";

    public static DataLog getInstance() {

        if (mDataLog == null)
            mDataLog = new DataLog();

        return mDataLog;

    }

    private DataLog() {

    }

    public void shareLog(Activity activity) {

        File uploadLogPath = new File(activity.getBaseContext().getFilesDir(), UPLOAD_LOG_FILE_NAME);
        File documentPath = new File(activity.getBaseContext().getFilesDir(),
                DocumentStorage.DOCUMENT_STORE_FILE_NAME);
        File syncPath = new File(activity.getBaseContext().getFilesDir(),
                SyncStorage.SYNC_STORAGE_FILE_NAME);
        Uri uploadLogUri = FileProvider.getUriForFile(activity.getBaseContext(),
                "at.ac.tuwien.caa.fileprovider", uploadLogPath);
        Uri documentUri = FileProvider.getUriForFile(activity.getBaseContext(),
                "at.ac.tuwien.caa.fileprovider", documentPath);
        Uri syncUri = FileProvider.getUriForFile(activity.getBaseContext(),
                "at.ac.tuwien.caa.fileprovider", syncPath);

        String emailSubject =   activity.getBaseContext().getString(R.string.log_email_subject);
        String[] emailTo =      new String[]{activity.getBaseContext().getString(R.string.log_email_to)};
//        String[] emailTo =      new String[]{"holl@cvl.tuwien.ac.at"};
        String text =           activity.getBaseContext().getString(R.string.log_email_text);

        Intent intent = ShareCompat.IntentBuilder.from(activity)
                .setType("text/plain")
                .setSubject(emailSubject)
                .setEmailTo(emailTo)
                .setText(text)
                .getIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.setAction(Intent.ACTION_SEND_MULTIPLE);

        ArrayList<Uri> uris = new ArrayList();
        uris.add(uploadLogUri);
        uris.add(documentUri);
        uris.add(syncUri);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        try {
            activity.startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
            Crashlytics.logException(e);
            Helper.showActivityNotFoundAlert(activity.getApplicationContext());
        }

    }

    public void writeUploadLog(Context context, String sender, String text) {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String logText = sender + ": " + text;
        if (timeStamp != null)
               logText = timeStamp + ": " + logText;

        writeUploadLog(context, logText);
    }

    public void writeUploadLog(Context context, String text) {

        if ((context == null ) || (text == null))
            return;

        try {
            String fileName = getUploadLogFileName(context);
            File file = new File(fileName);

            FileOutputStream fOut = new FileOutputStream(file,true);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fOut));

            bw.newLine();
            bw.write(text);
            bw.close();
            fOut.close();

        } catch (FileNotFoundException e1) {
            Crashlytics.logException(e1);
            e1.printStackTrace();
        } catch (IOException e1) {
            Crashlytics.logException(e1);
            e1.printStackTrace();
        }


    }

    private String getUploadLogFileName(Context context) throws IOException {

        File logPath = context.getFilesDir();
        File logFile = new File(logPath, UPLOAD_LOG_FILE_NAME);
        return logFile.getAbsolutePath().toString();

    }

}
