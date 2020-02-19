package at.ac.tuwien.caa.docscan.ui.docviewer;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 3/1/2018.
 */

public class SelectableToolbar {

    private final Context mContext;
    private Toolbar mToolbar;
    private AppBarLayout.LayoutParams mAppBarLayoutParams;
    private CoordinatorLayout.LayoutParams mCoordinatorLayoutParams;
    private AppBarLayout mAppBarLayout;
    private Drawable mNavigationDrawable;
    private CharSequence mLastTitle;
    private boolean mIsSelectMode = false;
    private SelectableToolbarCallback mCallback;

    public SelectableToolbar(Context context, Toolbar toolbar, AppBarLayout appBarLayout) {

        mContext = context;

        mToolbar = toolbar;
        mLastTitle = mToolbar.getTitle();
        mNavigationDrawable = mToolbar.getNavigationIcon();
        mAppBarLayout = appBarLayout;
        mAppBarLayoutParams = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        mCoordinatorLayoutParams = (CoordinatorLayout.LayoutParams) mAppBarLayout.getLayoutParams();
        mCallback = (SelectableToolbarCallback) context;

    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public void setTitle(CharSequence title) {

        boolean lastSelectMode = mIsSelectMode;

        mIsSelectMode = false;
        mToolbar.setNavigationIcon(mNavigationDrawable);
        mToolbar.setTitle(title);
        mLastTitle = title;
        mToolbar.setBackgroundColor(mContext.getResources().getColor(R.color.white));

//        Tell the activity that the selection state has changed:
        if (lastSelectMode)
            mCallback.onSelectionActivated(false);

    }

    public boolean isSelectMode() {
        return mIsSelectMode;
    }

    public void update(int selectionCount) {

        boolean lastSelectMode = mIsSelectMode;

        mIsSelectMode = selectionCount > 0;

        if (mIsSelectMode)
            selectToolbar(selectionCount);
        else
            deselectToolbar();

        //        Tell the activity that the selection state has changed:
        if (lastSelectMode != mIsSelectMode)
            mCallback.onSelectionActivated(mIsSelectMode);
    }

    public void selectToolbar(int selectionCount) {

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


    public void resetToolbar() {

        boolean lastSelectMode = mIsSelectMode;

        deselectToolbar();

        //        Tell the activity that the selection state has changed:
        if (lastSelectMode != mIsSelectMode)
            mCallback.onSelectionActivated(mIsSelectMode);

    }

    private void deselectToolbar() {
        mIsSelectMode = false;

        // This is based on post of Denny Weinberg:
        // https://stackoverflow.com/questions/30771156/how-to-set-applayout-scrollflags-for-toolbar-programmatically/30771904
        mAppBarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        mCoordinatorLayoutParams.setBehavior(new AppBarLayout.Behavior());

        mToolbar.setBackgroundColor(mContext.getResources().getColor(R.color.white));
//            Set the default back button:
        mToolbar.setNavigationIcon(mNavigationDrawable);
        mToolbar.setTitle(mLastTitle);
        mAppBarLayout.setLayoutParams(mCoordinatorLayoutParams);
    }

    public interface SelectableToolbarCallback {

        void onSelectionActivated(boolean activated);
    }

}
