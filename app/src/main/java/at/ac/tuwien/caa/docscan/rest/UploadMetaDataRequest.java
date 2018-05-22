package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 5/8/2018.
 */

public class UploadMetaDataRequest extends RestRequest.JSONObjectRestRequest {

    private int mUploadId;

    public UploadMetaDataRequest(Context context, int uploadId) {

        super(context);
        mMethod = Request.Method.GET;
        mUploadId = uploadId;

        RequestHandler.processJsonRequest(this);

    }

    @Override
    public String getExtendedUrl() {

        return "uploads/" + Integer.toString(mUploadId);

    }

    @Override
    public void handleResponse(JSONObject response) {

    }

    public interface UploadMetaDataCallback extends RestRequest.RestCallback{

        void onMetaData(int uploadId, String title);

    }
}
