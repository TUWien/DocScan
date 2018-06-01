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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.GPS;

import static android.content.Intent.ACTION_SEND_MULTIPLE;


public class DataLog {

    private static DataLog mDataLog = null;

    private ArrayList<ShotLog> mShotLogs;

    private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";

    private static final String LOG_FILE_NAME = "docscanlog.json";
    private static final String UPLOAD_LOG_FILE_NAME = "uploadlog.txt";
    private static final String DATE_NAME = "date";
    private static final String FILE_NAME = "filename";
    private static final String GPS_NAME = "gps";
    private static final String SERIES_MODE_NAME = "series mode";
    private static final String GPS_LONGITUDE_NAME = "longitude";
    private static final String GPS_LATITUDE_NAME = "latitude";

    public static DataLog getInstance() {

        if (mDataLog == null)
            mDataLog = new DataLog();

        return mDataLog;

    }

    private DataLog() {

    }

    public void shareUploadLog(Activity activity) {

        File logPath = new File(activity.getBaseContext().getFilesDir(), UPLOAD_LOG_FILE_NAME);
        Uri contentUri = FileProvider.getUriForFile(activity.getBaseContext(), "at.ac.tuwien.caa.fileprovider", logPath);

        String emailSubject =   activity.getBaseContext().getString(R.string.log_email_subject);
        String[] emailTo =      new String[]{"holl@cvl.tuwien.ac.at"};
        String text =           activity.getBaseContext().getString(R.string.log_email_text);

        Intent intent = ShareCompat.IntentBuilder.from(activity)
                .setType("text/plain")
                .setSubject(emailSubject)
                .setEmailTo(emailTo)
                .setStream(contentUri)
                .setText(text)
                .getIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        activity.startActivity(intent);

    }

    public void shareLog(Activity activity) {

        File uploadLogPath = new File(activity.getBaseContext().getFilesDir(), UPLOAD_LOG_FILE_NAME);
        File logPath = new File(activity.getBaseContext().getFilesDir(), LOG_FILE_NAME);
        Uri logUri = FileProvider.getUriForFile(activity.getBaseContext(), "at.ac.tuwien.caa.fileprovider", logPath);
        Uri uploadLogUri = FileProvider.getUriForFile(activity.getBaseContext(), "at.ac.tuwien.caa.fileprovider", uploadLogPath);

        String emailSubject =   activity.getBaseContext().getString(R.string.log_email_subject);
        String[] emailTo =      new String[]{activity.getBaseContext().getString(R.string.log_email_to)};
//        String[] emailTo =      new String[]{"holl@cvl.tuwien.ac.at"};
        String text =           activity.getBaseContext().getString(R.string.log_email_text);

        Intent intent = ShareCompat.IntentBuilder.from(activity)
                .setType("text/plain")
                .setSubject(emailSubject)
                .setEmailTo(emailTo)
//                .setStream(contentUri)
                .setText(text)
                .getIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        intent.setAction(Intent.ACTION_SEND_MULTIPLE);

        ArrayList<Uri> uris = new ArrayList();
        uris.add(logUri);
        uris.add(uploadLogUri);
//        uris.add(Uri.fromFile(logPath));
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        activity.startActivity(intent);

    }

    public void logShot(String fileName, GPS gps, Date date, boolean seriesMode) {

        ShotLog shotLog = new ShotLog(fileName, gps, date, seriesMode);
        if (mShotLogs == null)
            mShotLogs = new ArrayList<>();
        mShotLogs.add(shotLog);

    }

    public void readLog(Context context) {

        try {
            String fileName = getLogFileName(context);
            InputStream in = new FileInputStream(new File(fileName));
            mShotLogs = readJsonStream(in);

        }
        catch (FileNotFoundException e) {
//            There is no log existing, create a new one:
        }
        catch(Exception e) {
            Log.d(getClass().getName(), e.toString());
        }

        if (mShotLogs == null)
            mShotLogs = new ArrayList();
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
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }


    }

    public void writeLog(Context context) {

        try {
            String fileName = getLogFileName(context);
            File file = new File(fileName);
            file.createNewFile();
            OutputStream out = new FileOutputStream(fileName);
            writeJsonStream(out, mShotLogs);
        }
        catch(IOException e) {
            Log.d(getClass().getName(), e.toString());
        }

    }

    private String getLogFileName(Context context) throws IOException {

        File logPath = context.getFilesDir();
        File logFile = new File(logPath, LOG_FILE_NAME);
        return logFile.getAbsolutePath().toString();

    }

    private String getUploadLogFileName(Context context) throws IOException {

        File logPath = context.getFilesDir();
        File logFile = new File(logPath, UPLOAD_LOG_FILE_NAME);
        return logFile.getAbsolutePath().toString();

    }

    private ArrayList<ShotLog> readJsonStream(InputStream in) throws IOException, ParseException {

        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        ArrayList<ShotLog> shotLog = readList(reader);
        reader.close();

        return shotLog;

    }

    private void writeJsonStream(OutputStream out, ArrayList<ShotLog> shotLogs) throws IOException {

        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeList(writer, shotLogs);
        writer.close();

    }

    private ArrayList<ShotLog> readList(JsonReader reader) throws ParseException {

        ArrayList<ShotLog> shotLogs = new ArrayList<>();

        try {
            reader.beginArray();
            while (reader.hasNext()) {
                shotLogs.add(readShotLog(reader));
            }
            reader.endArray();

        } catch (IOException e) {
            e.printStackTrace();
        }


        return shotLogs;

    }

    private void writeList(JsonWriter writer, ArrayList<ShotLog> shotLogs) throws IOException {

        writer.beginArray();

        if (shotLogs == null)
            shotLogs = new ArrayList<>();

        for (ShotLog shotLog : shotLogs) {
            writeShotLog(writer, shotLog);
        }
        writer.endArray();

    }

    private ShotLog readShotLog(JsonReader reader) throws IOException, ParseException {

        GPS gps = null;
        String dateString, fileName = null;
        Date date = null;
        boolean seriesMode = false;

        reader.beginObject();
        while (reader.hasNext()){
            String name = reader.nextName();
            if (name.equals(FILE_NAME)) {
                fileName = reader.nextString();
            }
            else if (name.equals(DATE_NAME)) {
                dateString = reader.nextString();
                if (dateString != null)
                    date = string2Date(dateString);
            }
            else if (name.equals(GPS_NAME)) {
                gps = readGPS(reader);
            }
            else if (name.equals(SERIES_MODE_NAME)) {
                seriesMode = reader.nextBoolean();
            }

        }
        reader.endObject();

        ShotLog shotLog = new ShotLog(fileName, gps, date, seriesMode);
        return shotLog;

    }

    private GPS readGPS(JsonReader reader) throws IOException {

        String longitude = null;
        String latitude = null;

        reader.beginObject();
//        reader.beginArray();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals(GPS_LONGITUDE_NAME))
                longitude = reader.nextString();
            else if (name.equals(GPS_LATITUDE_NAME))
                latitude = reader.nextString();
            else
                reader.skipValue();
        }
//        reader.endArray();
        reader.endObject();

        GPS gps = null;
        if (longitude!= null && latitude != null)
            gps = new GPS(longitude, latitude);

        return gps;

    }

    private void writeShotLog(JsonWriter writer, ShotLog shotLog) throws IOException{

        writer.beginObject();

//        file naem:
        String fileName = shotLog.getFileName();
        writer.name(FILE_NAME).value(fileName);

//        time stamp:
        String date = date2String(shotLog.getDate());
        writer.name(DATE_NAME).value(date);

//        location:
        if (shotLog.getGPS() != null) {
            writer.name(GPS_NAME);
            writeGPS(writer, shotLog.getGPS());
        }
        else
            writer.name(GPS_NAME).nullValue();

//        series mode:
        writer.name(SERIES_MODE_NAME).value(shotLog.isSeriesMode());

        writer.endObject();

    }

    private Date string2Date(String dateString) throws ParseException {
        DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        Date date = df.parse(dateString);
        return date;
    }

    private String date2String(Date date) {
        String timeStamp = new SimpleDateFormat(DATE_FORMAT).format(date);
        return timeStamp;
    }

    private void writeGPS(JsonWriter writer, GPS gps) throws IOException {

        writer.beginObject();
        writer.name(GPS_LONGITUDE_NAME).value(gps.getLongitude());
        writer.name(GPS_LATITUDE_NAME).value(gps.getLatitude());
        writer.endObject();

    }

    private class ShotLog {

        private GPS mGPS;
        private Date mDate;
        private boolean mSeriesMode;
        private String mFileName;

        private ShotLog(String fileName, GPS gps, Date date, boolean seriesMode) {
            mFileName = fileName;
            mGPS = gps;
            mDate = date;
            mSeriesMode = seriesMode;
        }

        private Date getDate() {
            return mDate;
        }

        private GPS getGPS() {
            return mGPS;
        }

        private boolean isSeriesMode() { return mSeriesMode; }

        public String getFileName() { return mFileName; }
    }


}
