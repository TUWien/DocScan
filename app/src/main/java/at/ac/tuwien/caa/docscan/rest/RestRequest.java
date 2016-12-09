package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import org.json.JSONArray;

/**
 * Created by fabian on 01.12.2016.
 */
public abstract class RestRequest {

    private Context mContext;
    protected String mUrl;
    protected RestCallback mRestCallback;

    public RestRequest(Context context) {

        mContext = context;
        mRestCallback = (RestCallback) context;
    }

    public String getUrl() {

        return mUrl;

    }

    public Context getContext() {

        return mContext;

    }

    public abstract void handleResponse(String response);

    public void handleResponse(JSONArray response) {

    }

    /**
     * Callback interface. This interface is used to hold a generic reference to the callback.
     * The actual interface should be implemented in the child classes of RestRequest.
     */
    public interface RestCallback {

    }
}
