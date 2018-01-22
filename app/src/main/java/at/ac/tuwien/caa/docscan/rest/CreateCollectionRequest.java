package at.ac.tuwien.caa.docscan.rest;


import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 29.06.2017.
 */

public class CreateCollectionRequest extends RestRequest.JSONObjectRestRequest {

    //    JSON method for retrieving documents: https://transkribus.eu/TrpServer/rest/collections/{collection-ID}/{document-ID}/fulldoc
    private final static String URL = "collections/createCollection?collName=";
    private String mCollName;

    public CreateCollectionRequest(Context context, String collName) {
        super(context);
        mMethod = Request.Method.POST;
        mCollName = collName;

        RequestHandler.processJsonRequest(this);

    }

    @Override
    public String getExtendedUrl() {
//        Escape whitespaces:
        String urlColName = mCollName.replace(" ", "%20");

        return URL + urlColName;
    }

    @Override
    public void handleResponse(JSONObject response) {

        ((CreateCollectionCallback) mRestCallback).onCollectionCreated(mCollName);

    }

    public interface CreateCollectionCallback extends RestRequest.RestCallback{

        void onCollectionCreated(String collName);

    }
}
