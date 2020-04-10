package at.ac.tuwien.caa.docscan.rest;

/**
 * Created by fabian on 01.12.2016.
 */

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


public class RequestHandler {

    public static final int REQUEST_LOGIN = 0;
    public static final int REQUEST_COLLECTIONS = 1;


    public static void createRequest(Context context, int requestType) {

        switch (requestType) {
            case REQUEST_LOGIN:
                new LoginRequest(context);
                break;
            case REQUEST_COLLECTIONS:
                new CollectionsRequest(context);
        }

    }

    /**
     * Creates a new request queue. If API is between JELLY_BEAN and KIT_KAT a HttpStack with
     * TLSSocketFactory is generated. This fixes the SSLHandshakeException on Android 4 devices.
     * Taken from: https://stackoverflow.com/questions/31269425/how-do-i-tell-the-tls-version-in-android-volley
     * @param context
     * @return
     */
    public static RequestQueue createRequestQueue(Context context) {

        RequestQueue requestQueue;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            HttpStack stack = null;
            try {
                stack = new HurlStack(null, new TLSSocketFactory());
            } catch (KeyManagementException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                Log.d("Your Wrapper Class", "Could not create new stack for TLS v1.2");
                stack = new HurlStack();
            } catch (NoSuchAlgorithmException e) {
                Crashlytics.logException(e);
                e.printStackTrace();
                Log.d("Your Wrapper Class", "Could not create new stack for TLS v1.2");
                stack = new HurlStack();
            }
            requestQueue = Volley.newRequestQueue(context, stack);
        } else {
            requestQueue = Volley.newRequestQueue(context);
        }

        return requestQueue;
    }

    public static void processLoginRequest(final LoginRequest request) {

//        checkProviderInstaller(request);


        StringRequest stringRequest = new StringRequest(Request.Method.POST, request.getURL(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        request.handleResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        request.handleLoginError();
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                //Adding parameters to request
                params.put("user", User.getInstance().getUserName());
                params.put("pw", User.getInstance().getPassword());

                return params;
            }

        };

        //Set a retry policy in case of SocketTimeout & ConnectionTimeout Exceptions.
        //Volley does retry for you if you have specified the policy.
        if (stringRequest != null)
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 2, 2));

        RequestQueue requestQueue = createRequestQueue(request.getContext());
        requestQueue.add(stringRequest);

    }


    public static void processRequest(final RestRequest.XMLRequest request) {

        int method = request.getMethod();
// TODO: I do not know why this has to be a XML Request although the response is a JSO

        StringRequest stringRequest = new StringRequest(method, request.getURL(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        request.handleResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        request.handleRestError(error);
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Cookie", "JSESSIONID=" + User.getInstance().getSessionID());
                return headers;
            }
        };

        //Set a retry policy in case of SocketTimeout & ConnectionTimeout Exceptions.
        //Volley does retry for you if you have specified the policy.
        if (stringRequest != null)
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 2, 2));
//            stringRequest.setRetryPolicy(new DefaultRetryPolicy(100,1, 1));

        //Adding the string request to the queue
//        RequestQueue requestQueue = Volley.newRequestQueue(request.getContext());
        RequestQueue requestQueue = createRequestQueue(request.getContext());
        requestQueue.add(stringRequest);
    }

    public static void processJsonRequest(final RestRequest request) {

        Request r = null;
        if (request instanceof RestRequest.JSONObjectRestRequest)
            r = getObjectRequest((RestRequest.JSONObjectRestRequest) request);
        else
            r = getArrayRequest((RestRequest.JSONArrayRestRequest) request);


        //Adding the string request to the queue
//        RequestQueue requestQueue = Volley.newRequestQueue(request.getContext());
        RequestQueue requestQueue = createRequestQueue(request.getContext());
        requestQueue.add(r);

    }

    public static void processMultiPartRequest(Context context,  MultipartRequest multipartRequest) {

        //Adding the string request to the queue
//        RequestQueue requestQueue = Volley.newRequestQueue(context);
        RequestQueue requestQueue = createRequestQueue(context);
        requestQueue.add(multipartRequest);

    }

    private static JsonArrayRequest getArrayRequest(final RestRequest.JSONArrayRestRequest request) {


        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(request.getMethod(), request.getURL(), request.getJSONArray(),
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        request.handleResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        request.handleRestError(error);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Cookie", "JSESSIONID=" + User.getInstance().getSessionID());
                return headers;
            }
        };

        //Set a retry policy in case of SocketTimeout & ConnectionTimeout Exceptions.
        //Volley does retry for you if you have specified the policy.
        if (jsArrayRequest != null)
            jsArrayRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 2, 2));
//            jsArrayRequest.setRetryPolicy(new DefaultRetryPolicy(100,1, 1));

        return jsArrayRequest;

    }

    private static JsonObjectRequest getObjectRequest(final RestRequest.JSONObjectRestRequest request) {

        JsonObjectRequest jsArrayRequest = new JsonHelperRequest(request.getMethod(), request.getURL(), request.getJSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        request.handleResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        request.handleRestError(error);

                        NetworkResponse response = error.networkResponse;
                        if(response != null && response.data != null){
                            String json = new String(response.data);
                            json = trimMessage(json, "message");
                        }

                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Cookie", "JSESSIONID=" + User.getInstance().getSessionID());
                return headers;
            }

            @Override
            public String getBodyContentType(){
                return "application/json";
            }
        };

        jsArrayRequest.setShouldCache(false);

        //Set a retry policy in case of SocketTimeout & ConnectionTimeout Exceptions.
        //Volley does retry for you if you have specified the policy.
        if (jsArrayRequest != null)
            jsArrayRequest.setRetryPolicy(new DefaultRetryPolicy(5000, 2, 2));
//            jsArrayRequest.setRetryPolicy(new DefaultRetryPolicy(100,1, 1));

//        jsArrayRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return jsArrayRequest;

    }

//    private static int getMethod(final RestRequest request) {
//
//        int method;
//        if (request instanceof CollectionsRequest)
//            method = Request.Method.GET;
//        else
//            method = Request.Method.GET;
//
//        return method;
//
//    }

    private static String trimMessage(String json, String key){
        String trimmedString = null;

        try{
            JSONObject obj = new JSONObject(json);
            trimmedString = obj.getString(key);
        } catch(JSONException e){
            Crashlytics.logException(e);
            e.printStackTrace();
            return null;
        }

        return trimmedString;
    }



}
