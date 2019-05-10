package at.ac.tuwien.caa.docscan.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 3/1/2018.
 */

public class SelectionToolbar {

    private final Context mContext;
    private Toolbar mToolbar;
    private AppBarLayout.LayoutParams mAppBarLayoutParams;
    private CoordinatorLayout.LayoutParams mCoordinatorLayoutParams;
    private AppBarLayout mAppBarLayout;
    private Drawable mNavigationDrawable;

    public SelectionToolbar(Context context, Toolbar toolbar, AppBarLayout appBarLayout) {

        mContext = context;

        mToolbar = toolbar;
        mNavigationDrawable = mToolbar.getNavigationIcon();
        mAppBarLayout = appBarLayout;
        mAppBarLayoutParams = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        mCoordinatorLayoutParams = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();

    }


    public void scrollToolbar(int selectionCount) {

        // This is based on post of Denny Weinberg:
        // https://stackoverflow.com/questions/30771156/how-to-set-applayout-scrollflags-for-toolbar-programmatically/30771904
        mAppBarLayoutParams.setScrollFlags(0);
        mCoordinatorLayoutParams.setBehavior(null);

//            Set the action bar title:
        mToolbar.setTitle(Integer.toString(selectionCount) + " "
                + mContext.getResources().getString(R.string.gallery_selected));
//            Set custom home button:
        mToolbar.setNavigationIcon(R.drawable.ic_clear_black_24dp);
        mToolbar.setBackgroundColor(mContext.getResources().getColor(R.color.colorAccent));

        mAppBarLayout.setLayoutParams(mCoordinatorLayoutParams);

    }


    public void fixToolbar() {

        // This is based on post of Denny Weinberg:
        // https://stackoverflow.com/questions/30771156/how-to-set-applayout-scrollflags-for-toolbar-programmatically/30771904
        mAppBarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        mCoordinatorLayoutParams.setBehavior(new AppBarLayout.Behavior());

        mToolbar.setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimary));
//            Set the default back button:
        mToolbar.setNavigationIcon(mNavigationDrawable);

        mAppBarLayout.setLayoutParams(mCoordinatorLayoutParams);

    }

}
