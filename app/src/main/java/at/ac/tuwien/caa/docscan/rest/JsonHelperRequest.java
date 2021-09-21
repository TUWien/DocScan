package at.ac.tuwien.caa.docscan.rest;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import fr.arnaudguyon.xmltojsonlib.XmlToJson;

/**
 * The only purpose of this class is to convert a XML response into a JSON response. This is needed
 * for document upload via Transkribus Rest API.
 */

public class JsonHelperRequest extends JsonObjectRequest {

    public JsonHelperRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    /**
     * Converts a XML response into a JSON response with the help of HttpHeaderParser.
     *
     * @param response
     * @return
     */
    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {

        try {

            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            XmlToJson xmlToJson = new XmlToJson.Builder(jsonString).build();
            return Response.success(new JSONObject(xmlToJson.toString()),
                    HttpHeaderParser.parseCacheHeaders(response));

        } catch (UnsupportedEncodingException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            FirebaseCrashlytics.getInstance().recordException(je);
            return Response.error(new ParseError(je));
        }

    }
}
