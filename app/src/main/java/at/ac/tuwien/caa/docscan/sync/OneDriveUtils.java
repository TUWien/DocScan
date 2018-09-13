package at.ac.tuwien.caa.docscan.sync;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.microsoft.graph.BuildConfig;
import com.microsoft.graph.authentication.IAuthenticationAdapter;
import com.microsoft.graph.authentication.MSAAuthAndroidAdapter;
import com.microsoft.graph.core.GraphErrorCodes;
import com.microsoft.graph.extensions.GraphServiceClient;
import com.microsoft.graph.extensions.IGraphServiceClient;
import com.microsoft.graph.extensions.User;
import com.onedrive.sdk.authentication.ADALAuthenticator;
import com.onedrive.sdk.authentication.MSAAuthenticator;
import com.onedrive.sdk.concurrency.ICallback;
import com.onedrive.sdk.core.ClientException;
import com.onedrive.sdk.core.DefaultClientConfig;
import com.onedrive.sdk.core.IClientConfig;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;
import com.onedrive.sdk.extensions.OneDriveClient;

public class OneDriveUtils {

    private static OneDriveUtils mInstance;

    public static OneDriveUtils getInstance() {

        if (mInstance == null)
            mInstance = new OneDriveUtils();

        return mInstance;

    }


    public void startGraphSDK(Application app, Activity activity) {

        final IAuthenticationAdapter authenticationAdapter = new MSAAuthAndroidAdapter(app) {
            @Override
            public String getClientId() {
                return at.ac.tuwien.caa.docscan.BuildConfig.OneDriveApiKey;
            }

            @Override
            public String[] getScopes() {
                return new String[] {
                        // An example set of scopes your application could use
                        "https://graph.microsoft.com/Calendars.ReadWrite",
                        "https://graph.microsoft.com/Contacts.ReadWrite",
                        "https://graph.microsoft.com/Files.ReadWrite",
                        "https://graph.microsoft.com/Mail.ReadWrite",
                        "https://graph.microsoft.com/Mail.Send",
                        "https://graph.microsoft.com/User.ReadBasic.All",
                        "https://graph.microsoft.com/User.ReadWrite",
                        "offline_access",
                        "openid"
                };
            }
        };

        com.microsoft.graph.concurrency.ICallback<Void> callback = new
                com.microsoft.graph.concurrency.ICallback<Void>() {


            @Override
            public void success(Void aVoid) {

                // Use the authentication provider previously defined within the project and create a configuration instance
                final com.microsoft.graph.core.IClientConfig config = com.microsoft.graph.core.
                        DefaultClientConfig.createWithAuthenticationProvider(authenticationAdapter);


// Create the service client from the configuration
                final IGraphServiceClient client = new GraphServiceClient.Builder()
                        .fromConfig(config)
                        .buildClient();

                final com.microsoft.graph.concurrency.ICallback<User> clientCallback =
                        new com.microsoft.graph.concurrency.ICallback<User>() {
                            @Override
                            public void success(final User user) {
                                //Handle successful logout
                            }

                            @Override
                            public void failure(com.microsoft.graph.core.ClientException ex) {

                            }


                        };

                client.getMe()
                        .buildRequest()
                        .get(clientCallback);

            }

            @Override
            public void failure(com.microsoft.graph.core.ClientException ex) {

            }

        };

        authenticationAdapter.login(activity, callback);





    }




    public void startClient(Activity activity) {

        final MSAAuthenticator msaAuthenticator = new MSAAuthenticator() {
            @Override
            public String getClientId() {
                return "";
            }

            @Override
            public String[] getScopes() {
                return new String[]{"onedrive.appfolder"};
            }
        };


        final ADALAuthenticator adalAuthenticator = new ADALAuthenticator() {
            @Override
            public String getClientId() {
                return "<adal-client-id>";
            }

            @Override
            protected String getRedirectUrl() {
                return "https://localhost";
            }
        };

        final IClientConfig oneDriveConfig = DefaultClientConfig.createWithAuthenticators(
                msaAuthenticator,
                adalAuthenticator);


//        final DefaultCallback<IOneDriveClient> callback = new DefaultCallback<IOneDriveClient>(activity) {
//            @Override
//            public void success(final IOneDriveClient result) {
//                // OneDrive client created successfully.
//            }
//
//            @Override
//            public void failure(final ClientException error) {
//                // Exception happened during creation.
//            }
//        };

        ICallback<IOneDriveClient> callback = new ICallback<IOneDriveClient>() {
            @Override
            public void success(IOneDriveClient iOneDriveClient) {

            }

            @Override
            public void failure(ClientException ex) {

            }
        };

        new OneDriveClient.Builder()
                .fromConfig(oneDriveConfig)
                .loginAndBuildClient(activity, callback);

//        final IOneDriveClient oneDriveClient = new OneDriveClient.Builder()
//                .fromConfig(oneDriveConfig)
//
//                .get(new ICallback<Item>() {
//                    @Override
//                    public void success(final Item result) {
//                        final String msg = "Found Root " + result.id;
//                        Toast.makeText((Activity) context, msg, Toast.LENGTH_SHORT)
//                                .show();
//                    }
//
//                    @Override
//                    public void failure(ClientException ex) {
//
//                    }
//
//                    int b = 0;
//
//
//    }

    }
}
