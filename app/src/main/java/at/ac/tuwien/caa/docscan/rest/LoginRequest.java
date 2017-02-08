package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

/**
 * Created by fabian on 01.12.2016.
 */
public class LoginRequest extends RestRequest {

    private static final String SESSION_ID_START = "<sessionId>";
    private static final String SESSION_ID_END = "</sessionId>";

    public LoginRequest(Context context) {

        super(context);
        mUrl = "https://transkribus.eu/TrpServer/rest/auth/login";

        RequestHandler.processLoginRequest(this);

    }

    public void handleResponse(String response) {

        String id = getSessionID(response);

        if (!id.isEmpty()) {
            User.getInstance().setLoggedIn(true);
            User.getInstance().setSessionID(id);
            ((LoginCallback) mRestCallback).onLogin(User.getInstance());
        }
        else
            User.getInstance().setLoggedIn(false);

    }

    private static String getSessionID(String response) {

        String sessionID = "";
        int startIdx = response.indexOf(SESSION_ID_START);
        int endIdx = response.indexOf(SESSION_ID_END);
        if (startIdx != -1 && endIdx != -1) {
            startIdx += SESSION_ID_START.length();
            sessionID = response.substring(startIdx, endIdx);
        }

        return sessionID;
    }


    public interface LoginCallback extends RestRequest.RestCallback{
        void onLogin(User user);
    }

}
