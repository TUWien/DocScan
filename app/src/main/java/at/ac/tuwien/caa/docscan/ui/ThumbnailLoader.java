package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;

import java.io.IOException;

/**
 * Class responsible for loading thumbnails from images. This is time intense and hence it is
 * done in an own thread (AsyncTask).
 */
public class ThumbnailLoader extends AsyncTask<String, Void, BitmapDrawable> {

    private Context mContext;
    private ThumbnailCallback mCallback;

    public interface ThumbnailCallback {
        void thumbnailLoaded(BitmapDrawable drawable);
    }

    public ThumbnailLoader(Context context, ThumbnailCallback callback) {

        mContext = context;
        mCallback = callback;

    }

    @Override
    protected BitmapDrawable doInBackground(String... fileNames) {

        String fileName = fileNames[0];

        Bitmap thumbNailBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(fileName), 200, 200);
        if (thumbNailBitmap == null)
            return null;

        // Determine the rotation angle of the image:
        int angle = -1;
        try {
            ExifInterface exif = new ExifInterface(fileName);
            String attr = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            angle = getAngleFromExif(Integer.valueOf(attr));
        } catch (IOException e) {
            return null;
        }

        //Rotate the image:
        Matrix mtx = new Matrix();
        mtx.setRotate(angle);
        thumbNailBitmap = Bitmap.createBitmap(thumbNailBitmap, 0, 0, thumbNailBitmap.getWidth(), thumbNailBitmap.getHeight(), mtx, true);

        // Update the gallery button:
        final BitmapDrawable thumbDrawable = new BitmapDrawable(mContext.getResources(), thumbNailBitmap);
        if (thumbDrawable == null)
            return null;



        return thumbDrawable;

    }

    @Override
    protected void onPostExecute(BitmapDrawable result) {

        mCallback.thumbnailLoaded(result);

    }

    private int getAngleFromExif(int orientation) {

        switch (orientation) {

            case 1:
                return 0;
            case 6:
                return 90;
            case 3:
                return 180;
            case 8:
                return 270;

        }

        return -1;

    }

}