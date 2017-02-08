package at.ac.tuwien.caa.docscan.rest;

/**
 * Created by fabian on 01.12.2016.
 */
public class User {

    private static User mInstance = null;

    private String mUserName;
    private String mPassword;
    private boolean mIsLoggedin;
    private String mSessionID;

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

        mUserName = "llu53232@noicd.com";
        mPassword = "testuserpwd";
    }

    public String getUserName() {
        return mUserName;
    }

    public void setUserName(String userName) { mUserName = userName; }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) { mPassword = password; }

    public void setLoggedIn(boolean loggedIn) {
        mIsLoggedin = loggedIn;
    }

    public void setSessionID(String sessionID) {
        mSessionID = sessionID;
    }

    public String getSessionID() {
        return mSessionID;
    }
}
