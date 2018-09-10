package at.ac.tuwien.caa.docscan.sync;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;

import at.ac.tuwien.caa.docscan.rest.LoginRequest;
import at.ac.tuwien.caa.docscan.rest.User;

public class DriveUtils {

    private static DriveUtils mInstance;

    private GoogleSignInClient mSignInClient;
    private DriveClient mClient;
    private LoginRequest.LoginCallback mCallback;

    public static DriveUtils getInstance() {

        if (mInstance == null)
            mInstance = new DriveUtils();

        return mInstance;

    }

    private DriveUtils() {

    }

    public void authenticate(Context context) {

        //        TODO: check how client looks in error cases
        mSignInClient = buildGoogleSignInClient(context);

//        mSignInClient.getSignInIntent()

    }

    public GoogleSignInClient getSignInClient() {

        return mSignInClient;

    }

    public void initClient(Context context) {

        if (context != null) {

            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);

            mClient = Drive.getDriveClient(context, account);

            User.getInstance().setLoggedIn(true);
            User.getInstance().setFirstName(account.getGivenName());
            User.getInstance().setLastName(account.getFamilyName());
            User.getInstance().setConnection(User.SYNC_DRIVE);

            ((LoginRequest.LoginCallback) context).onLogin(User.getInstance());
        }

    }

    private GoogleSignInClient buildGoogleSignInClient(Context context) {

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .requestIdToken("505934464640-7panfppavaitfope0vn7987bpr75rsbb.apps.googleusercontent.com")
                        .requestEmail()
                        .build();

        return GoogleSignIn.getClient(context, signInOptions);
    }




}
