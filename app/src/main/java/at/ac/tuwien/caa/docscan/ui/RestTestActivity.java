package at.ac.tuwien.caa.docscan.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.rest.Collection;
import at.ac.tuwien.caa.docscan.rest.CollectionsRequest;
import at.ac.tuwien.caa.docscan.rest.DocumentMetaData;
import at.ac.tuwien.caa.docscan.rest.DocumentRequest;
import at.ac.tuwien.caa.docscan.rest.DocumentsMetaDataRequest;
import at.ac.tuwien.caa.docscan.rest.RequestHandler;
import at.ac.tuwien.caa.docscan.rest.StartUploadRequest;
import at.ac.tuwien.caa.docscan.rest.User;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

//import com.android.volley.Response;

//import com.android.volley.Response;

//import com.android.volley.Response;


/**
 * Created by fabian on 22.06.2017.
 */

public class RestTestActivity extends BaseNavigationActivity implements CollectionsRequest.CollectionsCallback,
        DocumentsMetaDataRequest.DocumentsMetaDataCallback, StartUploadRequest.StartUploadCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO; remove
        SyncInfo.readFromDisk(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

        Button collectionsButton = (Button) findViewById(R.id.debug_collections_button);
        collectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestCollections();
            }
        });

        Button uploadButton = (Button) findViewById(R.id.debug_upload_button);
//        uploadButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                testRetroFit();
//            }
//        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestUpload();
            }
        });

    }

    private void requestCollections() {
        RequestHandler.createRequest(this, RequestHandler.REQUEST_COLLECTIONS);
    }

    private void requestUpload() {
//        RequestHandler.upload
//        new AddCollectionRequest(this);
        new StartUploadRequest(this);
    }



    @Override
    protected NavigationDrawer.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItemEnum.REST_TEST;
//        return NavigationDrawer.NavigationItemEnum.INVALID;
    }

    @Override
    public void onCollections(List<Collection> collections) {

        for (Collection collection : collections) {

            if (collection.getName().equals("test")) {
                if (User.getInstance() != null) {
                    User.getInstance().setCollection(collection);
                }
            }
        }

    }

    @Override
    public void onDocumentsMetaData(List<DocumentMetaData> documentMetaDatas) {

        for (DocumentMetaData documentMetaData : documentMetaDatas) {
            DocumentRequest request = new DocumentRequest(this, documentMetaData);

        }
    }

    @Override
    public void onUploadStart(int uploadId) {

//        startMultiPartUpload(uploadId);
        testRetroFit(uploadId);
//        testIon(uploadId);
    }

    private void testIon(int uploadId) {

        File file = SyncInfo.getInstance().getSyncList().get(SyncInfo.getInstance().getSyncList().size()-1).getFile();

        Ion.with(this.getApplicationContext())
                .load("https://transkribus.eu/TrpServerTesting/rest/uploads/" + Integer.toString(uploadId))
                .setHeader("Cookie", "JSESSIONID=" + User.getInstance().getSessionID())
                .setMultipartFile("img", "application/octet-stream", file)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                    }
                });
    }

    private void testRetroFit(int uploadId) {

        new UploadTask().execute(uploadId);

    }

    interface UploadTranskribus {


        @Multipart
        @POST("uploads/{uploadId}")
        Call<ResponseBody> upload(
                @Path("uploadId") String id,
                @Part("description") RequestBody description,
                @Part MultipartBody.Part file
        );


        OkHttpClient okHttpClient = new OkHttpClient.Builder()
//                .readTimeout(1000, TimeUnit.DAYS)
//                .writeTimeout(1000, TimeUnit.DAYS)
                .addInterceptor(
                        new Interceptor() {
                            @Override
                            public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {

                                Request original = chain.request();

                                // Request customization: add request headers
                                okhttp3.Request.Builder requestBuilder = original.newBuilder()
                                        .header("Cookie", "JSESSIONID=" + User.getInstance().getSessionID())
                                        .header("Content-Type", "application/json");


                                okhttp3.Request request = requestBuilder.build();
                                return chain.proceed(request);
                            }
                        })

                .addInterceptor(
                        new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
                )

                .build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://transkribus.eu/TrpServerTesting/rest/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

    }

    private class UploadTask extends AsyncTask<Integer, Void, Void> {


        @Override
        protected Void doInBackground(Integer... params) {


//            ==========================================================

            File file = SyncInfo.getInstance().getSyncList().get(SyncInfo.getInstance().getSyncList().size()-1).getFile();

            MediaType mediaType = MediaType.parse("application/octet-stream");

            String descriptionString = "descriptionString";
            RequestBody description = RequestBody.create(okhttp3.MultipartBody.FORM, descriptionString);

//            Version 1: Construct the body with a File:
            RequestBody requestFile = RequestBody.create(mediaType, file);
            MultipartBody.Part partV1 = MultipartBody.Part.createFormData("img", file.getAbsolutePath(), requestFile);

//            Version 2: Construct the body with a FileInputStream:
            InputStream in = null;
            RequestBody requestBody = null;
            MultipartBody.Part partV2 = null;
            try {
                in = new FileInputStream(new File(file.getAbsolutePath()));
                byte[] buf;
                buf = new byte[in.available()];
                while (in.read(buf) != -1);
                requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), buf);
                partV2 = MultipartBody.Part.createFormData("img", file.getAbsolutePath(), requestBody);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            UploadTranskribus uploadService = UploadTranskribus.retrofit.create(UploadTranskribus.class);

            Call<ResponseBody> call = uploadService.upload(Integer.toString(params[0]), description, partV1);
//            Call<ResponseBody> call = uploadService.upload(Integer.toString(params[0]), description, partV2);

            call.enqueue(new Callback<ResponseBody>() {


                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {

                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("Upload error:", t.getMessage());
                }
            });



            return null;

        }
    }

    private void buildPart(DataOutputStream dataOutputStream, byte[] fileData, String fileName) throws IOException {

        String twoHyphens = "--";
        String lineEnd = "\r\n";
        String boundary = "apiclient-" + System.currentTimeMillis();
//        String mimeType = "multipart/form-data;boundary=" + boundary;

        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: application/octet-stream; name=\"img\";" + lineEnd);
        dataOutputStream.writeBytes("Content-Type: application/octet-stream;" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
        int bytesAvailable = fileInputStream.available();

        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        // read file and write it into form...
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        dataOutputStream.writeBytes(lineEnd);
    }

    private byte[] getBytes(File file) {

        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return bytes;

    }

//    private void startMultiPartUpload(int uploadId) {
//
//        String url = "https://transkribus.eu/TrpServerTesting/rest/uploads/" + Integer.toString(uploadId);
//
//        byte[] multipartBody = null;
//
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        DataOutputStream dos = new DataOutputStream(bos);
//
//
//        File file1 = SyncInfo.getInstance().getSyncList().get(0).getFile();
//        byte[] fileData1 = getBytes(file1);
//        File file2 = SyncInfo.getInstance().getSyncList().get(0).getFile();
//        byte[] fileData2 = getBytes(file2);
//
//        String twoHyphens = "--";
//        String lineEnd = "\r\n";
//        String boundary = "apiclient-" + System.currentTimeMillis();
////        String mimeType = "multipart/form-data;boundary=" + boundary;
//        String mimeType = "multipart/form-data;";
////        String mimeType = "application/jsonX;";
//
//        try {
//            // the first file
//            buildPart(dos, fileData1, "ic_action_android.png");
//            // the second file
//            buildPart(dos, fileData2, "ic_action_book.png");
//            // send multipart form data necesssary after file data
//            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
//            // pass to multipart body
//            multipartBody = bos.toByteArray();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        final Context context = this;
//
//        MultipartRequest multipartRequest = new MultipartRequest(url, null, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
//            @Override
//            public void onResponse(NetworkResponse response) {
//                Toast.makeText(context, "Upload successfully!", Toast.LENGTH_SHORT).show();
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(context, "Upload failed!\r\n" + error.toString(), Toast.LENGTH_SHORT).show();
//            }
//        })
//        {
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                HashMap<String, String> headers = new HashMap<String, String>();
//                headers.put("Cookie", "JSESSIONID=" + User.getInstance().getSessionID());
////                headers.put("Content-Type", "application/json; charset=utf-8");
//                return headers;
//            }
//        };
//
//        RequestHandler.processMultiPartRequest(this, multipartRequest);
//
//
//
//    }



    public class Coll {

        String name;
        String colName;

    }

    interface CollectionsTranskribus {

    @GET("collections/list")
    Call<List<Coll>> collections();

    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(
                    new Interceptor() {
                        @Override
                        public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {

                            Request original = chain.request();

                            // Request customization: add request headers
                            okhttp3.Request.Builder requestBuilder = original.newBuilder()
                                    .header("Cookie", "JSESSIONID=" + User.getInstance().getSessionID());


                            okhttp3.Request request = requestBuilder.build();
                            return chain.proceed(request);
                        }
                    })
            .build();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://transkribus.eu/TrpServerTesting/rest/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build();

    }




}
