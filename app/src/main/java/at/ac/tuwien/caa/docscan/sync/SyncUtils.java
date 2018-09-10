package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.rest.UserHandler;

import static at.ac.tuwien.caa.docscan.rest.User.SYNC_DRIVE;
import static at.ac.tuwien.caa.docscan.rest.User.SYNC_DROPBOX;
import static at.ac.tuwien.caa.docscan.rest.User.SYNC_TRANSKRIBUS;

/**
 * Created by fabian on 31.08.2017.
 */

public class SyncUtils {


    public static void login(Context context, LoginRequest.LoginCallback loginCallback) {

        if (User.getInstance().isLoggedIn())
            return;

        boolean isUserSaved = UserHandler.loadCredentials(context);
        if (isUserSaved) {

            if (User.getInstance().getConnection() == User.SYNC_TRANSKRIBUS) {
                RequestHandler.createRequest(context, RequestHandler.REQUEST_LOGIN);
                User.getInstance().setAutoLogInDone(true);
            }
            else if (User.getInstance().getConnection() == SYNC_DROPBOX) {
                DropboxUtils.getInstance().loginToDropbox(loginCallback, User.getInstance().getDropboxToken());
                User.getInstance().setAutoLogInDone(true);
            }
        }


    }

    public static String getConnectionText(Context context, int connection) {

        switch (connection) {
            case SYNC_DRIVE:
                return context.getResources().getString(R.string.sync_drive_text);
            case SYNC_TRANSKRIBUS:
                return context.getResources().getString(R.string.sync_transkribus_text);
            case SYNC_DROPBOX:
                return context.getResources().getString(R.string.sync_dropbox_text);
        }

        return null;

    }
}
