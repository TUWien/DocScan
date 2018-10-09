package at.ac.tuwien.caa.docscan.ui.document;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.logic.Document;

/**
 * This class is used to generate JSON strings that can be used for the upload to Transkribus.
 * Note that we do not directly generate a JSON from the document, because we have to exclude several
 * values from the JSON that are needed in DocumentStorage (which uses the the default JSON parser).
 */
public class DocumentJSONParser {

    private static String CLASS_NAME = "DocumentJSONParser";

    public static String toJSONString(Document document) {

        JSONDocument jsonDocument = new JSONDocument(document);

        Gson gson = new GsonBuilder().setFieldNamingPolicy(
                FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        String json = gson.toJson(jsonDocument);

        return json;


    }


    private static ArrayList<JSONPage> toJSONPageList(Document document) {

        ArrayList<JSONPage> pageList = new ArrayList<>();

        if (document != null && document.getPages() != null) {
            for (int i = 0; i < document.getPages().size(); i++)
                pageList.add(new JSONPage(document, i));
        }
        
        return pageList;

    }



    private static class JSONDocument {

        //    String serialization needed for generating JSON strings from the member fields:
        private static final String JSON_METADATA =             "md";
        private static final String JSON_RELATED_UPLOAD_ID =    "relatedUploadId";
        private static final String JSON_PAGE_LIST =            "pageList";

        @SerializedName(JSON_METADATA)                          private JSONMetaData mMetaData;
        @SerializedName(JSON_RELATED_UPLOAD_ID)                 private Integer mRelatedUploadId;
        @SerializedName(JSON_PAGE_LIST)                         private JSONPageList mPageList;

        private JSONDocument(Document document) {

            mMetaData = new JSONMetaData(document);
            if (document.getMetaData() != null)
                mRelatedUploadId = document.getMetaData().getRelatedUploadId();

            if (document.getPages() != null && !document.getPages().isEmpty())
                mPageList = new JSONPageList(document);

        }


    }

    private static class JSONPageList {

        private static final String JSON_PAGES =                 "pages";

        @SerializedName(JSON_PAGES)                              private ArrayList<JSONPage> mPages;

        private JSONPageList(Document document) {

            mPages = toJSONPageList(document);

        }

    }

    private static class JSONPage {

        //    String serialization needed for generating JSON strings from the member fields:
        private static final String JSON_FILE =                 "fileName";
        private static final String JSON_PAGE_NR =              "pageNr";

        @SerializedName(JSON_FILE)                              private String mFileName;
        @SerializedName(JSON_PAGE_NR)                           private int mPageNr;


        private JSONPage(Document document, int pageNr) {
            mFileName = document.getPages().get(pageNr).getFile().getName();
            mPageNr = pageNr + 1;

        }

    }



    private static class JSONMetaData {

        //    String serialization needed for generating JSON strings from the member fields:
        private static final String JSON_TITLE =                "title";
        private static final String JSON_AUTHORITY =            "authority";
        private static final String JSON_HIERARCHY =            "hierarchy";
        private static final String JSON_SIGNATURE =            "extid";
        private static final String JSON_URI =                  "backlink";

        @SerializedName(JSON_TITLE)                             private String mTitle;
        @SerializedName(JSON_AUTHORITY)                         private String mAuthority;
        @SerializedName(JSON_HIERARCHY)                         private String mHierarchy;
        @SerializedName(JSON_SIGNATURE)                         private String mSignature;
        @SerializedName(JSON_URI)                               private String mUri;

        private JSONMetaData(Document document) {
            mTitle = document.getTitle();
            if (document.getMetaData() != null)
                setMetaDataValues(document.getMetaData());
        }

        private void setMetaDataValues(DocumentMetaData md) {

//            mTitle = md.getTitle();
            mAuthority = md.getAuthority();
            mHierarchy = md.getHierarchy();
            mSignature = md.getSignature();
            mUri = md.getUri();

        }
    }
}
