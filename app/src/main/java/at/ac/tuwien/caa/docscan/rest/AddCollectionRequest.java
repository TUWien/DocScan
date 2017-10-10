package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 05.09.2017.
 */

public class AddCollectionRequest extends RestRequest.JSONObjectRestRequest implements
        StartUploadRequest.StartUploadCallback {

    private static final String URL = "collections/createCollection";
    private Context mContext;

    public AddCollectionRequest(Context context) {

        super(context);
        mMethod = Request.Method.POST;
        RequestHandler.processJsonRequest(this);
        mContext = context;

    }

    @Override
    public String getExtendedUrl() {
        return URL;
    }

    @Override
    public void handleResponse(JSONObject response) {
//        new StartUploadRequest(mContext, response);
    }

    @Override
    public void onUploadStart(int uploadId, String title) {

    }
}
