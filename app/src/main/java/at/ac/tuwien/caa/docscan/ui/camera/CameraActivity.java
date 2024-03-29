package at.ac.tuwien.caa.docscan.ui.camera;

import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.FLIP_SHOT_TIME;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.PAGE_SEGMENTATION;
import static at.ac.tuwien.caa.docscan.camera.TaskTimer.TaskType.SHOT_TIME;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.load.engine.GlideException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.zxing.Result;

import org.koin.android.compat.ViewModelCompat;
import org.koin.java.KoinJavaComponent;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.ActionSheet;
import at.ac.tuwien.caa.docscan.camera.CameraPaintLayout;
import at.ac.tuwien.caa.docscan.camera.CameraPreview;
import at.ac.tuwien.caa.docscan.camera.DebugViewFragment;
import at.ac.tuwien.caa.docscan.camera.LocationHandler;
import at.ac.tuwien.caa.docscan.camera.PaintView;
import at.ac.tuwien.caa.docscan.camera.SheetAction;
import at.ac.tuwien.caa.docscan.camera.TaskTimer;
import at.ac.tuwien.caa.docscan.camera.TextOrientationActionSheet;
import at.ac.tuwien.caa.docscan.camera.cv.CVResult;
import at.ac.tuwien.caa.docscan.camera.cv.DkPolyRect;
import at.ac.tuwien.caa.docscan.camera.cv.NativeWrapper;
import at.ac.tuwien.caa.docscan.camera.cv.Patch;
import at.ac.tuwien.caa.docscan.camera.cv.thread.preview.IPManager;
import at.ac.tuwien.caa.docscan.db.model.Page;
import at.ac.tuwien.caa.docscan.extensions.ContextExtensionsKt;
import at.ac.tuwien.caa.docscan.extensions.IntentExtensionsKt;
import at.ac.tuwien.caa.docscan.logic.DocumentViewerLaunchViewType;
import at.ac.tuwien.caa.docscan.logic.Failure;
import at.ac.tuwien.caa.docscan.logic.FileHandler;
import at.ac.tuwien.caa.docscan.logic.GlideHelper;
import at.ac.tuwien.caa.docscan.logic.GlideLegacyCallback;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.PermissionHandler;
import at.ac.tuwien.caa.docscan.logic.PreferencesHandler;
import at.ac.tuwien.caa.docscan.logic.Resource;
import at.ac.tuwien.caa.docscan.logic.Success;
import at.ac.tuwien.caa.docscan.ui.base.BaseNavigationActivity;
import at.ac.tuwien.caa.docscan.ui.base.NavigationDrawer;
import at.ac.tuwien.caa.docscan.ui.dialog.ADialog;
import at.ac.tuwien.caa.docscan.ui.dialog.DialogButton;
import at.ac.tuwien.caa.docscan.ui.dialog.DialogResult;
import at.ac.tuwien.caa.docscan.ui.dialog.DialogViewModel;
import at.ac.tuwien.caa.docscan.ui.document.CreateDocumentActivity;
import at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity;
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity;
import kotlin.Lazy;
import kotlin.Pair;
import me.drakeet.support.toast.ToastCompat;
import timber.log.Timber;

/*********************************************************************************
 *  DocScan is a Android app for document scanning.
 *
 *  Author:         Fabian Hollaus, Florian Kleber, Markus Diem
 *  Organization:   TU Wien, Computer Vision Lab
 *  Date created:   21. July 2016
 *
 *  This file is part of DocScan.
 *
 *  DocScan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  DocScan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with DocScan.  If not, see <http://www.gnu.org/licenses/>.
 *********************************************************************************/

/**
 * The main class of the app. It is responsible for creating the other views and handling
 * callbacks from the created views as well as user input.
 */
public class CameraActivity extends BaseNavigationActivity implements TaskTimer.TimerCallbacks,
        CameraPreview.CVCallback, CameraPreview.CameraPreviewCallback, CVResult.CVResultCallback {

    private static final String DEBUG_VIEW_FRAGMENT = "DebugViewFragment";
    private static final int PERMISSION_CAMERA = 1;
    private static final int PERMISSION_REQUEST_CODE_LOCATION = 2;
    private static final int CREATE_DOCUMENT_FROM_QR_REQUEST = 0;

    private static final String EXTRA_DOC_ID = "EXTRA_DOC_ID";
    private static final String EXTRA_PAGE_ID = "EXTRA_PAGE_ID";

    public static final int IMG_ORIENTATION_0 = 0;
    public static final int IMG_ORIENTATION_90 = 1;
    public static final int IMG_ORIENTATION_180 = 2;
    public static final int IMG_ORIENTATION_270 = 3;

    private final Lazy<CameraViewModel> viewModel = ViewModelCompat.viewModel(this, CameraViewModel.class);
    private final Lazy<DialogViewModel> dialogViewModel = ViewModelCompat.viewModel(this, DialogViewModel.class);
    private final Lazy<PreferencesHandler> preferencesHandler = KoinJavaComponent.inject(PreferencesHandler.class);
    private final Lazy<FileHandler> fileHandler = KoinJavaComponent.inject(FileHandler.class);

    @SuppressWarnings("deprecation")
    private Camera.PictureCallback mPictureCallback;
    private ImageButton mGalleryButton;
    //    removing mForceShootButton:
    private TaskTimer mTaskTimer;
    private CameraPreview mCameraPreview;
    private PaintView mPaintView;
    private TextView mCounterView;
    private CVResult mCVResult;
    // Debugging variables:
    private DebugViewFragment mDebugViewFragment;
    private boolean mIsDebugViewEnabled;
    private int mCameraOrientation;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsPictureSafe;
    private TextView mTextView;
    private boolean mIsSeriesMode = false;
    private boolean mIsSeriesModePaused = true;
    // We hold here a reference to the popupmenu and the list, because we are not sure what is first initialized:
    private List<String> mFlashModes;
    private PopupMenu mFlashPopupMenu, mWhiteBalancePopupMenu;
    //    private Drawable mGalleryButtonDrawable;
    private ProgressBar mProgressBar;
    private TaskTimer.TimerCallbacks mTimerCallbacks;
    private DkPolyRect[] mLastDkPolyRects;
    private int mLastTabPosition;
    private int mTextOrientation = IMG_ORIENTATION_0;
    private boolean mLockExposureSupported;
    private boolean mRestoreTab = false;
    private boolean mIsQRActive = false;

    public static Intent newInstance(Context context, boolean newTask) {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        return intent;
    }

    public static Intent newInstance(Context context, UUID docId, UUID pageId) {
        Intent intent = new Intent(context, CameraActivity.class);
        intent.putExtra(EXTRA_DOC_ID, docId);
        intent.putExtra(EXTRA_PAGE_ID, pageId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /*
     * Static initialization of the OpenCV and docscan-native modules.
     */
    static {

        Timber.d("initializing OpenCV");

//         We need this for Android 4:
        if (!OpenCVLoader.initDebug()) {
            Timber.d("Error while initializing OpenCV.");
        } else {

            System.loadLibrary("opencv_java3");
            System.loadLibrary("docscan-native");

            Timber.d("OpenCV initialized");
        }

    }

    private long mLastTime;
    private boolean mIsFocusMeasured;

    @Override
    protected boolean keepScreenOn() {
        return true;
    }

    /**
     * Creates the camera Activity.
     *
     * @param savedInstanceState saved instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        // TODO: Deprecated flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        observe();
        initActivity();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * Observes changes to the viewModel's livedata.
     */
    private void observe() {
        viewModel.getValue().getObservableDocumentWithPages().observe(this, documentWithPages -> {
            String title;
            ActionBar bar = getSupportActionBar();
            if (documentWithPages != null) {
                title = documentWithPages.getDocument().getTitle();
            } else {
                title = "";
            }
            if (bar != null) {
                bar.setTitle(title);
            }
        });
        viewModel.getValue().getObservableThumbnail().observe(this, page -> loadThumbNail(page));
        viewModel.getValue().getObservableTookImage().observe(this, resourceEvent -> {
            Resource<Page> resource = resourceEvent.getContentIfNotHandled();
            if (resource != null) {
                if (resource instanceof Failure) {
                    mIsPictureSafe = false;
                    showSaveErrorDialog(((Failure<Page>) resource).getException());
                } else if (resource instanceof Success) {
                    mIsPictureSafe = true;

                }
            }
        });
        viewModel.getValue().getObservableImageLoadingProgress().observe(this, isLoading -> {
            // the loading state will be managed by the glide loading functions
            // this is done to prevent a blinking of the old image.
            if (isLoading) {
                mGalleryButton.setVisibility(View.INVISIBLE);
            }
            mProgressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        });
        viewModel.getValue().getObservableOpenGallery().observe(this, event -> {
            Pair<UUID, UUID> pair = event.getContentIfNotHandled();
            if (pair != null) {
                navigateToGallery(pair.getFirst(), pair.getSecond());
            }
        });
        dialogViewModel.getValue().getObservableDialogAction().observe(this, event -> {
            DialogResult dialogResult = event.getContentIfNotHandled();
            if (dialogResult != null) {
                if (dialogResult.getDialogAction() == ADialog.DialogAction.RATIONALE_CAMERA_PERMISSION) {
                    if (dialogResult.getPressedAction() == DialogButton.POSITIVE) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                    }
                } else if (dialogResult.getDialogAction() == ADialog.DialogAction.RATIONALE_LOCATION_PERMISSION) {
                    if (dialogResult.getPressedAction() == DialogButton.POSITIVE) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE_LOCATION);
                    } else if (dialogResult.getPressedAction() == DialogButton.DISMISS || dialogResult.getPressedAction() == DialogButton.NEGATIVE) {
                        preferencesHandler.getValue().setGeoTaggingEnabled(false);
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!PermissionHandler.INSTANCE.isCameraPermissionGiven(getBaseContext())) {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            showDialog(ADialog.DialogAction.RATIONALE_CAMERA_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        }
    }

    private void enablePreview(boolean isEnabled) {
        findViewById(R.id.camera_permission_container).setVisibility(isEnabled ? View.GONE : View.VISIBLE);
        if (isEnabled) {
            // Resume camera access:
            mCameraPreview.resume();
            mPaintView.resume();
        } else {
            mCameraPreview.stop();
            mPaintView.pause();
        }
    }

    /**
     * Stops the camera and the paint view thread.
     */
    @Override
    public void onPause() {
        super.onPause();
        mPaintView.pause();
        mCameraPreview.pause();
        preferencesHandler.getValue().setSeriesModeActive(mIsSeriesMode);
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onStop() {
        super.onStop();
        mCameraPreview.stop();
    }


    /**
     * Called after the Activity resumes. Resumes the camera and the the paint view thread.
     */
    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume");

        // start fetching requested doc or the active doc as default and fallback
        UUID documentId = null;
        if (getIntent().getSerializableExtra(EXTRA_DOC_ID) != null) {
            documentId = (UUID) getIntent().getSerializableExtra(EXTRA_DOC_ID);
            getIntent().removeExtra(EXTRA_DOC_ID);
        }
        UUID pageId = null;
        if (getIntent().getSerializableExtra(EXTRA_PAGE_ID) != null) {
            pageId = (UUID) getIntent().getSerializableExtra(EXTRA_PAGE_ID);
            getIntent().removeExtra(EXTRA_PAGE_ID);
        }
        viewModel.getValue().load(documentId, pageId);

        mIsPictureSafe = true;
        boolean hasCameraPermission = PermissionHandler.INSTANCE.isCameraPermissionGiven(getBaseContext());
        enablePreview(hasCameraPermission);
        if (hasCameraPermission) {
            requestLocation();
        }

        // Read user settings:

        mPaintView.drawFocusText(preferencesHandler.getValue().getShowFocusValues());
        mPaintView.drawGrid(preferencesHandler.getValue().getShowGrid());
        NativeWrapper.setUseLab(!preferencesHandler.getValue().isFastSegmentation());

        mIsFocusMeasured = preferencesHandler.getValue().isFocusMeasure();
        mCVResult.setMeasureFocus(mIsFocusMeasured);
        IPManager.getInstance().setIsFocusMeasured(mIsFocusMeasured);

        mTextOrientation = preferencesHandler.getValue().getTextOrientation();
        mPaintView.setTextOrientation(mTextOrientation);

        showDebugView(preferencesHandler.getValue().getShowDebugView());

        showControlsLayout(!mIsQRActive);

        ContextExtensionsKt.checksGoogleAPIAvailability(this, true);

        LinearLayout lockExposureTextView = findViewById(R.id.lock_exposure_text_view);
        if (lockExposureTextView != null)
            lockExposureTextView.setVisibility(View.INVISIBLE);

    }

    private void navigateToGallery(@NonNull UUID docId, @Nullable UUID fileId) {
        startActivity(PageSlideActivity.newInstance(this, docId, fileId));
    }

    private void showControlsLayout(boolean showControls) {

        Timber.d("showControlsLayout");

        int controlsVisibility, qrCodeVisibility;

        if (showControls) {
            controlsVisibility = View.VISIBLE;
            qrCodeVisibility = View.INVISIBLE;

        } else {
            controlsVisibility = View.INVISIBLE;
            qrCodeVisibility = View.VISIBLE;
        }

        // Show/hide the menu buttons:
//        Weak devices might have no flash, so check if mFlashModes is null:
//        if ((mFlashModes != null) && (mFlashMenuItem != null))
//            mFlashMenuItem.setVisible(showControls);
//        if (mDocumentMenuItem != null)
//            mDocumentMenuItem.setVisible(showControls);
//        if (mGalleryMenuItem != null)
//            mGalleryMenuItem.setVisible(showControls);
//        if (mUploadMenuItem != null)
//            mUploadMenuItem.setVisible(showControls);

//        Show/hide the gallery button:


//        Deleted the overflow button, so I had to comment this out:
//        // Show/hide the overflow button:
//        if (mOptionsMenu != null)
//            mOptionsMenu.setGroupVisible(R.id.overflow_menu_group, showControls);

        RelativeLayout l = findViewById(R.id.controls_layout);
        TabLayout t = findViewById(R.id.camera_tablayout);
        if (l != null) {
            l.setVisibility(controlsVisibility);
            t.setVisibility(controlsVisibility);
        }

        if (mIsSeriesMode)
            mLastTabPosition = 1;
        else
            mLastTabPosition = 0;
        if (t.getSelectedTabPosition() != mLastTabPosition)
            restoreLastTab();


        RelativeLayout qrLayout = findViewById(R.id.qr_controls_layout);
        if (qrLayout != null)
            qrLayout.setVisibility(qrCodeVisibility);

    }

    // ================= end: methods from the Activity lifecyle =================

    /**
     * Initializes the activity.
     */

    private void initActivity() {

        setContentView(R.layout.activity_camera);

        MaterialButton cameraPermission = findViewById(R.id.btn_grant_camera_permission);
        cameraPermission.setOnClickListener(v -> requestCameraPermission());
        MaterialButton appSettings = findViewById(R.id.btn_request_app_settings);
        appSettings.setOnClickListener(v -> {
            IntentExtensionsKt.showAppSettings(this);
        });

        loadPreferences();

        mCVResult = new CVResult(this);
        IPManager.getInstance().setCVResult(mCVResult);

        mCVResult.setSeriesMode(mIsSeriesMode);

        mCameraPreview = findViewById(R.id.camera_view);

        mPaintView = findViewById(R.id.paint_view);
        if (mPaintView != null)
            mPaintView.setCVResult(mCVResult);

        mCounterView = findViewById(R.id.counter_view);

        setupToolbar();
        setupNavigationDrawer();
        setupDebugView();

//        initOrientationListener();

        // This is used to measure execution time of time intense tasks:
        mTaskTimer = new TaskTimer();

        mTextView = findViewById(R.id.instruction_view);
        if (mIsSeriesMode && mIsSeriesModePaused)
            setInstructionText(getString(R.string.instruction_series_paused));

//        initCameraControlLayout();

//        initDrawables();
        initPictureCallback();
        initButtons();

//        TODO: this is not that beautiful...
        final TabLayout t = findViewById(R.id.camera_tablayout);

        t.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

//                The tab is restored programmatically - not by the user, in this case do nothing:
                if (mRestoreTab) {
                    mRestoreTab = false;
                    return;
                }
//                TODO: replace with static int
//                We have to remember the last tab selection, because when setting is selected, we
//                have to restore the last selection:
                if (tab.getPosition() == 0) {
                    startAutomaticMode(false);
                    mLastTabPosition = 0;
                } else if (tab.getPosition() == 1) {
                    startAutomaticMode(true);
                    mLastTabPosition = 1;
                } else if (tab.getPosition() == 2) {
                    openSettingsMenu();
//                    TabLayout.Tab restoredTab = t.getTabAt(mLastTabPosition);
//                    t.setScrollPosition(mLastTabPosition, 0f, true);
//                    restoredTab.select();
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        FloatingActionButton fab = findViewById(R.id.document_fab);
        fab.setOnClickListener(view -> startActivity(DocumentViewerActivity.Companion.newInstance(CameraActivity.this, DocumentViewerLaunchViewType.DOCUMENTS)));

    }

    private void openRotateTextDirMenu() {

        ArrayList<SheetAction> actions = getRotateTextDirSheetActions();

        ActionSheet.SheetSelection s = action -> {

            switch (action.getId()) {
                case R.id.action_rotate_text_dir_left:
                    mTextOrientation--;
                    break;
                case R.id.action_rotate_text_dir_right:
                    mTextOrientation++;
                    break;
            }
//                Modulo of negative number is still negative, so make it positive:
            if (mTextOrientation < 0)
                mTextOrientation += 4;
            mTextOrientation = mTextOrientation % 4;

            saveTextOrientation();

            mPaintView.setTextOrientation(mTextOrientation);
        };

        ActionSheet.DialogStatus f = new ActionSheet.DialogStatus() {
            @Override
            public void onShown() {
                View v = findViewById(R.id.controls);
                v.setVisibility(View.INVISIBLE);
                mPaintView.drawTextOrientationLarge(true);
            }

            @Override
            public void onDismiss() {
                View v = findViewById(R.id.controls);
                v.setVisibility(View.VISIBLE);
                mPaintView.drawTextOrientationLarge(false);
            }
        };

        TextOrientationActionSheet.DialogConfirm c = new TextOrientationActionSheet.DialogConfirm() {

            int lastOrientation = mTextOrientation;

            @Override
            public void onOk() {

            }

            @Override
            public void onCancel() {
                Timber.d("onCancel");
                mTextOrientation = lastOrientation;
                mPaintView.setTextOrientation(lastOrientation);
                saveTextOrientation();
            }
        };

        boolean opaqueBackground = mCameraPreview.isPreviewFitting();
        getSupportFragmentManager().beginTransaction().add(new TextOrientationActionSheet(actions, s, f, c, opaqueBackground),
                "TAG").commit();

    }

    private void saveTextOrientation() {
        preferencesHandler.getValue().setTextOrientation(mTextOrientation);
    }

    @NonNull
    private ArrayList<SheetAction> getRotateTextDirSheetActions() {
        ArrayList<SheetAction> actions = new ArrayList<>();
        actions.add(new SheetAction(
                R.id.action_rotate_text_dir_left,
                getString(R.string.action_rotate_left_title),
                R.drawable.ic_rotate_left_white_24dp));
        actions.add(new SheetAction(
                R.id.action_rotate_text_dir_right,
                getString(R.string.action_rotate_right_title),
                R.drawable.ic_rotate_right_white_24dp));
        return actions;
    }

    private void openSettingsMenu() {

        ArrayList<SheetAction> actions = getSettingsSheetActions();

        ActionSheet.SheetSelection s = action -> {

            switch (action.getId()) {
                case R.id.action_show_grid_item:
                    showGrid(true);
                    break;
                case R.id.action_hide_grid_item:
                    showGrid(false);
                    break;
                case R.id.action_lock_wb_item:
                    lockExposure();
                    break;
                case R.id.action_unlock_wb_item:
                    unlockExposure();
                    break;
                case R.id.action_rotate_text_dir_item:
                    openRotateTextDirMenu();
                    break;
                case R.id.action_document_qr_item:
                    startQRMode();
                    break;
            }
        };

        ActionSheet.DialogStatus f = new ActionSheet.DialogStatus() {
            @Override
            public void onShown() {
//                mPaintView.drawTextOrientationLarge(true);
            }

            @Override
            public void onDismiss() {
                restoreLastTab();
            }
        };

        ActionSheet sheet = new ActionSheet(actions, s, f);

        getSupportFragmentManager().beginTransaction().add(sheet, "TAG").commit();

//        sheet.onDismiss(new DialogInterface() {
//            @Override
//            public void cancel() {
//                restoreLastTab();
//            }
//
//            @Override
//            public void dismiss() {
//                restoreLastTab();
//            }
//        });

    }

    private void restoreLastTab() {

//        do nothing in the selection listener:
        mRestoreTab = true;

        final TabLayout t = findViewById(R.id.camera_tablayout);
        t.getTabAt(mLastTabPosition).select();
    }

    @NonNull
    private ArrayList<SheetAction> getSettingsSheetActions() {

        ArrayList<SheetAction> actions = new ArrayList<>();

//        Text orientation action:
        actions.add(new SheetAction(
                R.id.action_rotate_text_dir_item,
                getString(R.string.action_setting_rotate_text_dir),
                R.drawable.ic_rotate_text_gray_24px));

        if (preferencesHandler.getValue().getShowGrid())
            actions.add(new SheetAction(
                    R.id.action_hide_grid_item,
                    getString(R.string.action_setting_hide_grid),
                    R.drawable.ic_grid_off_gray_24dp));
        else
            actions.add(new SheetAction(
                    R.id.action_show_grid_item,
                    getString(R.string.action_setting_show_grid),
                    R.drawable.ic_grid_on_gray_24dp));

//        White balance - if supported:
        if (mLockExposureSupported) {
            //        This is dirty, but at the moment I do not want another member:
            boolean wbLocked =
                    findViewById(R.id.lock_exposure_text_view).getVisibility() == View.VISIBLE;
            if (wbLocked)
                actions.add(new SheetAction(
                        R.id.action_unlock_wb_item,
                        getString(R.string.action_setting_unlock_wb),
                        R.drawable.ic_lock_open_gray_24dp));
            else
                actions.add(new SheetAction(
                        R.id.action_lock_wb_item,
                        getString(R.string.action_setting_lock_wb),
                        R.drawable.ic_lock_outline_gray_24dp));
        }

        actions.add(new SheetAction(
                R.id.action_document_qr_item,
                getString(R.string.action_setting_qr_mode),
                R.drawable.ic_qr_code_gray));

        return actions;
    }

    private void showLockedExposureDialog() {
        if (!preferencesHandler.getValue().getShowExposureLockWarning())
            return;

        MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(CameraActivity.this);
        LayoutInflater adbInflater = LayoutInflater.from(this);
        View eulaLayout = adbInflater.inflate(R.layout.check_box_dialog, null);

        final CheckBox checkBox = eulaLayout.findViewById(R.id.skip);
        alertDialog.setView(eulaLayout);
        alertDialog.setTitle(R.string.camera_lock_exposure_title);
        alertDialog.setMessage(R.string.camera_lock_exposure_msg);

        alertDialog.setPositiveButton(getString(R.string.button_ok),
                (dialog, which) -> {
                    if (checkBox.isChecked()) {
                        preferencesHandler.getValue().setShowExposureLockWarning(false);
                    }
                });

        alertDialog.show();

    }

    private void loadPreferences() {
        mIsSeriesMode = preferencesHandler.getValue().isSeriesModeActive();
        if (mIsSeriesMode)
            mIsSeriesModePaused = true;

        if (mIsSeriesMode)
            setInstructionText(getString(R.string.instruction_series_paused));

        updateMode();
    }

    /**
     * This function accesses the hardware buttons (like volume buttons). We need this access,
     * because shutter remotes emulate such a key press over bluetooth.
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (mIsPictureSafe)
                // get an image from the camera
                takePicture();
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            super.onBackPressed();
        }

        return true;
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.camera_menu, menu);
//
//        mFlashMenuItem = menu.findItem(R.id.flash_mode_item);
//        mDocumentMenuItem = menu.findItem(R.id.document_item);
//        mGalleryMenuItem = menu.findItem(R.id.gallery_item);
//        mUploadMenuItem = menu.findItem(R.id.upload_item);
////        mWhiteBalanceMenuItem = menu.findItem(R.id.white_balance_item);
//        mLockExposureMenuItem = menu.findItem(R.id.lock_exposure_item);
//        mUnlockExposureMenuItem = menu.findItem(R.id.unlock_exposure_item);
//
////        inflater.inflate(R.menu.white_balance_menu, mWhiteBalanceMenuItem.getSubMenu());
//
//        // The flash menu item is not visible at the beginning ('weak' devices might have no flash)
//        if (mFlashModes != null && !mIsSeriesMode)
//            mFlashMenuItem.setVisible(true);
//
//        MenuItem item = menu.findItem(R.id.grid_item);
//
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean showGrid = sharedPref.getBoolean(getResources().getString(R.string.key_show_grid), false);
//
//        if (showGrid)
//            item.setTitle(getString(R.string.hide_grid_item_title));
//        else
//            item.setTitle(getString(R.string.show_grid_item_title));
//
//        return true;
//
//    }

//    @SuppressWarnings("deprecation")
//    private void initDrawables() {
//
////        We need to use AppCompatResources for drawables from vector files for pre lollipop devices:
//        mFlashAutoDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_flash_auto_white_24dp);
//        mFlashOffDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_flash_off_white_24dp);
//        mFlashOnDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_flash_on_white_24dp);
//        mFlashTorchDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_lightbulb_outline_white_24dp);
//
//    }

    /**
     * Called after permission has been given or has been rejected. This is necessary on Android M
     * and younger Android systems.
     *
     * @param requestCode  Request code
     * @param permissions  Permission
     * @param grantResults results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isPermissionGiven = false;
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_LOCATION:
                // we accept either coarse or fine location
                for (int result : grantResults) {
                    if (result == PackageManager.PERMISSION_GRANTED) {
                        isPermissionGiven = true;
                        break;
                    }
                }
                if (isPermissionGiven) {
                    startLocationAccess();
                } else {
                    preferencesHandler.getValue().setGeoTaggingEnabled(false);
                    showDialog(ADialog.DialogAction.LOCATION_PERMISSION_DISABLED);
                }
                break;
            case PERMISSION_CAMERA:
                isPermissionGiven = (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
                enablePreview(isPermissionGiven);
                break;
        }
    }

    /**
     * Start the CreateDocumentActivity via an intent.
     *
     * @param qrText
     */
    private void startCreateSeriesActivity(String qrText) {
        stopQRMode();
        startActivityForResult(CreateDocumentActivity.Companion.newInstance(this, qrText), CREATE_DOCUMENT_FROM_QR_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_DOCUMENT_FROM_QR_REQUEST) {
//            Take care that the image processing is resumed, regardless of the intent result:
            resumeFromQRMode();
        }
    }


    // ================= start: methods for opening the gallery =================

    /**
     * Connects the gallery button with its OnClickListener.
     */
    private void initGalleryCallback() {

        mGalleryButton = findViewById(R.id.gallery_button);
        mProgressBar = findViewById(R.id.saving_progressbar);

        mGalleryButton.setOnClickListener(
                v -> viewModel.getValue().initiateGallery());
    }

    // ================= end: methods for opening the gallery =================

    // ================= start: methods for accessing the location =================
    private void requestLocation() {
        if (preferencesHandler.getValue().isGeoTaggingEnabled()) {
            if (!PermissionHandler.INSTANCE.isLocationPermissionGiven(this)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showDialog(ADialog.DialogAction.RATIONALE_LOCATION_PERMISSION);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE_LOCATION);
                }
            } else {
                startLocationAccess();
            }
        }
    }

    private void startLocationAccess() {

//        This can be used to let the user enable GPS:
//        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//        startActivity(intent);
        // TODO: this does not ensure that the location services are enabled.
        LocationHandler.getInstance(this);

    }

    // ================= start: methods for saving pictures =================

    /**
     * Callback called after an image has been taken by the camera.
     */
    @SuppressWarnings("deprecation")
    private void initPictureCallback() {
        mIsPictureSafe = true;
        // Callback for picture saving:
        mPictureCallback = (data, camera) -> {

            mTimerCallbacks.onTimerStopped(SHOT_TIME);
            mTimerCallbacks.onTimerStarted(SHOT_TIME);
            mTimerCallbacks.onTimerStopped(FLIP_SHOT_TIME);

            try {
                // resume the camera again (this is necessary on the Nexus 5X, but not on the Samsung S5)
                if (mCameraPreview.getCamera() != null && !viewModel.getValue().isRetakeMode()) {
                    mCameraPreview.getCamera().startPreview();
                }
            } catch (RuntimeException e) {
//                    We catch this to avoid:
//                    java.lang.RuntimeException: Camera is being used after Camera.release() was called
            }

            savePicture(data);
        };
    }

    /**
     * Setup a listener for photo shoot button.
     */
    private void initPhotoButton() {

        ImageButton photoButton = findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        photoButton.setOnClickListener(
                v -> {
                    if (mIsSeriesMode) {
                        mIsSeriesModePaused = !mIsSeriesModePaused;
//    removing mForceShootButton:
//                            if (mIsSeriesMode && mForceShootButton != null)
//                                mForceShootButton.setVisibility(View.INVISIBLE);
//                            showShootModeToast();
                        if (mIsSeriesModePaused)
                            setInstructionText(getString(R.string.instruction_series_paused));
                        updateMode();

//                            // Show the SeriesGeneralActivity just if the user started the series mode and the hide
//                            // dialog setting is not true:
//                            if (mIsSeriesMode && !mIsSeriesModePaused &&  !mHideSeriesDialog)
//                                startDocumentActivity();
                    } else if (mIsPictureSafe) {
//                            In manual mode remove the focus point:
                        if (mCameraPreview != null)
                            mCameraPreview.startContinousFocus();
                        // get an image from the camera
                        takePicture();
                    }
                });

    }

    /**
     * Initializes the buttons that are used in the camera_controls_layout. These layouts are
     * recreated on orientation changes, so we need to assign the callbacks again.
     */
    private void initButtons() {

        initPhotoButton();
        initGalleryCallback();
//        initShootModeSpinner();
        updatePhotoButtonIcon();
        initCancelQRButton();
        //    removing mForceShootButton:
//        initForceShootButton();
        initHudButton();
        initInfoButton();

    }

    private void initInfoButton() {

        AppCompatImageButton infoButton = findViewById(R.id.camera_info_button);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final boolean resumeSeriesModeAfterClose;

                if (mIsSeriesMode && !mIsSeriesModePaused) {
//                    We pause series moment here:
                    mIsSeriesModePaused = true;
                    resumeSeriesModeAfterClose = true;
                    updateMode();
                } else
                    resumeSeriesModeAfterClose = false;

                MaterialAlertDialogBuilder alertDialog = new MaterialAlertDialogBuilder(CameraActivity.this);

                String statusText = mTextView.getText().toString();
                String dialogTitle = getString(R.string.camera_status_title_prefix) + " " +
                        statusText;
                alertDialog.setTitle(dialogTitle);

                String text = getInstructionDetail(statusText);

                boolean showSeriesModeHint = isHintForSeriesModeRequired(statusText);
                if (showSeriesModeHint)
                    text += "\n\n" + getString(R.string.instruction_series_fix);


                alertDialog.setMessage(text);
                alertDialog.setPositiveButton(getString(R.string.button_ok),
                        (dialog, which) -> {
                            if (resumeSeriesModeAfterClose) {
                                Timber.d("resumeSeriesModeAfterClose");
                                mIsSeriesModePaused = false;
                                updateMode();
                            }
                        });
                alertDialog.setOnDismissListener(dialogInterface -> {
                    if (resumeSeriesModeAfterClose) {
                        Timber.d("resumeSeriesModeAfterClose");
                        mIsSeriesModePaused = false;
                        updateMode();
                    }
                });
                alertDialog.show();
            }
        });

    }

    private boolean isHintForSeriesModeRequired(String statusText) {

        if (!mIsSeriesMode)
            return false;

        if (statusText.equalsIgnoreCase(getString(R.string.instruction_unsharp)) ||
                statusText.equalsIgnoreCase(getString(R.string.instruction_no_changes)) ||
                statusText.equalsIgnoreCase(getString(R.string.instruction_movement)) ||
                statusText.equalsIgnoreCase(getString(R.string.instruction_perspective)) ||
                statusText.equalsIgnoreCase(getString(R.string.instruction_rotation)) ||
                statusText.equalsIgnoreCase(getString(R.string.instruction_empty)))
            return true;
        else
            return false;

    }

    private void initHudButton() {

        boolean showHudButton = preferencesHandler.getValue().getShowHUD();

        AppCompatImageButton hudButton = findViewById(R.id.hud_button);
        if (!showHudButton)
            hudButton.setVisibility(View.INVISIBLE);
        else {
            hudButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mirrorScreen(!isScreenMirrored());
                }
            });
        }
    }

    /**
     * Returns true if the screen is mirrored on the y-axes.
     *
     * @return
     */
    private boolean isScreenMirrored() {

        return findViewById(R.id.main_frame_layout).getScaleY() == -1.f;

    }

    /**
     * Mirrors the screen on the y-axes.
     */
    private void mirrorScreen(boolean mirror) {

        float scale;
        if (mirror)
            scale = -1.f;
        else
            scale = 1.f;

        View mainFrame = findViewById(R.id.main_frame_layout);
        mainFrame.setScaleY(scale);

    }

    //    removing mForceShootButton:
//    private void initForceShootButton() {
//
//        mForceShootButton = findViewById(R.id.force_shoot_button);
//        mForceShootButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                takePicture();
//            }
//        });
//
//    }

    private void initCancelQRButton() {

//        ImageButton button = findViewById(R.id.cancel_qr_button);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                resumeFromQRMode();
//            }
//        });

        FloatingActionButton fab = findViewById(R.id.cancel_qr_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resumeFromQRMode();
            }
        });

    }

    private void resumeFromQRMode() {
        mIsQRActive = false;
        mCameraPreview.startQrMode(false);
        IPManager.getInstance().setProcessFrame(true);
        IPManager.getInstance().setIsPaused(false);
        mRestoreTab = false;
        showControlsLayout(!mIsQRActive);
    }

//    private void initShootModeSpinner() {
//
//        // Spinner for shoot mode:
//        Spinner shootModeSpinner = findViewById(R.id.shoot_mode_spinner);
//        String[] shootModeText = getResources().getStringArray(R.array.shoot_mode_array);
//        Integer[] shootModeIcons = new Integer[]{R.drawable.ic_photo_vector, R.drawable.ic_burst_mode_vector};
//        shootModeSpinner.setAdapter(new ShootModeAdapter(this, R.layout.spinner_row, shootModeText, shootModeIcons));
//        shootModeSpinner.setOnItemSelectedListener(this);
//
////        Used to prevent firing the onItemSelected method:
//        mItemSelectedAutomatically = true;
//        if (mIsSeriesMode)
//            shootModeSpinner.setSelection(SERIES_POS);
//
//    }

    public void startAutomaticMode(boolean automatic) {

        Timber.d("startAutomaticMode");

        if (automatic) {
            mIsSeriesMode = true;
            mIsSeriesModePaused = false;
            if (mCameraPreview != null)
                mCameraPreview.startAutoFocus();
        } else {
            mIsSeriesMode = false;
            if (mCameraPreview != null)
                mCameraPreview.startContinousFocus();
            if (mPaintView != null)
                mPaintView.drawMovementIndicator(false); // This is necessary to prevent a drawing of the movement indicator
        }

        updateMode();

    }

//    @Override
//    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//
//        if ((position == SERIES_POS) && !mIsSeriesMode) {
//            mIsSeriesMode = true;
//            mIsSeriesModePaused = false;
//            mCameraPreview.startAutoFocus();
//        }
//        else if (position != SERIES_POS && mIsSeriesMode) {
//            mIsSeriesMode = false;
//            mCameraPreview.startContinousFocus();
//            mPaintView.drawMovementIndicator(false); // This is necessary to prevent a drawing of the movement indicator
//        }
//
//        // Show a toast and update the mode, but just if he selected the spinner manually:
//        if (!mItemSelectedAutomatically) {
//            showShootModeToast();
//            updateMode();
//
//        }
//
//        mItemSelectedAutomatically = false;
//
//
//
//    }


    private void showShootModeToast() {

        int msg;
        if (mIsSeriesMode) {
            if (mIsSeriesModePaused)
                msg = R.string.toast_series_paused;
            else
                msg = R.string.toast_series_started;
        } else
            msg = R.string.toast_single;

        showToastText(msg);
    }

//    private void updateShootModeSpinner() {
//
//        Spinner shootModeSpinner = findViewById(R.id.shoot_mode_spinner);
//        if (mIsSeriesMode)
//            shootModeSpinner.setSelection(SERIES_POS);
//        else
//            shootModeSpinner.setSelection(SINGLE_POS);
//
//    }

    private void updatePhotoButtonIcon() {

        ImageButton photoButton = findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        int drawable;

        if (mIsSeriesMode) {
            if (mIsSeriesModePaused) {
                drawable = R.drawable.ic_play_arrow_24dp;
//                displaySeriesModePaused(); // shows a text in the text view and removes any CVResults shown.
                clearCVResult();
            } else {
                drawable = R.drawable.ic_pause_24dp;
//                setTextViewText(R.string.instruction_series_started);
            }
        } else {
//            showToastText(R.string.toast_single);
            drawable = R.drawable.ic_photo_camera_white_24dp;
//            mIsSeriesModePaused = false;
        }

//        if (mCameraPreview != null)
//            mCameraPreview.pauseImageProcessing(mIsSeriesModePaused);

        photoButton.setImageResource(drawable);


    }

    private void updateMode() {

        ImageButton photoButton = findViewById(R.id.photo_button);
        if (photoButton == null)
            return;

        int drawable;

        if (mIsSeriesMode) {
            if (mIsSeriesModePaused) {
                drawable = R.drawable.ic_play_arrow_24dp;
                clearCVResult();
//                displaySeriesModePaused(); // shows a text in the text view and removes any CVResults shown.
//    removing mForceShootButton:
//                if (mForceShootButton != null)
//                    mForceShootButton.setVisibility(View.INVISIBLE);
            } else {
                drawable = R.drawable.ic_pause_24dp;
//                setTextViewText(R.string.instruction_series_started);
            }
        } else {
//            showToastText(R.string.toast_single);
            drawable = R.drawable.ic_photo_camera_white_24dp;
            mIsSeriesModePaused = false;
//    removing mForceShootButton:
//            if (mForceShootButton != null)
//                mForceShootButton.setVisibility(View.INVISIBLE);
        }

//        Hide the flash button in series mode:
//        if (mFlashMenuItem != null)
//            mFlashMenuItem.setVisible(!mIsSeriesMode);

        if (mCVResult != null)
            mCVResult.setSeriesMode(mIsSeriesMode);

//        if (mCameraPreview != null)
//            mCameraPreview.pauseImageProcessing(mIsSeriesModePaused, mIsFocusMeasured);

        IPManager.getInstance().setIsSeriesMode(mIsSeriesMode);
        IPManager.getInstance().setIsPaused(mIsSeriesModePaused);

//        IPManager.getInstance().setIsPaused(mIsSeriesModePaused);

        photoButton.setImageResource(drawable);

        if (mIsSeriesMode) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

//        // Show the SeriesGeneralActivity just if the user started the series mode and the hide
//        // dialog setting is not true:
//        if (mIsSeriesMode && !mIsSeriesModePaused && !mItemSelectedAutomatically && !mHideSeriesDialog) {
//            startDocumentActivity();
//        }


    }

//    @Override
//    public void onNothingSelected(AdapterView<?> parent) {
//
//    }


    /**
     * Tells the camera to take a picture.
     */
    private void takePicture() {

//        if (!mIsPictureSafe)
//            return;

        mIsPictureSafe = false;
        Camera.ShutterCallback shutterCallback = () -> {
            mPaintView.showFlicker();
            boolean flashInSeriesMode = preferencesHandler.getValue().isFlashSeriesMode();

            if (flashInSeriesMode && mIsSeriesMode)
                mCameraPreview.shortFlash();
        };

        if (mCameraPreview.getCamera() != null) {
            mCameraPreview.getCamera().takePicture(shutterCallback, null, mPictureCallback);
        }
    }

    /**
     * Save the image in an own thread (AsyncTask):
     *
     * @param data image as a byte stream.
     */
    private void savePicture(byte[] data) {
        Timber.d("savePicture");
        viewModel.getValue().saveRawImageData(data,
                getExifOrientation(),
                getDPI(),
                LocationHandler.getInstance(this).getLocation());
    }

    private void showSaveErrorDialog(Throwable throwable) {
        // TODO: Play an error sound, to indicate that something is wrong as the user might not see the display during automatic series mode.
        // TODO: Take throwable into consideration.
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(CameraActivity.this);
        builder.setMessage(R.string.picture_save_error_text).setTitle(R.string.picture_save_error_title);

        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mIsPictureSafe = true;
            }
        });

        builder.create().show();

    }


    // ================= end: methods for saving pictures =================


    // ================= start: methods for navigation drawer =================
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    /**
     * Called after configuration changes -> This includes also orientation change. By handling
     * orientation changes by ourselves, we can prevent a restart of the camera, which results in a
     * speedup.
     *
     * @param newConfig new configuration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        Timber.d("configuration changed");
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);

        rotateCameraAndLayout();

    }

    /**
     * Initializes an OrientEventListener that is used to detect orientation changes that are not
     * detected by onConfigurationChanged (e.g. landscape to reverse landscape and portrait to reverse portrait).
     */
//    private void initOrientationListener() {
//
//        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//
//                int displayRotation = getDisplayRotation();
//
////                Catch the display rotation changes that are not covered by configuration changes
////                (e.g. landscape to reverse landscape and portrait to reverse portrait):
//                if ((mLastDisplayRotation == Surface.ROTATION_0 && displayRotation == Surface.ROTATION_180) ||
//                        (mLastDisplayRotation == Surface.ROTATION_180 && displayRotation == Surface.ROTATION_0) ||
//                        (mLastDisplayRotation == Surface.ROTATION_90 && displayRotation == Surface.ROTATION_270) ||
//                        (mLastDisplayRotation == Surface.ROTATION_270 && displayRotation == Surface.ROTATION_90)) {
//                    if (mCameraPreview != null)
//                        mCameraPreview.displayRotated();
//                }
//
//                mLastDisplayRotation = displayRotation;
//
//            }
//        };
//    }
    private void rotateCameraAndLayout() {

        // Tell the camera that the orientation changed, so it can adapt the preview orientation:
        if (mCameraPreview != null)
            mCameraPreview.displayRotated();

//        Rotating a mirrored screen causes some layout errors, so we de-mirror it before:
        if (isScreenMirrored())
            mirrorScreen(false);

        // Change the layout dynamically: Remove the current camera_controls_layout and add a new
        // one, which is appropriate for the orientation (portrait or landscape xml's).
        ViewGroup appRoot = findViewById(R.id.main_frame_layout);
        if (appRoot == null)
            return;

        View f = findViewById(R.id.controls);
        if (f == null)
            return;

        appRoot.removeView(f);

        getLayoutInflater().inflate(R.layout.camera_controls_layout, appRoot);
//        View view = findViewById(R.id.camera_controls_layout);
//        view.setBackgroundColor(getResources().getColor(R.color.control_background_color_transparent));

        showControlsLayout(!mIsQRActive);

//        // Initialize the newly created buttons:
        initButtons();
    }

    public Point getPreviewDimension() {

//        Taken from: http://stackoverflow.com/questions/1016896/get-screen-dimensions-in-pixels
        View v = findViewById(R.id.controls);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null || wm.getDefaultDisplay() == null)
            return new Point();

        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Point dim = null;

        if (v != null) {
            int rotation = getDisplayRotation(this);
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
                dim = new Point(size.x, size.y - v.getHeight());
//                return size.y - v.getHeight();
            else if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
                dim = new Point(size.x - v.getWidth(), size.y);
//                return size.x - v.getWidth();
        }

        return dim;


    }

    private void showDebugView(boolean showDebugView) {


        if (showDebugView && !mIsDebugViewEnabled) {
            // Create the debug view - if it is not already created:
            if (mDebugViewFragment == null) {
                mDebugViewFragment = new DebugViewFragment();
            }

            getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
        } else if (!showDebugView && mIsDebugViewEnabled && mDebugViewFragment != null) {

            getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();

        }

        mIsDebugViewEnabled = showDebugView;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {

            case R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;

//        Deleted the overflow button, so I had to comment this out:

////            Show / hide the debug view
//            case R.id.debug_view_item:
//
//                // Create the debug view - if it is not already created:
//                if (mDebugViewFragment == null) {
//                    mDebugViewFragment = new DebugViewFragment();
//                }
//
//                // Show the debug view:
//                if (getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT) == null) {
//                    mIsDebugViewEnabled = true;
//                    item.setTitle(R.string.hide_debug_view_text);
//                    getSupportFragmentManager().beginTransaction().add(R.id.container_layout, mDebugViewFragment, DEBUG_VIEW_FRAGMENT).commit();
//                }
//                // Hide the debug view:
//                else {
//                    mIsDebugViewEnabled = false;
//                    item.setTitle(R.string.show_debug_view_text);
//                    getSupportFragmentManager().beginTransaction().remove(mDebugViewFragment).commit();
//                }
//
//                return true;
//
//            // Switch between the two page segmentation methods:
//            case R.id.use_lab_item:
//
//                if (NativeWrapper.useLab()) {
//                    NativeWrapper.setUseLab(false);
//                    item.setTitle(R.string.precise_page_seg_text);
//                }
//                else {
//                    NativeWrapper.setUseLab(true);
//                    item.setTitle(R.string.fast_page_seg_text);
//                }
//
//                return true;
//
//            // Focus measurement:
//            case R.id.show_fm_values_item:
//
//                if (mPaintView.isFocusTextVisible()) {
//                    item.setTitle(R.string.show_fm_values_text);
//                    mPaintView.drawFocusText(false);
//                } else {
//                    item.setTitle(R.string.hide_fm_values_text);
//                    mPaintView.drawFocusText(true);
//                }
//
//                break;
//
//            // Guide lines:
//            case R.id.show_guide_item:
//
//                if (mPaintView.areGuideLinesDrawn()) {
//                    mPaintView.drawGuideLines(false);
//                    item.setTitle(R.string.show_guide_text);
//                } else {
//                    mPaintView.drawGuideLines(true);
//                    item.setTitle(R.string.hide_guide_text);
//                }
//
//                break;
//
////            // Threading:
////            case R.id.threading_item:
////                if (mCameraPreview.isMultiThreading()) {
////                    mCameraPreview.setThreading(false);
////                    item.setTitle(R.string.multi_thread_text);
////                }
////                else {
////                    mCameraPreview.setThreading(true);
////                    item.setTitle(R.string.single_thread_text);
////                }
//
//
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }


    }

    private void setupDebugView() {

        mDebugViewFragment = (DebugViewFragment) getSupportFragmentManager().findFragmentByTag(DEBUG_VIEW_FRAGMENT);
        mTimerCallbacks = this;

        mIsDebugViewEnabled = (mDebugViewFragment == null);
        if (mDebugViewFragment == null)
            mIsDebugViewEnabled = false;
        else
            mIsDebugViewEnabled = true;

    }

    /**
     * Initializes the navigation drawer, when the app is started.
     */
    @SuppressWarnings("deprecation")
    private void setupNavigationDrawer() {

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        NavigationView mDrawer = findViewById(R.id.left_drawer);
        setupDrawerContent(mDrawer);

    }


    /**
     * Connects the items in the navigation drawer with a listener.
     *
     * @param navigationView NavigationView
     */
    private void setupDrawerContent(NavigationView navigationView) {

        navigationView.setNavigationItemSelectedListener(

                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {

                        selectDrawerItem(menuItem);
                        return true;
                    }
                });
    }

    /**
     * Called after an item is selected in the navigation drawer.
     *
     * @param menuItem ID of the selected item.
     */
    private void selectDrawerItem(MenuItem menuItem) {

        mDrawerLayout.closeDrawers();

    }

    // ================= end: methods for navigation drawer =================


    // ================= start: CALLBACKS invoking TaskTimer =================

    /**
     * Called before a task is executed. This is used to measure the time of the task execution.
     *
     * @param type TaskType of the sender, as defined in TaskTimer.
     */
    @Override
    public void onTimerStarted(TaskTimer.TaskType type) {

        // Do nothing if the debug view is not visible:
        if (!mIsDebugViewEnabled)
            return;

        if (mTaskTimer == null)
            return;

        mTaskTimer.startTaskTimer(type);


    }

    /**
     * Called after a task is executed. This is used to measure the time of the task execution.
     *
     * @param type
     */
    @Override
    public void onTimerStopped(final TaskTimer.TaskType type) {

        if (!mIsDebugViewEnabled)
            return;

        if (mTaskTimer == null)
            return;

        final long timePast = mTaskTimer.getTaskTime(type);

        // Normally the timer callback should just be called if the debug view is visible:
        if (mIsDebugViewEnabled) {

            if (mDebugViewFragment != null) {
                // The update of the UI elements must be done from the UI thread:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDebugViewFragment.setTimeText(type, timePast);
                    }
                });
            }
        }
    }


//    /**
//     * Returns true if the debug view is visible. Mainly used before TaskTimer events are triggered.
//     *
//     * @return boolean
//     */
//    public static boolean isDebugViewEnabled() {
//
//        return mIsDebugViewEnabled;
//
//    }

    // ================= stop: CALLBACKS invoking TaskTimer =================


    public static int getDisplayRotation(Context context) {
        WindowManager w = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        return w.getDefaultDisplay().getRotation();
    }


    // ================= start: CALLBACKS called from native files =================

    @Override
    public void onMovement(boolean moved) {

        if (IPManager.getInstance().getIsPaused()) {
            mPaintView.drawMovementIndicator(false);
            mCVResult.clearResults();
            return;
        }

        // This happens if the user has just switched to single mode and the event occurs later than the touch event.
        if (!mIsSeriesMode) {
            mPaintView.drawMovementIndicator(false);
            return;
        } else if (mIsSeriesModePaused) {
            clearCVResult();
//            displaySeriesModePaused();
            return;
        }

        mPaintView.drawMovementIndicator(moved);

        if (moved) {
            mCVResult.clearResults();
            setInstructionText(getString(R.string.instruction_movement));
        }

//        else {
//            // This forces an update of the textview if it is still showing the R.string.instruction_movement text
//            if (mTextView.getText() == getResources().getString(R.string.instruction_movement))
//                setTextViewText(R.string.instruction_none);
//        }

    }

    /**
     * Called after focus measurement is finished.
     *
     * @param patches Patches array
     */
    @Override
    public void onFocusMeasured(Patch[] patches) {

        if (IPManager.getInstance().getIsPaused()) {
            mPaintView.drawMovementIndicator(false);
            mCVResult.clearResults();
            return;
        }

        if (mCVResult != null)
            mCVResult.setPatches(patches);

//        if (mCVResult != null && patches != null && patches.length > 0)
//            mCVResult.setPatches(patches);

//        CVManager.getInstance().setNextTask(TASK_TYPE_MOVE);

//        if (mIsSeriesMode)
//            CVManager.getInstance().setNextTask(TASK_TYPE_MOVE);
//        else
//            CVManager.getInstance().setNextTask(TASK_TYPE_PAGE);

    }

    /**
     * Called after page segmentation is finished.
     *
     * @param polyRects Array of polyRects
     */
    @Override
    public void onPageSegmented(DkPolyRect[] polyRects) {

        if (IPManager.getInstance().getIsPaused()) {
            mPaintView.drawMovementIndicator(false);
            mCVResult.clearResults();
            return;
        }
        mTimerCallbacks.onTimerStopped(PAGE_SEGMENTATION);

        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - mLastTime;

        mLastTime = currentTime;

        if (mCVResult != null) {
//            mCVResult.setPatches(null);
//            if (!mCVResult.isStable())
//                mCVResult.setPatches(new Patch[0]);

            mCVResult.setDKPolyRects(polyRects);
        }

//        if (isRectJumping(dkPolyRects))
//            mCameraPreview.startFocusMeasurement(false);
//        else
//            mCameraPreview.startFocusMeasurement(true);

        mLastDkPolyRects = polyRects;

        mTimerCallbacks.onTimerStarted(PAGE_SEGMENTATION);

    }


    boolean isRectJumping(DkPolyRect[] dkPolyRects) {

        boolean isJumping = false;

        Timber.d("jumping?");

        if (dkPolyRects != null && mLastDkPolyRects != null) {
            Timber.d("check 1");
            if (dkPolyRects.length == 1 && mLastDkPolyRects.length == 1) {
                Timber.d("check 2");
                PointF distVec = mLastDkPolyRects[0].getLargestDistVector(dkPolyRects[0]);
                PointF normedPoint = mCVResult.normPoint(distVec);

                if (normedPoint.length() >= .05) {
                    isJumping = true;
                }

                Timber.d("distance: %s", normedPoint.length());
            }
        }

        return isJumping;
    }


    // ================= end: CALLBACKS called from native files =================


    // =================  start: CameraPreview.CameraPreviewCallback CALLBACK =================

    /**
     * Called after the dimension of the camera view is set. The dimensions are necessary to convert
     * the frame coordinates to view coordinates.
     *
     * @param width  width of the view
     * @param height height of the view
     */
    @Override
    public void onMeasuredDimensionChange(int width, int height) {

        mCVResult.setViewDimensions(width, height);
        Timber.d("onMeasuredDimensionChange: " + width + " + " + height);

    }

    @Override
    public void onFlashModesFound(List<String> modes) {

        mFlashModes = modes;

        if (mFlashPopupMenu != null) // Menu is not created yet
            setupFlashUI();

        // The flash menu item is not visible at the beginning ('weak' devices might have no flash)
//        if (mFlashModes != null && mFlashMenuItem != null)
//            mFlashMenuItem.setVisible(true);

    }

    @Override
    public void onExposureLockFound(boolean isSupported) {

        mLockExposureSupported = isSupported;
//        if (mUnlockExposureMenuItem != null && mUnlockExposureMenuItem.isVisible())
//            mUnlockExposureMenuItem.setVisible(false);
//        if (mLockExposureMenuItem != null) {
//            if (isSupported)
//                mLockExposureMenuItem.setVisible(true);
//            else
//                mLockExposureMenuItem.setVisible(false);
//        }


    }

    @Override
    public void onFocused(boolean focused) {

        Timber.d("onFocused: %s", focused);

        if (!focused)
            setInstructionText(getString(R.string.instruction_no_auto_focus));
//        else
//            setInstructionText("");

        if (mIsSeriesMode && focused)
            IPManager.getInstance().setProcessFrame(true);

    }


//    @Override
//    public void onWhiteBalanceFound(List<String> whiteBalances) {
//
////        if (mWhiteBalancePopupMenu == null) // Menu is not created yet
////            return;
////
//////        At least one white balance mode found must be handled by the UI:
////        boolean isWBSupported = false;
////        for (String whiteBalance : whiteBalances)
////            isWBSupported |= enableWBItem(whiteBalance);
////
//////        Otherwise hide the WB item:
////        if (!isWBSupported)
////            mWhiteBalanceMenuItem.setVisible(false);
//    }

//    /**
//     * Returns true if the whiteBalance is handled in the UI.
//     * @param whiteBalance
//     * @return
//     */
//    private boolean enableWBItem(String whiteBalance) {
//
//        switch (whiteBalance) {
//            case Camera.Parameters.WHITE_BALANCE_AUTO:
//                mWhiteBalancePopupMenu.getMenu().findItem(R.id.white_balance_auto_item)
//                        .setVisible(true);
//                return true;
//            case Camera.Parameters.WHITE_BALANCE_DAYLIGHT:
//                mWhiteBalancePopupMenu.getMenu().findItem(R.id.white_balance_sunny_item)
//                        .setVisible(true);
//                return true;
//            default:
//                return false;
//        }
//
//    }

//    private void enableWBButton(final String whiteBalance) {
//
//        AppCompatButton button = null;
//        switch(whiteBalance) {
//            case Camera.Parameters.WHITE_BALANCE_AUTO:
//                button = findViewById(R.id.wb_auto_button);
//                break;
//            case Camera.Parameters.WHITE_BALANCE_INCANDESCENT:
//                button = findViewById(R.id.wb_incandescent_button);
//                break;
//            case Camera.Parameters.WHITE_BALANCE_FLUORESCENT:
//                button = findViewById(R.id.wb_iridescent_button);
//                break;
//            case Camera.Parameters.WHITE_BALANCE_DAYLIGHT:
//                button = findViewById(R.id.wb_sunny_button);
//        }
//
//        if (button != null) {
//            button.setVisibility(View.VISIBLE);
//            button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (mCameraPreview != null)
//                        mCameraPreview.setWhiteBalance(whiteBalance);
//                }
//            });
//        }
//
//    }

//    private void enableWB

    @Override
    public void onFocusTouch(PointF point) {

        Timber.d("onFocusTouch");

        if (mPaintView != null)
            mPaintView.drawFocusTouch(point);

    }

    @Override
    public void onFocusTouchSuccess() {


        if (mPaintView != null)
            mPaintView.drawFocusTouchSuccess();

    }


    @Override
    public void onWaitingForDoc(boolean waiting, boolean doAutoFocus) {

        if (mIsSeriesModePaused) {
//            displaySeriesModePaused();
            clearCVResult();
            return;
        }

//        if (waiting)
//            setTextViewText(R.string.instruction_no_changes);
//        else if (mIsAggressiveAutoFocus)
//            mCameraPreview.focusOnPoint();

        if (waiting)
            setInstructionText(getString(R.string.instruction_no_changes));
//            setTextViewText(R.string.instruction_no_changes);
        else if (doAutoFocus)
            mCameraPreview.focusOnPoint();


    }

    @Override
    public void onCaptureVerified() {

        if (!mIsPictureSafe) {
            Timber.d("onCaptureVerified: not safe to save picture");
            return;
        }


        mTextView.setText(getResources().getString(R.string.taking_picture_text));

        takePicture();


    }

    @Override
    public void onQRCode(Result result) {

        Timber.d("qr code found: " + result);

        final String text;
        // Tell the user that we are still searching for a QR code:
        if (result == null) {
            text = getString(R.string.instruction_searching_qr);
            setInstructionText(text);

        }
        // Start the CreateDocumentActivity:
        else {
            // Stop searching for QR code:
//            stopQRMode();

            text = result.toString();
            startCreateSeriesActivity(text);

        }


    }

    private void stopQRMode() {
        mIsQRActive = false;
        mCameraPreview.startQrMode(false);
        IPManager.getInstance().setIsPaused(false);
    }

    private void setInstructionText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTextView == null)
                    return;
//                Check if the series mode is paused - in that case, do not draw other text:
                if (mIsSeriesMode && mIsSeriesModePaused) {
                    clearCVResult();
                    mTextView.setText(getString(R.string.instruction_series_paused));
                } else
                    mTextView.setText(text);
            }
        });
    }

//    @Override
//    public void onBarCodeFound(final Barcode barcode) {
//
//        final String txt;
//        if (barcode != null)
//            txt = barcode.rawValue;
//        else
//            txt = "";
//
//        //  Tell the user that a barcode is found
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                // This code will always run on the UI thread, therefore is safe to modify UI elements.
//                mTextView.setText(txt);
//            }
//        });
//
//    }

//    private void setTextViewText(int msg) {
//
//        String msgText = getResources().getString(msg);
//        if (mTextView != null)
//            mTextView.setText(msgText);
//
//    }

//    public void showSeriesPopup(MenuItem item) {
//
//        View menuItemView = findViewById(R.id.document_item);
//        if (menuItemView == null)
//            return;
//
//        // Create the menu for the first time:
//        if (mSeriesPopupMenu == null) {
//            mSeriesPopupMenu = new PopupMenu(this, menuItemView);
//            mSeriesPopupMenu.setOnMenuItemClickListener(this);
//            mSeriesPopupMenu.inflate(R.menu.series_menu);
//
//            setupFlashUI();
//        }
//
//        mSeriesPopupMenu.show();
//
//    }

//    public void showSeriesPopup(MenuItem item) {
//
//        View menuItemView = findViewById(R.id.document_item);
//        if (menuItemView == null)
//            return;
//
//        PopupMenu seriesPopupMenu = new PopupMenu(this, menuItemView);
//        seriesPopupMenu.setOnMenuItemClickListener(this);
//        seriesPopupMenu.inflate(R.menu.series_menu);
//        seriesPopupMenu.show();
//
//    }

//    public void showFlashPopup(MenuItem item) {
//
//        View menuItemView = findViewById(R.id.flash_mode_item);
//        if (menuItemView == null)
//            return;
//
//        mFlashPopupMenu = new PopupMenu(this, menuItemView);
//        mFlashPopupMenu.setOnMenuItemClickListener(this);
//        mFlashPopupMenu.inflate(R.menu.flash_mode_menu);
//
//        setupFlashUI();
//
//        mFlashPopupMenu.show();
//
//    }

    private void showGrid(boolean showGrid) {

        mPaintView.drawGrid(showGrid);

//        Save the new setting:
        preferencesHandler.getValue().setShowGrid(showGrid);

    }

    public void showGrid(MenuItem item) {

        boolean showGrid = !preferencesHandler.getValue().getShowGrid();
        mPaintView.drawGrid(showGrid);

//        Save the new setting:
        preferencesHandler.getValue().setShowGrid(showGrid);

//        Change the item text:
        if (showGrid)
            item.setTitle(getString(R.string.hide_grid_item_title));
        else
            item.setTitle(getString(R.string.show_grid_item_title));

    }

    private void lockExposure() {

        if (mCameraPreview != null) {

            showLockedExposureDialog();

            mCameraPreview.lockExposure(true);
            mCameraPreview.lockWhiteBalance(true);

//            if (mLockExposureMenuItem != null)
//                mLockExposureMenuItem.setVisible(false);
//            if (mUnlockExposureMenuItem != null)
//                mUnlockExposureMenuItem.setVisible(true);

            final LinearLayout lockTextView = findViewById(R.id.lock_exposure_text_view);
            lockTextView.setAlpha(0f);
            lockTextView.setVisibility(View.VISIBLE);
            lockTextView.animate()
                    .alpha(1f)
                    .setDuration(1000);
        }

    }

    private void unlockExposure() {

        if (mCameraPreview != null) {
            mCameraPreview.lockExposure(false);
            mCameraPreview.lockWhiteBalance(false);

//            if (mLockExposureMenuItem != null)
//                mLockExposureMenuItem.setVisible(true);
//            if (mUnlockExposureMenuItem != null)
//                mUnlockExposureMenuItem.setVisible(false);

            final LinearLayout lockTextView = findViewById(R.id.lock_exposure_text_view);
            lockTextView.setVisibility(View.INVISIBLE);

            final LinearLayout unlockTextView = findViewById(R.id.unlock_exposure_text_view);
            unlockTextView.setAlpha(1f);
            unlockTextView.setVisibility(View.VISIBLE);
            unlockTextView.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            unlockTextView.setVisibility(View.INVISIBLE);
                        }
                    });

//            final TextView lockTextView = findViewById(R.id.lock_exposure_text_view);
//            lockTextView.setAlpha(0.f);
//            lockTextView.setVisibility(View.VISIBLE);
//            lockTextView.animate()
//                    .alpha(1f)
//                    .setDuration(500);

        }

    }


    public void lockExposure(MenuItem item) {

        if (mCameraPreview != null) {

            showLockedExposureDialog();

            mCameraPreview.lockExposure(true);
            mCameraPreview.lockWhiteBalance(true);

//            if (mLockExposureMenuItem != null)
//                mLockExposureMenuItem.setVisible(false);
//            if (mUnlockExposureMenuItem != null)
//                mUnlockExposureMenuItem.setVisible(true);

            final LinearLayout lockTextView = findViewById(R.id.lock_exposure_text_view);
            lockTextView.setAlpha(0f);
            lockTextView.setVisibility(View.VISIBLE);
            lockTextView.animate()
                    .alpha(1f)
                    .setDuration(1000);
        }

    }

    public void unlockExposure(MenuItem item) {

        if (mCameraPreview != null) {
            mCameraPreview.lockExposure(false);
            mCameraPreview.lockWhiteBalance(false);

//            if (mLockExposureMenuItem != null)
//                mLockExposureMenuItem.setVisible(true);
//            if (mUnlockExposureMenuItem != null)
//                mUnlockExposureMenuItem.setVisible(false);

            final LinearLayout lockTextView = findViewById(R.id.lock_exposure_text_view);
            lockTextView.setVisibility(View.INVISIBLE);

            final LinearLayout unlockTextView = findViewById(R.id.unlock_exposure_text_view);
            unlockTextView.setAlpha(1f);
            unlockTextView.setVisibility(View.VISIBLE);
            unlockTextView.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            unlockTextView.setVisibility(View.INVISIBLE);
                        }
                    });

//            final TextView lockTextView = findViewById(R.id.lock_exposure_text_view);
//            lockTextView.setAlpha(0.f);
//            lockTextView.setVisibility(View.VISIBLE);
//            lockTextView.animate()
//                    .alpha(1f)
//                    .setDuration(500);

        }

    }

//    public void whiteBalanceAuto(MenuItem item) {
//
//        if (mCameraPreview != null)
//            mCameraPreview.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//
//    }
//
//    public void whiteBalanceDaylight(MenuItem item) {
//
//        if (mCameraPreview != null)
//            mCameraPreview.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
//
//    }
//
//    public void whiteBalanceCloudy(MenuItem item) {
//
//        if (mCameraPreview != null)
//            mCameraPreview.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_FLUORESCENT);
//
//    }

    @SuppressWarnings("deprecation")
    private void setupFlashUI() {

        if (mFlashModes == null)
            return;

//        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO))
//            mFlashPopupMenu.getMenu().findItem(R.id.flash_auto_item).setVisible(false);
//
//        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_ON))
//            mFlashPopupMenu.getMenu().findItem(R.id.flash_on_item).setVisible(false);
//
//        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_OFF))
//            mFlashPopupMenu.getMenu().findItem(R.id.flash_off_item).setVisible(false);
//
//        if (!mFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH))
//            mFlashPopupMenu.getMenu().findItem(R.id.flash_torch_item).setVisible(false);

    }

//    @Override
//    @SuppressWarnings("deprecation")
//    public boolean onMenuItemClick(MenuItem item) {
//
//        switch (item.getItemId()) {
//            case R.id.flash_auto_item:
//                mFlashMenuItem.setIcon(mFlashAutoDrawable);
//                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
//                return true;
//            case R.id.flash_off_item:
//                mFlashMenuItem.setIcon(mFlashOffDrawable);
//                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
////                mFlashMode = Camera.Parameters.FLASH_MODE_OFF;
//                return true;
//            case R.id.flash_on_item:
//                mFlashMenuItem.setIcon(mFlashOnDrawable);
//                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
////                mFlashMode = Camera.Parameters.FLASH_MODE_ON;
//                return true;
//            case R.id.flash_torch_item:
//                mFlashMenuItem.setIcon(mFlashTorchDrawable);
//                mCameraPreview.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
////                mFlashMode = Camera.Parameters.FLASH_MODE_ON;
//                return true;
//
//            case R.id.series_new_item:
//                startActivity(new Intent(getApplicationContext(), CreateDocumentActivity.class));
////                startActivity(new Intent(getApplicationContext(), MainActivity.class));
//                return true;
//
//            case R.id.series_edit_item:
//                String documentName = DocumentStorage.getInstance(this).getTitle();
//                if (documentName != null) {
//                    Intent intent = new Intent(getApplicationContext(), EditDocumentActivity.class);
//                    intent.putExtra(EditDocumentActivity.DOCUMENT_NAME_KEY, documentName);
//                    startActivity(intent);
//                }
////                startActivity(new Intent(getApplicationContext(), MainActivity.class));
//                return true;
//
//            case R.id.series_switch_item:
////                startActivity(new Intent(getApplicationContext(), OpenDocumentActivity.class));
//                startActivity(new Intent(getApplicationContext(), SelectDocumentActivity.class));
//                return true;
//
//            case R.id.series_qr_item:
//                mIsQRActive = true;
//                showControlsLayout(false);
//                mCVResult.clearResults();
//                mCameraPreview.startQrMode(true);
//
//                IPManager.getInstance().setIsPaused(true);
//                return true;
//
//            default:
//                return false;
//
//        }
//    }

    private void startQRMode() {

        mIsQRActive = true;
        showControlsLayout(false);
        mCVResult.clearResults();
        mCameraPreview.startQrMode(true);

    }


    /**
     * Called after the dimension of the camera frame is set. The dimensions are necessary to convert
     * the frame coordinates to view coordinates.
     *
     * @param width             width of the frame
     * @param height            height of the frame
     * @param cameraOrientation orientation of the camera
     */
    @Override
    public void onFrameDimensionChange(int width, int height, int cameraOrientation) {

        mCameraOrientation = cameraOrientation;
        mCVResult.setFrameDimensions(width, height, cameraOrientation);

//        if (mIsSeriesMode)
//            mCameraPreview.startAutoFocus();
//        else
//            mCameraPreview.startContinousFocus();

        CameraPaintLayout l = findViewById(R.id.camera_paint_layout);
        if (l != null)
            l.setFrameDimensions(width, height);

        if (mCameraPreview == null)
            return;

        View v = findViewById(R.id.controls);
        if ((v != null) && (!mCameraPreview.isPreviewFitting()))
            v.setBackgroundColor(getResources().getColor(R.color.control_background_color_transparent));

        if (mIsSeriesMode)
            mCameraPreview.startAutoFocus();
        else
            mCameraPreview.startContinousFocus();

////        Make the actionbar opaque in case the preview does not fit the entire screen
////        Note: this is especially for markus oneplus not tested on other devices yet:
//        if (mCameraPreview.isPreviewFitting() &&
//                (getDisplayRotation() == Surface.ROTATION_0 || getDisplayRotation() == Surface.ROTATION_180)) {
//
//            int actionBarHeight;
//
//            final TypedArray styledAttributes = getTheme().obtainStyledAttributes(
//                    new int[]{android.R.attr.actionBarSize}
//            );
//            actionBarHeight = (int) styledAttributes.getDimension(0, 0);
//            styledAttributes.recycle();
//
//            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) l.getLayoutParams();
//            params.setMargins(0, actionBarHeight, 0, 0);
//        }
//        else {
//            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) l.getLayoutParams();
//            params.setMargins(0, 0, 0, 0);
//        }

    }

    /**
     * Called after the the status of the CVResult object is changed.
     *
     * @param state state of the CVResult
     */
    @Override
    public void onStatusChange(final int state) {

        mTextView.setText(getInstructionMessage(state));
        //    removing mForceShootButton:
//        updateForceShootButton(state);

    }

    //    removing mForceShootButton:
//    private void updateForceShootButton(int state) {
//        //        Show or hide the force shoot button:
//        if (mIsSeriesMode) {
//            if (mForceShootButton != null) {
//                if (state == CVResult.DOCUMENT_STATE_UNSHARP ||
//                        state == CVResult.DOCUMENT_STATE_ROTATION ||
//                        state == CVResult.DOCUMENT_STATE_PERSPECTIVE)
//                    mForceShootButton.setVisibility(View.VISIBLE);
//                else
//                    mForceShootButton.setVisibility(View.INVISIBLE);
//            }
//        }
//    }

    private void clearCVResult() {

        if (mCVResult != null)
            mCVResult.clearResults();

        if (mPaintView != null)
            mPaintView.clearScreen();

    }


//    private void displaySeriesModePaused() {
//
//        if (mCVResult != null)
//            mCVResult.clearResults();
//
//        if (mPaintView != null)
//            mPaintView.clearScreen();
//
//        setInstructionText(R.string.instruction_series_paused);
//    }

    private void showToastText(int id) {

        String msg = getResources().getString(id);
        ToastCompat toast = ToastCompat.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();


//        Old version that cause BadTokenExceptions on API level 25. Could not catch it.
//        According to https://github.com/drakeet/ToastCompat this is an API 25 bug
//        try {
//
//            String msg = getResources().getString(id);
//
//            if (mToast != null)
//                mToast.cancel();
//
//            mToast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
//            mToast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
//            mToast.show();
//
//        }
//        catch (WindowManager.BadTokenException e) {
////            Ignore the bad token exception, according to https://github.com/drakeet/ToastCompat
////            this should only happen on API level 25, but I could not reproduce it with a virtual
////            machine.
//        }
    }


    private String getInstructionDetail(String msgText) {

        if (msgText.equals(getString(R.string.instruction_perspective)))
            return getString(R.string.instruction_perspective_detail);

        else if (msgText.equals(getString(R.string.instruction_empty)))
            return getString(R.string.instruction_empty_detail);

        else if (msgText.equals(getString(R.string.instruction_movement)))
            return getString(R.string.instruction_movement_detail);

        else if (msgText.equals(getString(R.string.instruction_no_auto_focus)))
            return getString(R.string.instruction_no_auto_focus_detail);

        else if (msgText.equals(getString(R.string.instruction_no_changes)))
            return getString(R.string.instruction_no_changes_detail);

        else if (msgText.equals(getString(R.string.instruction_no_focus_measured)))
            return getString(R.string.instruction_no_focus_measured);

        else if (msgText.equals(getString(R.string.instruction_ok)))
            return getString(R.string.instruction_ok_detail);

        else if (msgText.equals(getString(R.string.instruction_rotation)))
            return getString(R.string.instruction_rotation_detail);

        else if (msgText.equals(getString(R.string.instruction_series_paused)))
            return getString(R.string.instruction_series_paused_detail);

        else if (msgText.equals(getString(R.string.instruction_searching_qr)))
            return getString(R.string.instruction_searching_qr_detail);

//        else if (msgText.equals(getString(R.string.instruction_series_started)))
//            return getString(R.string.instruction_series_started_detail);

        else if (msgText.equals(getString(R.string.instruction_small)))
            return getString(R.string.instruction_small_detail);

        else if (msgText.equals(getString(R.string.instruction_unsharp)))
            return getString(R.string.instruction_unsharp_detail);
        else
            return "";

//        switch (msgText) {
//
//            case getString(R.string.instruction_unknown):
//
////            case CVResult.DOCUMENT_STATE_EMPTY:
////                return getResources().getString(R.string.instruction_empty_detail);
////
////            case CVResult.DOCUMENT_STATE_OK:
////                return getResources().getString(R.string.instruction_ok);
////
////            case CVResult.DOCUMENT_STATE_SMALL:
////                return getResources().getString(R.string.instruction_small_detail);
////
////            case CVResult.DOCUMENT_STATE_PERSPECTIVE:
////                return getResources().getString(R.string.instruction_perspective_detail);
////
////            case CVResult.DOCUMENT_STATE_UNSHARP:
////                return getResources().getString(R.string.instruction_unsharp_detail);
////
////            case CVResult.DOCUMENT_STATE_BAD_ILLUMINATION:
////                return getResources().getString(R.string.instruction_bad_illumination_detail);
////
////            case CVResult.DOCUMENT_STATE_ROTATION:
////                return getResources().getString(R.string.instruction_rotation_detail);
////
////            case CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED:
////                return getResources().getString(R.string.instruction_no_focus_measured_detail);
////
////            case CVResult.DOCUMENT_STATE_NO_ILLUMINATION_MEASURED:
////                return getResources().getString(R.string.instruction_no_illumination_measured_detail);
//
//        }
//
//        return getResources().getString(R.string.instruction_unknown);


    }

    /**
     * Returns instruction messages, depending on the current state of the CVResult object.
     *
     * @param state state of the CVResult object
     * @return instruction message
     */
    private String getInstructionMessage(int state) {

        switch (state) {

            case CVResult.DOCUMENT_STATE_EMPTY:
                return getResources().getString(R.string.instruction_empty);

            case CVResult.DOCUMENT_STATE_OK:
                return getResources().getString(R.string.instruction_ok);

            case CVResult.DOCUMENT_STATE_SMALL:
                return getResources().getString(R.string.instruction_small);

            case CVResult.DOCUMENT_STATE_PERSPECTIVE:
                return getResources().getString(R.string.instruction_perspective);

            case CVResult.DOCUMENT_STATE_UNSHARP:
                return getResources().getString(R.string.instruction_unsharp);

//            case CVResult.DOCUMENT_STATE_BAD_ILLUMINATION:
//                return getResources().getString(R.string.instruction_bad_illumination);

            case CVResult.DOCUMENT_STATE_ROTATION:
                return getResources().getString(R.string.instruction_rotation);

            case CVResult.DOCUMENT_STATE_NO_FOCUS_MEASURED:
                return getResources().getString(R.string.instruction_no_focus_measured);

//            case CVResult.DOCUMENT_STATE_NO_ILLUMINATION_MEASURED:
//                return getResources().getString(R.string.instruction_no_illumination_measured);

        }

        return getResources().getString(R.string.instruction_unknown);

    }

    // =================  end: CameraPreview.DimensionChange CALLBACK =================

    private void loadThumbNail(@Nullable Page page) {
        if (page == null) {
            GlideHelper.INSTANCE.loadPageIntoImageView(null, mGalleryButton, GlideHelper.GlideStyles.CAMERA_THUMBNAIL);
            mGalleryButton.setVisibility(View.INVISIBLE);
            return;
        }

        File file = fileHandler.getValue().getFileByPage(page);
        if (file != null) {
            GlideHelper.INSTANCE.loadPageIntoImageView(page, mGalleryButton, GlideHelper.GlideStyles.CAMERA_THUMBNAIL, new GlideLegacyCallback() {
                @Override
                public void onResourceReady(boolean isFirstResource) {
                    mGalleryButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onResourceFailed(boolean isFirstResource, @Nullable GlideException e) {
                    mGalleryButton.setVisibility(View.VISIBLE);
                }
            });
        } else {
            mGalleryButton.setVisibility(View.INVISIBLE);
        }
    }

    private int getDPI() {
        try {
            // try to retrieve camera dpi from preferences
            int dpi = preferencesHandler.getValue().getCameraDPI();
            if (dpi == -1) {
                // calculate camera DPI
                dpi = getDPIFromCamera();
                preferencesHandler.getValue().setCameraDPI(dpi);
            }
            return dpi;
        } catch (Exception exception) {
            Timber.e(exception, "Retrieving camera's DPI value has failed!");
            return -1;
        }
    }

    private int getDPIFromCamera() {
        //distance from tent in inches
        double cameraDistance = 16.535;
        if (mCameraPreview == null || mCameraPreview.getCamera() == null ||
                mCameraPreview.getCamera().getParameters() == null)
            return -1;

        float angle = mCameraPreview.getCamera().getParameters().getHorizontalViewAngle();
        int imgW = mCameraPreview.getCamera().getParameters().getPictureSize().width;
        return Helper.getDPI(cameraDistance, angle, imgW);
    }


    private int getExifOrientation() {

        //    private int getExifOrientation() {
//
//        switch (mCameraOrientation) {
//
//            case 0:
//                return 1;
//            case 90:
//                return 6;
//            case 180:
//                return 3;
//            case 270:
//                return 8;
//
//        }
//
//        return -1;
//
//            Log.d(CLASS_NAME, "mCameraOrientation: " + mCameraOrientation);
//            Log.d(CLASS_NAME, "mTextOrientation: " + mTextOrientation);
//
//            int camTextOrientation = mTextOrientation;
//
//
//            if (mCameraOrientation == 90)
//                camTextOrientation -= 1;
//            else if (mCameraOrientation == 180)
//                camTextOrientation += 2;
//            else if (mCameraOrientation == 270)
//                camTextOrientation += 1;

//            int camTextOrientation = mTextOrientation;
////            Compensate the camera rotation:
        int camTextOrientation = IMG_ORIENTATION_0;
        if (mCameraOrientation == 90)
            camTextOrientation = IMG_ORIENTATION_90;
//            TODO: find a way to test such a device (I am missing the 5X):
        else if (mCameraOrientation == 180)
            camTextOrientation = IMG_ORIENTATION_180;
//            TODO: find a way to test such a device (I am missing the 5X):
        else if (mCameraOrientation == 270)
            camTextOrientation = IMG_ORIENTATION_270;
//
        camTextOrientation -= mTextOrientation;
//
        if (camTextOrientation < 0)
            camTextOrientation += 4;
//
//            camTextOrientation = camTextOrientation % 4;

//            if (mCameraOrientation == 90)
//                camTextOrientation++;
        camTextOrientation = camTextOrientation % 4;

        Timber.d("mCameraOrientation: %s", mCameraOrientation);
        Timber.d("mTextOrientation: %s", mTextOrientation);
        Timber.d("camTextOrientation: %s", camTextOrientation);


        switch (camTextOrientation) {
            case IMG_ORIENTATION_0:
                return ExifInterface.ORIENTATION_NORMAL;
            case IMG_ORIENTATION_90:
                return ExifInterface.ORIENTATION_ROTATE_90;
            case IMG_ORIENTATION_180:
                return ExifInterface.ORIENTATION_ROTATE_180;
            case IMG_ORIENTATION_270:
                return ExifInterface.ORIENTATION_ROTATE_270;
            default:
                return ExifInterface.ORIENTATION_UNDEFINED;
        }


//            return ExifInterface.ORIENTATION_ROTATE_90;

    }

    @NonNull
    @Override
    protected NavigationDrawer.NavigationItem getSelfNavDrawerItem() {
        return NavigationDrawer.NavigationItem.CAMERA;
    }
}
