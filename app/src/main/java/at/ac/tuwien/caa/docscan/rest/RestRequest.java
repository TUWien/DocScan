package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

/**
 * Created by fabian on 01.12.2016.
 */
public abstract class RestRequest {

    private Context mContext;
    protected String mUrl;


    public RestRequest(Context context) {

        mContext = context;

    }

    public String getUrl() {

        return mUrl;

    }

    public Context getContext() {

        return mContext;

    }

    public abstract void handleResponse(String response);
}
