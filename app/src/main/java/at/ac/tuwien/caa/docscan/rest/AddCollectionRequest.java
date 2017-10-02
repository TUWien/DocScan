package at.ac.tuwien.caa.docscan.rest;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import at.ac.tuwien.caa.docscan.R;

import static android.R.attr.mimeType;

/**
 * Created by fabian on 05.09.2017.
 */

public class AddCollectionRequest extends RestRequest.JSONObjectRestRequest implements
        StartUploadRequest.StartUploadCallback {

    private static final String URL = "https://transkribus.eu/TrpServerTesting/rest/collections/createCollection";
    private Context mContext;

    public AddCollectionRequest(Context context) {

        super(context);
        mMethod = Request.Method.POST;
        RequestHandler.processJsonRequest(this);
        mContext = context;

    }

    @Override
    public String getUrl() {
        return URL;
    }

    @Override
    public void handleResponse(JSONObject response) {
        new StartUploadRequest(mContext);
    }

    @Override
    public void onUploadStart(int uploadId) {

    }
}
