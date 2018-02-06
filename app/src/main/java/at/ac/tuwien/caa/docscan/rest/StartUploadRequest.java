package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by fabian on 05.09.2017.
 */

public class StartUploadRequest extends RestRequest.JSONObjectRestRequest {

//    private static final String URL = "https://transkribus.eu/TrpServerTesting/rest/uploads?collId=";

    public static final int UPLOAD_ID_UNDEFINED = -1;
    private static final String UPLOAD_ID_ID = "uploadId";
    private static final String TRPUPLOAD_ID = "trpUpload";
    private static final String MD_ID = "md";
    private static final String TITLE_ID = "title";

    private int mCollId;

    public StartUploadRequest(Context context, JSONObject jsonObject, int collId) {

        super(context);
        mMethod = Request.Method.POST;
        mJSONObject = jsonObject;
        mCollId = collId;

        RequestHandler.processJsonRequest(this);

    }

    @Override
    public String getExtendedUrl() {

        return "uploads?collId=" + mCollId;

    }

    @Override
    public void handleResponse(JSONObject response) {


        try {
            JSONObject o = response.getJSONObject(TRPUPLOAD_ID);
            int id = o.getInt(UPLOAD_ID_ID);
            String title = o.getJSONObject(MD_ID).getString(TITLE_ID);
            ((StartUploadCallback) mRestCallback).onUploadStart(id, title);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    public interface StartUploadCallback extends RestRequest.RestCallback{

        void onUploadStart(int uploadId, String title);

    }
}
