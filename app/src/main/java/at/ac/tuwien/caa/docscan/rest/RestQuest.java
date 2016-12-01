package at.ac.tuwien.caa.docscan.rest;

/**
 * Created by fabian on 01.12.2016.
 */

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class RestQuest {

    private static RestQuest mInstance = null;
    private String mUserName;
    private String mPassword;
    private boolean mIsLoggedin;
    private String mSessionID;

    private static final String SESSION_ID_START = "<sessionId>";
    private static final String SESSION_ID_END = "</sessionId>";


    public static void createLoginRequest(Context context) {

        LoginRequest r = new LoginRequest(context);

    }

    public static void processLoginRequest(final LoginRequest request) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, request.getUrl(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        request.handleResponse(response);

//                        String sessionID = getSessionID(response);
//                        if (!mSessionID.isEmpty())
//                            mIsLoggedin = true;
//
//                        loginCallback.loginResponse(true);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //You can handle error here if you want
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                //Adding parameters to request
                params.put("user", User.getInstance().getUserName());
                params.put("pw", User.getInstance().getUserName());

                return params;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(request.getContext());
        requestQueue.add(stringRequest);


    }



//    public static void login(Context context) {
//
//        RestLoginCallback loginCallback = (RestLoginCallback) context;
//
//        boolean result = false;
//
//        String url = "https://transkribus.eu/TrpServer/rest/auth/login";
////        String url = "https://transkribus.eu/TrpServer/rest/collections/list.xml";
//        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//
//                        String sessionID = getSessionID(response);
//                        if (!mSessionID.isEmpty())
//                            mIsLoggedin = true;
//
//                        loginCallback.loginResponse(true);
//
//                    }
//                },
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        //You can handle error here if you want
//                    }
//                }) {
//
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//                Map<String,String> params = new HashMap<>();
//                //Adding parameters to request
//                params.put("user", mUserName);
//                params.put("pw", mPassword);
//
//                return params;
//            }
//        };
//
//        //Adding the string request to the queue
//        RequestQueue requestQueue = Volley.newRequestQueue(context);
//        requestQueue.add(stringRequest);
//
//
//    }

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

    public interface RestLoginCallback {

        void loginResponse(boolean response);

    }



}
