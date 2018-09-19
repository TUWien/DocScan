package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 29.06.2017.
 */

public class DocumentRequest extends RestRequest.JSONObjectRestRequest {

    private TranskribusDocumentMetaData mTranskribusDocumentMetaData;
//    JSON method for retrieving documents: https://transkribus.eu/TrpServer/rest/collections/{collection-ID}/{document-ID}/fulldoc
    private final static String[] URL = {"collections/", "/fulldoc"};

    public DocumentRequest(Context context, TranskribusDocumentMetaData transkribusDocumentMetaData) {
        super(context);
        mMethod = Request.Method.GET;
        mTranskribusDocumentMetaData = transkribusDocumentMetaData;

        RequestHandler.processJsonRequest(this);

    }

    @Override
    public String getExtendedUrl() {
        return URL[0] + mTranskribusDocumentMetaData.getCollection().getID() + "/" + mTranskribusDocumentMetaData.getID() + URL[1];
    }

    @Override
    public void handleResponse(JSONObject response) {

    }

//    public interface DocumentCallback extends RestRequest.RestCallback{
//
//        void onDocument(TranskribusDocumentMetaData document);
//
//    }
}
