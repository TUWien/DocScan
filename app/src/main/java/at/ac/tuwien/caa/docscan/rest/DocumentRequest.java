package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 29.06.2017.
 */

public class DocumentRequest extends RestRequest.JSONObjectRestRequest {

    private TranskribusDocumentMetaDataRequest mTranskribusDocumentMetaDataRequest;
    //    JSON method for retrieving documents: https://transkribus.eu/TrpServer/rest/collections/{collection-ID}/{document-ID}/fulldoc
    private final static String[] URL = {"collections/", "/fulldoc"};

    public DocumentRequest(Context context, TranskribusDocumentMetaDataRequest transkribusDocumentMetaDataRequest) {
        super(context);
        mMethod = Request.Method.GET;
        mTranskribusDocumentMetaDataRequest = transkribusDocumentMetaDataRequest;

        RequestHandler.processJsonRequest(this);

    }

    @Override
    public String getExtendedUrl() {
        return URL[0] + mTranskribusDocumentMetaDataRequest.getCollection().getID() + "/" + mTranskribusDocumentMetaDataRequest.getID() + URL[1];
    }

    @Override
    public void handleResponse(JSONObject response) {

    }

//    public interface DocumentCallback extends RestRequest.RestCallback{
//
//        void onDocument(TranskribusDocumentMetaDataRequest document);
//
//    }
}
