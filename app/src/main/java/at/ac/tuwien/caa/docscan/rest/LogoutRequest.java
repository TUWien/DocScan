package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 24.10.2017.
 */

public class LogoutRequest extends RestRequest.JSONObjectRestRequest {

    private static final String URL = "auth/logout";

    public LogoutRequest(Context context) {

        super(context);
        mMethod = Request.Method.POST;

        RequestHandler.processJsonRequest(this);
    }

    @Override
    public String getExtendedUrl() {
        return URL;
    }

    @Override
    public void handleResponse(JSONObject response) {

    }

    public interface LogoutCallback extends RestRequest.RestCallback {

        void onLogout();

    }
}
