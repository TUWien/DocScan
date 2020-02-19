package at.ac.tuwien.caa.docscan.rest;


import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.logic.DataLog;
import at.ac.tuwien.caa.docscan.logic.Helper;

/**
 * Created by fabian on 29.06.2017.
 */

public class UploadStatusRequest extends RestRequest.XMLRequest {

    //    JSON method for retrieving documents: https://transkribus.eu/TrpServer/rest/collections/{collection-ID}/{document-ID}/fulldoc
    private final static String URL = "uploads/";
    private int mUploadID;
    private static final String CLASS_NAME = "UploadStatusRequest";

    public UploadStatusRequest(Context context, int uploadID) {

        super(context);

        Log.d(CLASS_NAME, "constructor: uploadID: " + uploadID);

        mMethod = Request.Method.GET;
        mUploadID = uploadID;

        RequestHandler.processRequest(this);

    }

    public int getUploadID() {

        return mUploadID;

    }

    @Override
    public String getExtendedUrl() {
        return URL + mUploadID;
    }

    @Override
    public void handleResponse(String response) {

        try {
//            TODO: I have no clue, why this class has to be a XMLRequest to receive a JSON string...
            JSONObject j1 = new JSONObject(response);

//            Check if we are not processing an already finished upload:
            if (j1.has("finished")) {
                Log.d(CLASS_NAME, "handleResponse: finished");
                ((UploadStatusCallback) mRestCallback).onUploadAlreadyFinished(mUploadID);
                return;
            }
            JSONObject j2 = j1.getJSONObject("pageList");
            JSONArray a = j2.getJSONArray("pages");

//            Determine the files that are not uploaded:
            ArrayList<String> fileNames = new ArrayList<>();

            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (!o.getBoolean("pageUploaded"))
                    fileNames.add(o.getString("fileName"));
            }

            String title = j1.getJSONObject("md").getString("title");
            ((UploadStatusCallback) mRestCallback).onStatusReceived(mUploadID, title, fileNames);


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     This is a dirty workaround to solve the issue arising with unfinished uploads, that are
     already finished, but we did not receive a finish from the server, because the connection
     was closed before. After a while, such old and finished uploads are removed from the
     server and if we query them we get a 404 error. Therefore, we catch the error in this class.
     * @param error
     */

    @Override
    public void handleRestError(VolleyError error) {

        logError(error);

        if (error != null && error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                ((UploadStatusCallback) mRestCallback).onUploadAlreadyFinished(mUploadID);
                return;
            }
        }

//        Handle other errors:
        mRestCallback.handleRestError(this, error);

//        int statusCode = error.networkResponse.statusCode;
//        switch (statusCode) {
////            TODO: switch to the other status code
//            case HttpURLConnection.HTTP_NOT_FOUND: // 404-
////            case HttpURLConnection.HTTP_INTERNAL_ERROR:
//                ((UploadStatusCallback) mRestCallback).onUploadAlreadyFinished(mUploadID);
//                break;
//            default:
//                mRestCallback.handleRestError(this, error);
//                break;
//        }



    }

    private void logError(VolleyError error) {
        DataLog.getInstance().writeUploadLog(getContext(), CLASS_NAME,
                "handleRestError: request: " + this.toString());
        Log.d(CLASS_NAME, "handleRestError: request: " + this.toString());

        if (error == null || error.getMessage() == null || error.networkResponse == null) {
            DataLog.getInstance().writeUploadLog(getContext(), CLASS_NAME,
                    "handleRestError: error is null ");
            Log.d(CLASS_NAME, "handleRestError: error is null");
            return;
        }


        DataLog.getInstance().writeUploadLog(getContext(), CLASS_NAME,
                "handleRestError: error: " + error.getMessage());
        Log.d(CLASS_NAME, "handleRestError: error: " + error.getMessage());

        int statusCode = error.networkResponse.statusCode;
        DataLog.getInstance().writeUploadLog(getContext(), CLASS_NAME,
                "handleRestError: statusCode: " + statusCode);
        Log.d(CLASS_NAME, "handleRestError: statusCode: " + statusCode);


        String networkResponse = Helper.getNetworkResponse(error);
        DataLog.getInstance().writeUploadLog(getContext(), CLASS_NAME,
                "handleRestError: networkResponse: " + networkResponse);
        Log.d(CLASS_NAME, "handleRestError: networkResponse: " + networkResponse);
    }

    public interface UploadStatusCallback extends RestCallback{

        void onStatusReceived(int uploadID, String title, ArrayList<String> unfinishedFileNames);
        void onUploadAlreadyFinished(int uploadID);

    }
}
