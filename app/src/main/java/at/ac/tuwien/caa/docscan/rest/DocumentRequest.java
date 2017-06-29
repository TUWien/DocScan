package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;

import com.android.volley.Request;

import org.json.JSONObject;

/**
 * Created by fabian on 29.06.2017.
 */

public class DocumentRequest extends RestRequest.JSONObjectRestRequest {

    private DocumentMetaData mDocumentMetaData;
//    JSON method for retrieving documents: https://transkribus.eu/TrpServer/rest/collections/{collection-ID}/{document-ID}/fulldoc
    private final static String[] URL = {"https://transkribus.eu/TrpServer/rest/collections/", "/fulldoc"};

    public DocumentRequest(Context context, DocumentMetaData documentMetaData) {
        super(context);
        mMethod = Request.Method.GET;
        mDocumentMetaData = documentMetaData;

        RequestHandler.processJsonRequest(this);

    }

    @Override
    public String getUrl() {
        return URL[0] + mDocumentMetaData.getCollection().getID() + "/" + mDocumentMetaData.getID() + URL[1];
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
