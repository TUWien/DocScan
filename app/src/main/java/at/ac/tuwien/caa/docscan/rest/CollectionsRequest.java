package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fabian on 05.12.2016.
 */
public class CollectionsRequest extends RestRequest.JSONArrayRestRequest {

    //    private CollectionsCallback mCollectionsCallback;
    private static final String ID_ID = "colId";
    private static final String NAME_ID = "colName";
    private static final String ROLE_ID = "role";

    private static final String URL = "collections/list";

    public CollectionsRequest(Context context) {

        super(context);
        mMethod = Request.Method.GET;

        RequestHandler.processJsonRequest(this);
    }

    @Override
    public String getExtendedUrl() {
        return URL;
    }


    @Override
    public void handleResponse(JSONArray response) {


        try {
            List<Collection> collections = new ArrayList<Collection>();

            for (int i = 0; i < response.length(); i++) {
                JSONObject o = response.getJSONObject(i);
                int id = o.getInt(ID_ID);
                String name = o.getString(NAME_ID);
                String role = o.getString(ROLE_ID);

                Collection collection = new Collection(id, name, role);
                collections.add(collection);
            }

            ((CollectionsCallback) mRestCallback).onCollections(collections);

        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public interface CollectionsCallback extends RestRequest.RestCallback {

        void onCollections(List<Collection> collections);

    }
}
