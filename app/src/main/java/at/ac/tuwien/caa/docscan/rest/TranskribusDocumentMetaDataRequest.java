package at.ac.tuwien.caa.docscan.rest;

/**
 * Created by fabian on 29.06.2017.
 */

public class TranskribusDocumentMetaDataRequest {

    private int mID;
    private String mName;
    private Collection mCollection;

    public TranskribusDocumentMetaDataRequest(int id, String name, final Collection collection) {

        mID = id;
        mName = name;
        mCollection = collection;

    }

    public int getID() {
        return mID;
    }

    public final Collection getCollection() {
        return mCollection;
    }


}
