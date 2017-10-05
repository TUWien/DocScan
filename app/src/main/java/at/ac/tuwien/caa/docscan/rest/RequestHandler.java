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
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

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

    public static void processLoginRequest(final LoginRequest request) {

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

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(request.getContext());
        requestQueue.add(stringRequest);


    }

    public static void processRequest(final RestRequest.XMLRequest request) {

        int method = request.getMethod();

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
                        // TODO: error handling
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Cookie", "JSESSIONID=" + User.getInstance().getSessionID());
                return headers;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(request.getContext());
        requestQueue.add(stringRequest);
    }

    public static void processJsonRequest(final RestRequest request) {

        Request r = null;
        if (request instanceof RestRequest.JSONObjectRestRequest)
            r = getObjectRequest((RestRequest.JSONObjectRestRequest) request);
        else
            r = getArrayRequest((RestRequest.JSONArrayRestRequest) request);


        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(request.getContext());
        requestQueue.add(r);

    }

    public static void processMultiPartRequest(Context context,  MultipartRequest multipartRequest) {

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(context);
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
                        // TODO: error handling
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Cookie", "JSESSIONID=" + User.getInstance().getSessionID());
                return headers;
            }
        };

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
                        // TODO: error handling
                    }
                }) {
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


}
