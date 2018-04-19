package at.ac.tuwien.caa.docscan.ui.document;

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

public class DocumentMetaData {

    private static final String AUTHORITY = "authority";
    private static final String TITLE = "title";
    private static final String IDENTIFIER = "identifier";
    private static final String TYPE = "type";
    private static final String TYPE_HIERARCHY = "hierarchy description";
    private static final String TYPE_URI = "uri";
    private static final String SIGNATURE = "callNumber";
    private static final String DESCRIPTION = "description";
    private static final String DATE = "date";

    private String mTitle;
    private String mDescription;
    private String mAuthority;
    private String mHierarchy;
    private String mUri;
    private String mSignature;
    private Date mDate;

    public String getTitle() { return mTitle; }
    public String getDescription() { return mDescription; }
    public String getAuthority() { return mAuthority; }
    public String getHierarchy() { return mHierarchy; }
    public String getUri() { return mUri; }
    public String getSignature() { return mSignature; }
    public Date getDate() { return mDate; }

    private DocumentMetaData() {

    }

    private static class IdentifierValue {

        private String mIdentifier, mValue;

        private IdentifierValue(String identifier, String value) {
            mIdentifier = identifier;
            mValue = value;
        }

        private String getIdentifier() { return mIdentifier; }
        private String getValue() { return mValue; }
    }

    /**
     * Escapes characters that are not allowed for XML string. Replaces the characters with entity
     * references.
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

    public static DocumentMetaData parseXML(String text) {

        try {

            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xmlFactoryObject.newPullParser();

            String escapedText = escapeReferences(text);

            InputStream stream = new ByteArrayInputStream(escapedText.getBytes(Charset.forName("UTF-8")));
            parser.setInput(stream, null);

            DocumentMetaData documentMetaData = new DocumentMetaData();

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();

                if (name.equals(AUTHORITY))
                    documentMetaData.mAuthority = readTextFromTag(parser, AUTHORITY);

                else if (name.equals(IDENTIFIER)) {
                    IdentifierValue iv = readIdentifier(parser);
                    if (iv.getIdentifier().equals(TYPE_HIERARCHY))
                        documentMetaData.mHierarchy = iv.getValue();
                    else if (iv.getIdentifier().equals(TYPE_URI))
                        documentMetaData.mUri = iv.getValue();
                }
                else if (name.equals(TITLE))
                    documentMetaData.mTitle = readTextFromTag(parser, TITLE);
                else if (name.equals(DESCRIPTION))
                    documentMetaData.mDescription = readTextFromTag(parser, DESCRIPTION);
                else if (name.equals(SIGNATURE))
                    documentMetaData.mSignature = readTextFromTag(parser, SIGNATURE);
                else if (name.equals(DATE))
                    skip(parser);

            }

            return documentMetaData;


        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;

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

        parser.require(XmlPullParser.START_TAG, null, IDENTIFIER);
        String tag = parser.getName();
        String identifierType = parser.getAttributeValue(null, TYPE);
        String value = null;

        if (tag.equals(IDENTIFIER)) {
            if (identifierType.equals(TYPE_HIERARCHY) || identifierType.equals(TYPE_URI))
                value = readText(parser);
        }

        parser.require(XmlPullParser.END_TAG, null, IDENTIFIER);

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
