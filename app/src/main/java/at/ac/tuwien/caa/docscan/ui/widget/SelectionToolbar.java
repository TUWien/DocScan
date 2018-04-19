package at.ac.tuwien.caa.docscan.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

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
        mToolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_clear_material);
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
