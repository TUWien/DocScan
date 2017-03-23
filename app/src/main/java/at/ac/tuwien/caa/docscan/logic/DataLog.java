package at.ac.tuwien.caa.docscan.logic;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

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

/**
 * Created by fabian on 09.03.2017.
 */

public class DataLog {

    private static DataLog mDataLog = null;

    private ArrayList<ShotLog> mShotLog;

    private static final String DATE_FORMAT = "yyyyMMdd_HHmmss";

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



    }

    public void logShot(GPS gps, Date date) {

        ShotLog shotLog = new ShotLog(gps, date);
        mShotLog.add(shotLog);

    }

    public void readLog(Context context) {

        try {
            String fileName = getLogFileName(context);
//            InputStream out = new FileInputStream(fileName);

//            String fileName = context.getString(R.string.log_filename);

            InputStream in = new FileInputStream(new File(fileName));
            mShotLog = readJsonStream(in);

        }
        catch (FileNotFoundException e) {
//            There is no log existing, create a new one:
            mShotLog = new ArrayList();
        }
        catch(Exception e) {
            Log.d(getClass().getName(), e.toString());
        }
    }

    public void writeLog(Context context) {

        try {
            String fileName = getLogFileName(context);
//            OutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            boolean b = (new File(fileName)).createNewFile();
            OutputStream out = new FileOutputStream(fileName);
//
//            String fileName = getLogFileName(context);
//            OutputStream out = new FileOutputStream(fileName);
            writeJsonStream(out, mShotLog);
        }
        catch(IOException e) {
            Log.d(getClass().getName(), e.toString());
        }

    }

    private String getLogFileName(Context context) throws IOException {
//        File logPath = new File(context.getFilesDir(), "logs");
//        logPath.mkdirs();
        File logPath = context.getFilesDir();
        String fileName = context.getString(R.string.log_filename);
        File logFile = new File(logPath, fileName);

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

    private ArrayList<ShotLog> readList(JsonReader reader) throws IOException, ParseException {

        ArrayList<ShotLog> shotLogs = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            shotLogs.add(readShotLog(reader));
        }
        reader.endArray();

        return shotLogs;

    }

    private void writeList(JsonWriter writer, ArrayList<ShotLog> shotLogs) throws IOException {

        writer.beginArray();
        for (ShotLog shotLog : shotLogs) {
            writeShotLog(writer, shotLog);
        }
        writer.endArray();

    }

    private ShotLog readShotLog(JsonReader reader) throws IOException, ParseException {

        GPS gps = null;
        String dateString;
        Date date = null;

        reader.beginObject();
        while (reader.hasNext()){
            String name = reader.nextName();
            if (name.equals(DATE_NAME)) {
                dateString = reader.nextString();
                if (dateString != null)
                    date = string2Date(dateString);
            }
            else if (name.equals(GPS_NAME)) {
                gps = readGPS(reader);
            }

        }
        reader.endObject();

        ShotLog shotLog = new ShotLog(gps, date);
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
