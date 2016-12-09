package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by fabian on 05.12.2016.
 */
public class CollectionsRequest extends RestRequest {

//    private CollectionsCallback mCollectionsCallback;

    public CollectionsRequest(Context context) {

        super(context);
        mUrl = "https://transkribus.eu/TrpServer/rest/collections/list";

//        RequestHandler.processRequest(this);
        RequestHandler.processJsonRequest(this);
    }

    @Override
    public void handleResponse(String response) {

        ((CollectionsCallback) mRestCallback).onCollections(User.getInstance());

    }

    @Override
    public void handleResponse(JSONArray response) {

        ((CollectionsCallback) mRestCallback).onCollections(User.getInstance());

        try {
            JSONObject o = response.getJSONObject(0);
            String s = o.getString("colName");

            int b = 0;

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface CollectionsCallback extends RestRequest.RestCallback{

        void onCollections(User user);

    }
}
