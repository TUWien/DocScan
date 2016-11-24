package at.ac.tuwien.caa.docscan.transkribus;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 24.11.2016.
 */
public class TranskribusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.transkribus_container_view);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        LoginFragment fragment = new LoginFragment();
        fragmentTransaction.add(R.id.transkribus_container_layout, fragment);
        fragmentTransaction.commit();

    }


}
