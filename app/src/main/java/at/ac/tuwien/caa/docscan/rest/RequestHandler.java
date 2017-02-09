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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;


public class RequestHandler {

    public static final int REQUEST_LOGIN = 0;
    public static final int REQUEST_COLLECTIONS = 1;


    public static void createRequest(Object context, int requestType) {

        Context c = (Context) context;

        switch (requestType) {
            case REQUEST_LOGIN:
                new LoginRequest(c);
                break;
            case REQUEST_COLLECTIONS:
                new CollectionsRequest(c);
        }

    }

    public static void processLoginRequest(final LoginRequest request) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, request.getUrl(),
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

    public static void processRequest(final RestRequest request) {

        int method = getMethod(request);

        StringRequest stringRequest = new StringRequest(method, request.getUrl(),
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

        int method = getMethod(request);

        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(method, request.getUrl(), null,
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

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(request.getContext());
        requestQueue.add(jsArrayRequest);

    }

    private static int getMethod(final RestRequest request) {

        int method;
        if (request instanceof CollectionsRequest)
            method = Request.Method.GET;
        else
            method = Request.Method.GET;

        return method;

    }


}
