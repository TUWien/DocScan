package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

/**
 * Created by fabian on 01.12.2016.
 */
public class LoginRequest extends RestRequest {

    private static final String SESSION_ID_START = "<sessionId>";
    private static final String SESSION_ID_END = "</sessionId>";
    private static final String FIRST_NAME_ID_START = "<firstname>";
    private static final String FIRST_NAME_ID_END = "</firstname>";
    private static final String LAST_NAME_ID_START = "<lastname>";
    private static final String LAST_NAME_ID_END = "</lastname>";


    public LoginRequest(Context context) {

        super(context);
        mUrl = "https://transkribus.eu/TrpServer/rest/auth/login";

        RequestHandler.processLoginRequest(this);

    }

    public void handleResponse(String response) {

        String id = getSessionID(response);
        String firstName = getFirstName(response);
        String lastName = getLastName(response);

        if (!id.isEmpty()) {
            User.getInstance().setLoggedIn(true);
            User.getInstance().setSessionID(id);
            User.getInstance().setFirstName(firstName);
            User.getInstance().setLastName(lastName);
//            Now update the GUI with the user data:
            ((LoginCallback) mRestCallback).onLogin(User.getInstance());
        }
        else
            User.getInstance().setLoggedIn(false);

    }

    public void handleLoginError() {

        ((LoginCallback) mRestCallback).onLoginError();

    }

    private String getSessionID(String response) {

        return super.findString(response, SESSION_ID_START, SESSION_ID_END);

//        String sessionID = "";
//        int startIdx = response.indexOf(SESSION_ID_START);
//        int endIdx = response.indexOf(SESSION_ID_END);
//        if (startIdx != -1 && endIdx != -1) {
//            startIdx += SESSION_ID_START.length();
//            sessionID = response.substring(startIdx, endIdx);
//        }
//
//        return sessionID;
    }

    private String getFirstName(String response) {

        return super.findString(response, FIRST_NAME_ID_START, FIRST_NAME_ID_END);

    }

    private String getLastName(String response) {

        return super.findString(response, LAST_NAME_ID_START, LAST_NAME_ID_END);

    }


    public interface LoginCallback extends RestRequest.RestCallback{
        void onLogin(User user);
        void onLoginError();
    }

}
