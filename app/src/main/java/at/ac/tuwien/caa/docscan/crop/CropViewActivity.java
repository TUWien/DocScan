package at.ac.tuwien.caa.docscan.crop;

import android.os.Bundle;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.ui.BaseNoNavigationActivity;

/**
 * Created by fabian on 21.11.2017.
 */

public class CropViewActivity extends BaseNoNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_view);

        super.initToolbarTitle(R.string.crop_view_title);



    }

}
