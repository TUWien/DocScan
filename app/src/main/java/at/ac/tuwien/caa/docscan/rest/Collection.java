package at.ac.tuwien.caa.docscan.rest;

/**
 * Created by fabian on 29.06.2017.
 */

public class Collection {

    private int mID;
    private String mName;
    private String mRole;

    public Collection(int id, String name, String role) {

        mID = id;
        mName = name;
        mRole = role;

    }

    public String getName() {
        return mName;
    }

    public int getID() {
        return mID;
    }

    public String getRole() {
        return mRole;
    }
}
