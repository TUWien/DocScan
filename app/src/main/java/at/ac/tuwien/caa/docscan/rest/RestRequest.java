package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by fabian on 01.12.2016.
 */
public abstract class RestRequest {

    private Context mContext;
//    protected String mUrl;
    protected RestCallback mRestCallback;
    protected int mMethod;

    public RestRequest(Context context) {

        mContext = context;
        mRestCallback = (RestCallback) context;
    }

//    public String getUrl() {
//        return mUrl;
//    }
    public abstract String getUrl();

    public Context getContext() {
        return mContext;
    }

    public int getMethod() {
        return mMethod;
    }

    public String findString(String response, String prefix, String postfix) {

        String result = "";
        int startIdx = response.indexOf(prefix);
        int endIdx = response.indexOf(postfix);
        if (startIdx != -1 && endIdx != -1) {
            startIdx += prefix.length();
            result = response.substring(startIdx, endIdx);
        }

        return result;

    }

//    public abstract void handleResponse(String response);

    /**
     * Callback interface. This interface is used to hold a generic reference to the callback.
     * The actual interface should be implemented in the child classes of RestRequest.
     */
    public interface RestCallback {

    }

//      We do not need implicit reference from the inner class to the enclosing class, hence use static.
//      Otherwise we get an an enclosing instance that contains X.Y.Z is required error:

    public static abstract class XMLRequest extends RestRequest {

        public XMLRequest(Context context) {
            super(context);
        }

        public abstract void handleResponse(String response);

    }

//      We do not need implicit reference from the inner class to the enclosing class, hence use static.
//      Otherwise we get an an enclosing instance that contains X.Y.Z is required error:
    public static abstract class JSONArrayRestRequest extends RestRequest {

        protected JSONArray mJSONArray = null;

        public JSONArrayRestRequest(Context context) {
            super(context);
        }

        public abstract void handleResponse(JSONArray response);

        public JSONArray getJSONArray() { return mJSONArray; }
    }

    //      We do not need implicit reference from the inner class to the enclosing class, hence use static.
//      Otherwise we get an an enclosing instance that contains X.Y.Z is required error:
    public static abstract class JSONObjectRestRequest extends RestRequest {

        protected JSONObject mJSONObject = null;

        public JSONObjectRestRequest(Context context) {
            super(context);
        }

        public abstract void handleResponse(JSONObject response);

        public JSONObject getJSONObject() { return mJSONObject; }
    }
}
