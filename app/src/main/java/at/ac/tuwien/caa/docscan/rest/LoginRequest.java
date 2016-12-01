package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

/**
 * Created by fabian on 01.12.2016.
 */
public class LoginRequest extends RestRequest {

    public LoginRequest(Context context) {

        super(context);
        mUrl = "https://transkribus.eu/TrpServer/rest/auth/login";

        RestQuest.processLoginRequest(this);
    }

    public void handleResponse(String response) {



    }

}
