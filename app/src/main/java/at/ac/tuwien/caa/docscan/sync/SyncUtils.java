package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;

import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

/**
 * Created by fabian on 31.08.2017.
 */

public class SyncUtils {


    public static void login(Context context, LoginRequest.LoginCallback loginCallback) {

        if (User.getInstance().isLoggedIn())
            return;

        boolean isUserSaved = UserHandler.loadCredentials(context);
        if (isUserSaved && !User.getInstance().isAutoLogInDone()) {

            if (User.getInstance().getConnection() == User.SYNC_TRANSKRIBUS) {
                RequestHandler.createRequest(context, RequestHandler.REQUEST_LOGIN);
                User.getInstance().setAutoLogInDone(true);
            }
            else if (User.getInstance().getConnection() == User.SYNC_DROPBOX) {
                if (UserHandler.loadDropboxToken(context)) {
                    DropboxUtils.getInstance().loginToDropbox(loginCallback, User.getInstance().getDropboxToken());
                    User.getInstance().setAutoLogInDone(true);
                }
            }
        }


    }
}
