package at.ac.tuwien.caa.docscan.logic;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by fabian on 28.11.2017.
 */

public class TranskribusMetaData {

    public static final int NO_RELATED_UPLOAD_ID_ASSIGNED = -1;

    //    Strings needed to parse QR codes:
    private static final String QR_RELATED_UPLOAD_ID = "relatedUploadId";
    private static final String QR_AUTHORITY = "authority";
    private static final String QR_TITLE = "title";
    private static final String QR_IDENTIFIER = "identifier";
    private static final String QR_TYPE = "type";
    private static final String QR_TYPE_HIERARCHY = "hierarchy description";
    private static final String QR_TYPE_URI = "uri";
    private static final String QR_SIGNATURE = "callNumber";
    private static final String QR_DESCRIPTION = "description";
    private static final String QR_DATE = "date";
    private static final String QR_BACKLINK = "backlink";

    private String mTitle;
    private String mAuthority;
    private String mHierarchy;
    private String mSignature;
    //    private String mUri;
    private String mAuthor;
    private String mGenre;
    private String mWriter;
    private String mDescription;
    private Date mDate;
    private String mUrl;
    private Integer mRelatedUploadId;
    private String mLanguage;
    private boolean mReadme2020 = false;
    private boolean mReadme2020Public = false;

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public void setLanguage(String language) {
        mLanguage = language;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String authority) {
        mAuthority = authority;
    }

    public String getHierarchy() {
        return mHierarchy;
    }

    public void setHierarchy(String hierarchy) {
        mHierarchy = hierarchy;
    }

    //    public String getUri() { return mUri; }
    public String getSignature() {
        return mSignature;
    }

    public void setSignature(String signature) {
        mSignature = signature;
    }

    public Date getDate() {
        return mDate;
    }

    public Integer getRelatedUploadId() {
        return mRelatedUploadId;
    }

    public String getGenre() {
        return mGenre;
    }

    public void setGenre(String genre) {
        mGenre = genre;
    }

    public String getWriter() {
        return mWriter;
    }

    public void setWriter(String writer) {
        mWriter = writer;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void setReadme2020(boolean readme2020) {
        mReadme2020 = readme2020;
    }

    public boolean getReadme2020() {
        return mReadme2020;
    }

    public void setReadme2020Public(boolean isPublic) {
        mReadme2020Public = isPublic;
    }

    public boolean getReadme2020Public() {
        return mReadme2020Public;
    }


    public TranskribusMetaData() {

    }

    public String toJSON() {

        Gson gson = new GsonBuilder().setFieldNamingPolicy(
                FieldNamingPolicy.UPPER_CAMEL_CASE).create();
        String json = gson.toJson(this);

        return json;

    }

    public String getLink() {
        return mUrl;
    }

    private static class IdentifierValue {

        private String mIdentifier, mValue;

        private IdentifierValue(String identifier, String value) {
            mIdentifier = identifier;
            mValue = value;
        }

        private String getIdentifier() {
            return mIdentifier;
        }

        private String getValue() {
            return mValue;
        }
    }

    /**
     * Escapes characters that are not allowed for XML string. Replaces the characters with entity
     * references.
     *
     * @param text
     * @return
     */
    private static String escapeReferences(String text) {

        String result = text;
        result = result.replace("&", "&amp;");
//        result = result.replace("<", "&lt");
//        result = result.replace(">", "&gt");
//        result = result.replace("\"", "&quot;");
//        result = result.replace("'", "&apos");

        return result;

    }

    public static TranskribusMetaData parseXML(String text) {

        try {

            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xmlFactoryObject.newPullParser();

            String escapedText = escapeReferences(text);

            InputStream stream = new ByteArrayInputStream(escapedText.getBytes(Charset.forName("UTF-8")));
            parser.setInput(stream, null);

            TranskribusMetaData transkribusMetaData = new TranskribusMetaData();

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();

                if (name.equals(QR_AUTHORITY))
                    transkribusMetaData.mAuthority = readTextFromTag(parser, QR_AUTHORITY);

                else if (name.equals(QR_IDENTIFIER)) {
                    IdentifierValue iv = readIdentifier(parser);
                    if (iv.getIdentifier().equals(QR_TYPE_HIERARCHY))
                        transkribusMetaData.mHierarchy = iv.getValue();
//                    else if (iv.getIdentifier().equals(QR_TYPE_URI))
//                        transkribusMetaData.mUri = iv.getValue();
                } else if (name.equals(QR_TITLE))
                    transkribusMetaData.mTitle = readTextFromTag(parser, QR_TITLE);
                else if (name.equals(QR_DESCRIPTION))
                    transkribusMetaData.mDescription = readTextFromTag(parser, QR_DESCRIPTION);
                else if (name.equals(QR_SIGNATURE))
                    transkribusMetaData.mSignature = readTextFromTag(parser, QR_SIGNATURE);
                else if (name.equals(QR_RELATED_UPLOAD_ID)) {
                    String rId = readTextFromTag(parser, QR_RELATED_UPLOAD_ID);
                    transkribusMetaData.mRelatedUploadId = Integer.valueOf(rId);
                } else if (name.equals(QR_BACKLINK))
                    transkribusMetaData.mUrl = readTextFromTag(parser, QR_BACKLINK);

                else if (name.equals(QR_DATE))
                    skip(parser);

            }

            return transkribusMetaData;

        } catch (Exception e) {
            return null;
        }


    }


    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private static String readTextFromTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, tag);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, tag);
        return title;
    }


    private static IdentifierValue readIdentifier(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, null, QR_IDENTIFIER);
        String tag = parser.getName();
        String identifierType = parser.getAttributeValue(null, QR_TYPE);
        String value = null;

        if (tag.equals(QR_IDENTIFIER)) {
            if (identifierType.equals(QR_TYPE_HIERARCHY) || identifierType.equals(QR_TYPE_URI))
                value = readText(parser);
        }

        parser.require(XmlPullParser.END_TAG, null, QR_IDENTIFIER);

        return new IdentifierValue(identifierType, value);

    }


    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {

        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;

    }


}
