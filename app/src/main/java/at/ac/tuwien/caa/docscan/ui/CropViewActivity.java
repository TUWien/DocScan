package at.ac.tuwien.caa.docscan.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.MediaStoreSignature;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.crop.CropView;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Helper;

import static at.ac.tuwien.caa.docscan.crop.CropInfo.CROP_INFO_NAME;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropViewActivity extends BaseNoNavigationActivity {

    private CropView mCropView;
    private String mFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_view);

        super.initToolbarTitle(R.string.crop_view_title);

        CropInfo cropInfo = getIntent().getParcelableExtra(CROP_INFO_NAME);
        mCropView = findViewById(R.id.crop_view);
        initCropInfo(cropInfo);


        ImageButton button = findViewById(R.id.confirm_crop_view_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapView();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.crop_menu, menu);

//        mOptionsMenu = menu;
//
//        mFlashMenuItem = menu.findItem(R.id.flash_mode_item);
//        mDocumentMenuItem = menu.findItem(R.id.document_item);
//
//        // The flash menu item is not visible at the beginning ('weak' devices might have no flash)
//        if (mFlashModes != null)
//            mFlashMenuItem.setVisible(true);

        return true;

    }

    public void rotateCropView(MenuItem item) {

        View menuItemView = findViewById(R.id.document_item);
        if (menuItemView == null)
            return;

        try {
            if (Helper.rotateExif(new File(mFileName)))
                rotateCropView();
        }
        catch(IOException e) {

        }

    }

    private void startMapView() {

        ArrayList<PointF> cropPoints = mCropView.getCropPoints();

        Intent intent = new Intent(getApplicationContext(), MapViewActivity.class);
        CropInfo r = new CropInfo(cropPoints, mFileName);
        intent.putExtra(CROP_INFO_NAME, r);
        startActivity(intent);

    }

    private void initCropInfo(CropInfo cropInfo) {



//        Load image with Glide:
        mFileName = cropInfo.getFileName();
        File file = new File(mFileName);

        try {
            final ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = Integer.valueOf(exif.getAttribute(ExifInterface.TAG_ORIENTATION));

            GlideApp.with(this)
                    .load(file)
                    .signature(new MediaStoreSignature("", file != null ? file.lastModified() : 0L, orientation))
                    .listener(imgLoadListener)
                    .into(mCropView);

        } catch (IOException e) {
            GlideApp.with(this)
                    .load(file)
                    .listener(imgLoadListener)
                    .into(mCropView);
        }

    }

    private void rotateCropView() {

        mCropView.rotate90Degrees();
    }

    private RequestListener imgLoadListener = new RequestListener() {

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {

            Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
            Mat m = new Mat();
            Utils.bitmapToMat(bitmap, m);


            Mat mg = new Mat();
            Imgproc.cvtColor(m, mg, Imgproc.COLOR_RGBA2RGB);

//            TODO: put this into AsyncTask:
            DkPolyRect[] polyRects = NativeWrapper.getPageSegmentation(mg);

            if (polyRects.length > 0 && polyRects[0] != null) {
                ArrayList<PointF> cropPoints = normPoints(polyRects[0], bitmap.getWidth(), bitmap.getHeight());
                mCropView.setPoints(cropPoints);
            }
            else {
                mCropView.setDefaultPoints();
            }

            return false;
        }

    };

    private ArrayList<PointF> normPoints(DkPolyRect rect, int width, int height) {

        ArrayList<PointF> normedPoints = new ArrayList<>();

        for (PointF point : rect.getPoints()) {
            PointF normedPoint = new PointF();
            normedPoint.x = point.x / width;
            normedPoint.y = point.y / height;
            normedPoints.add(normedPoint);
        }

        return normedPoints;

    }



}
