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

    public StartUploadRequest(Context context, JSONObject jsonObject) {

        super(context);
        mMethod = Request.Method.POST;
        mJSONObject = jsonObject;

        RequestHandler.processJsonRequest(this);

//        try {
//
//            mJSONObject = new JSONObject(
//                    "{" +
//                            "    \"pageList\": {\"pages\": [" +
//                            "        {" +
//                            "            \"fileName\": \"" +
//                            SyncInfo.getInstance().getSyncList().get(SyncInfo.getInstance().getSyncList().size()-1).getFile().getName()
//                            + "\"," +
//                            "            \"pageNr\": 1" +
//                            "        }" +
//                            "    ]}" +
//                            "}");
//
//            RequestHandler.processJsonRequest(this);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public String getExtendedUrl() {

        return "uploads?collId=915";

    }

    @Override
    public void handleResponse(JSONObject response) {


        int id = UPLOAD_ID_UNDEFINED;
        try {
            JSONObject o = response.getJSONObject(TRPUPLOAD_ID);
            id = o.getInt(UPLOAD_ID_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ((StartUploadCallback) mRestCallback).onUploadStart(id);

    }


    public interface StartUploadCallback extends RestRequest.RestCallback{

        void onUploadStart(int uploadId);

    }
}
