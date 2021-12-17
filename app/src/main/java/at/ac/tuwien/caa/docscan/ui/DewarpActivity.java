package at.ac.tuwien.caa.docscan.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.NativeWrapper;

public class DewarpActivity extends BaseNoNavigationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

//        NativeWrapper.dummyWarp();

    }


}
