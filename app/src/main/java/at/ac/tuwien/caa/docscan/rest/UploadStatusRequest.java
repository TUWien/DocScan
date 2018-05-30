package at.ac.tuwien.caa.docscan.rest;


import android.content.Context;
import android.util.Log;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

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

            ((UploadStatusCallback) mRestCallback).onStatusReceived(mUploadID, fileNames);


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public interface UploadStatusCallback extends RestCallback{

        void onStatusReceived(int uploadID, ArrayList<String> unfinishedFileNames);
        void onUploadAlreadyFinished(int uploadID);

    }
}
