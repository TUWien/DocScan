package at.ac.tuwien.caa.docscan.rest;


import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 29.06.2017.
 */

public class CreateCollectionRequest extends RestRequest.JSONObjectRestRequest {

    //    JSON method for retrieving documents: https://transkribus.eu/TrpServer/rest/collections/{collection-ID}/{document-ID}/fulldoc
    private final static String URL = "https://transkribus.eu/TrpServerTesting/rest/collections/createCollection";

    public CreateCollectionRequest(Context context) {
        super(context);
        mMethod = Request.Method.POST;

        RequestHandler.processJsonRequest(this);

    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public void handleResponse(JSONObject response) {

    }

//    public interface DocumentCallback extends RestRequest.RestCallback{
//
//        void onDocument(Document document);
//
//    }
}
