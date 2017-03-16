package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.GPS;

/**
 * Created by fabian on 09.03.2017.
 */

public class DataLog {

    private static DataLog mDataLog = null;

    private ArrayList<ShotLog> mShotLog;

    private static final String DATE_NAME = "date";
    private static final String GPS_NAME = "gps";
    private static final String GPS_LONGITUDE_NAME = "longitude";
    private static final String GPS_LATITUDE_NAME = "latitude";

    public static DataLog getInstance() {

        if (mDataLog == null)
            mDataLog = new DataLog();

        return mDataLog;

    }

    private DataLog() {

        mShotLog = new ArrayList();

    }

    public void logShot(GPS gps, Date date) {

        ShotLog shotLog = new ShotLog(gps, date);
        mShotLog.add(shotLog);

    }

    public void writeLog(Context context) {

        try {
            Uri fileName = getLogFileName(context);
            OutputStream out = new FileOutputStream(fileName.toString());
            writeJsonStream(out, mShotLog);
        }
        catch(IOException e) {
            Log.d(getClass().getName(), e.toString());
        }

    }

    private Uri getLogFileName(Context context) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), context.getString(R.string.app_name));

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        // Create a log file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                context.getString(R.string.log_prefix) + timeStamp + context.getString(R.string.log_extension));

        return Uri.fromFile(mediaFile);


    }

    private void writeJsonStream(OutputStream out, ArrayList<ShotLog> shotLogs) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writeList(writer, shotLogs);
        writer.close();
    }

    private void writeList(JsonWriter writer, ArrayList<ShotLog> shotLogs) throws IOException {

        writer.beginArray();
        for (ShotLog shotLog : shotLogs) {
            writeLog(writer, shotLog);
        }
        writer.endArray();

    }

    private void writeLog(JsonWriter writer, ShotLog shotLog) throws IOException{

        writer.beginObject();

//        time stamp:
        String date = formatDate(shotLog.getDate());
        writer.name(DATE_NAME).value(date);

//        location:
        if (shotLog.getGPS() != null) {
            writer.name(GPS_NAME);
            writeGPS(writer, shotLog.getGPS());
        }
        else
            writer.name(GPS_NAME).nullValue();

        writer.endObject();

    }

    private String formatDate(Date date) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
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

        private ShotLog(GPS gps, Date date) {
            mGPS = gps;
            mDate = date;
        }

        private Date getDate() {
            return mDate;
        }

        private GPS getGPS() {
            return mGPS;
        }
    }


}
