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
 * Created by fabian on 29.06.2017.
 */

public class DocumentsMetaDataRequest extends RestRequest.JSONArrayRestRequest {

    private static final String ID_ID = "docId";
    private static final String TITLE_ID = "title";

    private Collection mCollection;
    // GET statement for documents: https://transkribus.eu/TrpServer/rest/collections/{collection-ID}/list
    private final static String[] URL = {"collections/", "/list"};

    public DocumentsMetaDataRequest(Context context, final Collection collection) {
        super(context);
        mMethod = Request.Method.GET;

        mCollection = collection;
        RequestHandler.processJsonRequest(this);
    }

    @Override
    public String getExtendedUrl() {
        return URL[0] + mCollection.getID() + URL[1];
    }


    @Override
    public void handleResponse(JSONArray response) {

        try {
            List<TranskribusDocumentMetaDataRequest> metaDatas = new ArrayList<TranskribusDocumentMetaDataRequest>();

            for (int i = 0; i < response.length(); i++) {
                JSONObject o = response.getJSONObject(i);
                int id = o.getInt(ID_ID);
                String name = o.getString(TITLE_ID);

                TranskribusDocumentMetaDataRequest document = new TranskribusDocumentMetaDataRequest(id, name, mCollection);
                metaDatas.add(document);
            }

            ((DocumentsMetaDataCallback) mRestCallback).onDocumentsMetaData(metaDatas);

        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

    }

    public interface DocumentsMetaDataCallback extends RestRequest.RestCallback {

        void onDocumentsMetaData(List<TranskribusDocumentMetaDataRequest> transkribusDocumentMetaDatumRequests);

    }
}
