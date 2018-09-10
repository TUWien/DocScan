package at.ac.tuwien.caa.docscan.rest;

/**
 * Created by fabian on 01.12.2016.
 */
public class User {

    private static User mInstance = null;

    public static final int SYNC_DROPBOX        = 0;
    public static final int SYNC_TRANSKRIBUS    = 1;
    public static final int SYNC_DRIVE          = 2;

    private String mUserName, mPassword, mFirstName, mLastName, mSessionID;
    private boolean mIsLoggedin, mIsAutoLogInDone, mTranskribusUploadCollIdSet;
    private int mConnection;
    private Collection mCollection;
    private String mDropboxToken;
    private String mDocumentName = null;
    private int mTranskribusUploadCollId;

    /**
     * Returns a singleton.
     * @return
     */
    public static User getInstance() {

        if (mInstance == null)
            mInstance = new User();

        return mInstance;

    }

    private User() {

        mIsLoggedin = false;
        mIsAutoLogInDone = false;
        mTranskribusUploadCollIdSet = false;
    }

    public void setCollection(Collection collection) {

        mCollection = collection;

    }

    public boolean isAutoLogInDone() { return mIsAutoLogInDone; }

    public void setAutoLogInDone(boolean isAutoLogInDone) { mIsAutoLogInDone = isAutoLogInDone; }

    public String getFirstName() { return mFirstName; }

    public void setFirstName(String firstName) { mFirstName = firstName; }

    public String getLastName() { return mLastName; }

    public void setLastName(String lastName) { mLastName = lastName; }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) { mUserName = userName; }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) { mPassword = password; }

    public boolean isLoggedIn() { return mIsLoggedin; }

    public void setLoggedIn(boolean loggedIn) {
        mIsLoggedin = loggedIn;
//        mIsAutoLogInDone = loggedIn;
    }

    public void setSessionID(String sessionID) {
        mSessionID = sessionID;
    }

    public String getSessionID() {
        return mSessionID;
    }

    public void setConnection(int connection) { mConnection = connection; }

    public int getConnection() { return mConnection; }

    public void setDropboxToken(String token) {
        mDropboxToken = token;
    }
    
    public String getDropboxToken() {
        return mDropboxToken;
    }

    public void setDocumentName(String documentName) { mDocumentName = documentName; }

    public String getDocumentName() { return mDocumentName; }

}
