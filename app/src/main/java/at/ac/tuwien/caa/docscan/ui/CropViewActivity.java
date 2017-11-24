package at.ac.tuwien.caa.docscan.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;

import java.io.IOException;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.crop.CropView;
import at.ac.tuwien.caa.docscan.logic.Helper;

import static at.ac.tuwien.caa.docscan.crop.CropInfo.CROP_INFO_NAME;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropViewActivity extends BaseNoNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_view);

        super.initToolbarTitle(R.string.crop_view_title);

        CropInfo cropInfo = getIntent().getParcelableExtra(CROP_INFO_NAME);

        // Unfortunately the exif orientation is not used by BitmapFactory:
        try {

            Bitmap bitmap = BitmapFactory.decodeFile(cropInfo.getFileName());
            CropView cropView = (CropView) findViewById(R.id.crop_view);

            ExifInterface exif = new ExifInterface(cropInfo.getFileName());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            int angle = Helper.getAngleFromExif(orientation);
            if (angle != -1) {
                //Rotate the image:
                Matrix mtx = new Matrix();
                mtx.setRotate(angle);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);
            }

            cropView.setBitmapAndPoints(bitmap, cropInfo.getPoints());

        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    private void testAffineTransform() {

//        MatOfPoint2f src = new MatOfPoint2f(new Point(2, 3), new Point(3, 1), new Point(1, 4));
//        MatOfPoint2f dst = new MatOfPoint2f(new Point(3, 3), new Point(7, 4), new Point(5, 6));
//
//        Mat transform = Imgproc.getAffineTransform(src, dst);
//
//        Imgproc.warpAffine();
//
//        warpAffine( src, warp_dst, warp_mat, warp_dst.size() );


    }

}
