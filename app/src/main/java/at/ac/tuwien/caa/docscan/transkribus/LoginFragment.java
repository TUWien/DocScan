package at.ac.tuwien.caa.docscan.transkribus;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 24.11.2016.
 */
public class LoginFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View loginView = inflater.inflate(R.layout.login_view, container, false);

        return loginView;

    }

}
