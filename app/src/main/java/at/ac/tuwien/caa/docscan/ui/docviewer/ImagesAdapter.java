package at.ac.tuwien.caa.docscan.ui.docviewer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.MediaStoreSignature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.ImageProcessLogger;
import at.ac.tuwien.caa.docscan.camera.cv.thread.crop.PageDetector;
import at.ac.tuwien.caa.docscan.gallery.CropRectTransform;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity;

import static at.ac.tuwien.caa.docscan.ui.docviewer.DocumentViewerActivity.LAUNCH_VIEWER_REQUEST;

/**
 * Created by fabian on 2/6/2018.
 */

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.GalleryViewHolder> {

    private static final String CLASS_NAME = "ImagesAdapter";

    private Document mDocument;
    private Context mContext;

    private int mWidth;
    private String mDocumentName;
    private CountableBooleanArray mSelections;

    // Callback to listen to selection changes:
    private ImagesAdapterCallback mCallback;
    private int mPaddingPixel;
    private boolean mIsSelectionMode = false;
    private int mColumnCount;
    private String scrollFileName = null;


    public ImagesAdapter(Context context, Document document) {

        mContext = context;

        mCallback = (ImagesAdapterCallback) context;
        mDocument = document;


        // Stores the checkbox states
        mSelections = new CountableBooleanArray();

        int paddingDp = 3;
        float density = context.getResources().getDisplayMetrics().density;
        mPaddingPixel = (int)(paddingDp * density);

        mColumnCount = 2;


    }

    public void setScrollFileName(String fileName) {
        scrollFileName = fileName;
    }

    public void setDocumentName(String documentName) {
        mDocumentName = documentName;
    }

    public void setSelectionMode(boolean isSelectionMode) {

        Log.d(CLASS_NAME, "setSelectionMode");
        boolean redraw = mIsSelectionMode != isSelectionMode;
        if (redraw) {
            mIsSelectionMode = isSelectionMode;
            notifyDataSetChanged();
        }

    }

    public void setColumnCount(int columnCount) {

        mColumnCount = columnCount;

    }

    public void updateImageView(String fileName) {

        if (mDocument == null || mDocument.getPages() == null || mDocument.getPages().isEmpty())
            return;

        int idx = 0;
        for (Page page : mDocument.getPages()) {
            if (page.getFile().getAbsolutePath().equalsIgnoreCase(fileName)) {
                notifyItemChanged(idx);
                return;
            }
            idx++;
        }

    }

    @Override
    public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the layout
        View photoView = inflater.inflate(R.layout.gallery_item, parent, false);
        mWidth = parent.getMeasuredWidth() / mColumnCount;
        GalleryViewHolder viewHolder = new GalleryViewHolder(photoView);

        return viewHolder;

    }

    @Override
    public void onBindViewHolder(GalleryViewHolder holder, int position) {

        if (mDocument == null || mDocument.getPages() == null)
            return;

        Page page = mDocument.getPages().get(position);
        if (page == null) {
            Helper.crashlyticsLog(CLASS_NAME, "onBindViewHolder", "page == null");
            return;
        }

        holder.mItemView.getLayoutParams().width = mWidth;

        double aspectRatio= aspectRatioForIndex(position);
        if (aspectRatio != 0)
            holder.mItemView.getLayoutParams().height = (int) Math.round(mWidth / aspectRatio);
        else
            holder.mItemView.getLayoutParams().height = mWidth;


//        Outer left item:
        if ((position % mColumnCount) == 0)
            holder.itemView.setPadding(0, mPaddingPixel, mPaddingPixel, mPaddingPixel);
//        Outer right item:
        else if ((position % mColumnCount) == (mColumnCount-1))
            holder.itemView.setPadding(mPaddingPixel, mPaddingPixel, 0, mPaddingPixel);
//        Middle item:
        else
            holder.itemView.setPadding(mPaddingPixel/2, mPaddingPixel, mPaddingPixel / 2,mPaddingPixel);

//        Show the image:
        initImageView(holder, position, page);

//      Set the title and init the OnClickListener:
        initCheckBox(holder, position, page);

    }

    private void initCheckBox(GalleryViewHolder holder, int position, Page page) {

        CheckBox checkBox = holder.mCheckBox;


        if (mSelections != null)
            checkBox.setChecked(mSelections.get(position, false));

//        not selected:
        if (!checkBox.isChecked()) {
            holder.mCheckBox.setVisibility(View.GONE);
            holder.mContainer.setBackgroundColor(holder.itemView.getContext().getResources().
                    getColor(R.color.white));

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.mImageView.getLayoutParams();
            lp.setMargins(0, 0, 0, 0);
        }
//        selected:
        else {
            holder.mCheckBox.setVisibility(View.VISIBLE);
            holder.mContainer.setBackgroundColor(holder.itemView.getContext().getResources().
                    getColor(R.color.colorSelectLight));

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.mImageView.getLayoutParams();
            float d = holder.itemView.getContext().getResources().getDisplayMetrics().density;
            int margin = (int)(-30 * d); // margin in pixels
            lp.setMargins(margin, margin, 0, 0);
        }


    }

    protected ArrayList<File> getSelectedFiles() {

        ArrayList<File> files = new ArrayList<>();
        int[] sel = getSelectionIndices();
        if (mDocument != null && mDocument.getPages() != null) {
            for (int idx : sel) {
                files.add(mDocument.getPages().get(idx).getFile());
            }
        }

        return files;

    }

    private synchronized void initImageView(GalleryViewHolder holder, final int position, Page page) {

        if (mDocument == null || mDocument.getPages() == null) {
            Helper.crashlyticsLog(CLASS_NAME, "initImageView",
                    "mDocument or mDocument.getPages == null ");
            return;
        }

        ImageView imageView = holder.mImageView;

        File file = mDocument.getPages().get(position).getFile();
        if (!file.exists())
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setTransitionName(file.getAbsolutePath());
        }

        final String fileName = file.getAbsolutePath();

        int exifOrientation = -1;
        boolean isCropped = false;
        long modified = file.lastModified();

        try {
            exifOrientation =  Helper.getExifOrientation(file);
            isCropped = PageDetector.isCropped(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (ImageProcessLogger.isAwaitingImageProcessing(file)) {

            holder.mCheckBox.setEnabled(false);

            if (mContext != null) {
                GlideApp.with(mContext)
                        .load(page.getFile().getPath())
                        //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                        .signature(new MediaStoreSignature("", modified, exifOrientation))
                        .into(imageView);
            }
            else
                Helper.crashlyticsLog(CLASS_NAME, "initImageView",
                        "mContext == null");
            holder.mProgressBar.setVisibility(View.VISIBLE);

            return;
        }
        else {
            holder.mProgressBar.setVisibility(View.INVISIBLE);
            holder.mCheckBox.setEnabled(true);
        }

        if (exifOrientation != -1) {
//            Draw the page detection border:
            if (!isCropped) {

                if (mContext != null) {
                    GlideApp.with(mContext)
                            .load(page.getFile().getPath())
                            //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                            .signature(new MediaStoreSignature("", modified, exifOrientation))
                            // TODO: enable disk caching!
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .transform(new CropRectTransform(fileName, mContext))
                            .override(400, 400)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    onImageLoadingDone(position, fileName);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    onImageLoadingDone(position, fileName);
                                    return false;                                }
                            })
                            .into(imageView);
                }
                else
                    Helper.crashlyticsLog(CLASS_NAME, "initImageView",
                            "mContext == null");
            }
//            Image is already cropped, draw no border:
            else {
                if (mContext != null) {
                    GlideApp.with(mContext)
                            .load(page.getFile().getPath())
                            //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                            .signature(new MediaStoreSignature("", modified, exifOrientation))
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    onImageLoadingDone(position, fileName);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    onImageLoadingDone(position, fileName);
                                    return false;
                                }
                            })
                            .into(imageView);
                }
                else
                    Helper.crashlyticsLog(CLASS_NAME, "initImageView",
                        "mContext == null");
            }
        }
        else {
//            Exif data contains no cropping information, simply show the image:
            if (mContext != null) {
                GlideApp.with(mContext)
                        .load(page.getFile().getPath())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                onImageLoadingDone(position, fileName);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                onImageLoadingDone(position, fileName);
                                return false;
                            }
                        })
                        .into(imageView);
            }
            else
                Helper.crashlyticsLog(CLASS_NAME, "initImageView",
                    "mContext == null");
        }

    }

    private void onImageLoadingDone(int position, String fileName) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (scrollFileName != null && fileName.equalsIgnoreCase(scrollFileName))
                mCallback.onScrollImageLoaded();
//            else if (position == 0)
//                mCallback.onImageLoaded();
        }

//        if (position == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//            mCallback.onImageLoaded();
    }

    @Override
    public int getItemCount() {

        if (mDocument == null || mDocument.getPages() == null)
            return 0;

        return mDocument.getPages().size();

    }

    public int[] getSelectionIndices() {

        int[] selectionIndices = new int[getSelectionCount()];
        int index = 0;

        if (mDocument != null && mDocument.getPages() != null) {
            for (int i = 0; i < mDocument.getPages().size(); i++) {
                if (mSelections != null && mSelections.get(i)) {
                    selectionIndices[index] = i;
                    index++;
                }
            }
        }
        else
            Helper.crashlyticsLog(CLASS_NAME, "getSelectionIndices",
                    "mDocument == null || mDocument.getPages() == null");

        return selectionIndices;

    }

    public int getSelectionCount() {

        if (mSelections == null)
            return -1;

        return mSelections.count();

    }

    public void selectAllItems() {

        setAllSelections(true);

    }

    public void deselectAllItems() {

        setAllSelections(false);
        mIsSelectionMode = false;

    }


    private void setAllSelections(boolean isSelected) {

        if (mDocument == null || mDocument.getPages() == null || mSelections == null)
            return;

        for (int i = 0; i < mDocument.getPages().size(); i++) {
            mSelections.put(i, isSelected);
        }

        //        We need to redraw the check boxes:
        this.notifyDataSetChanged();

//        if (mCallback != null)
//    //        We need to inform the parent activity that the selection has changed:
//            mCallback.onSelectionChange(mSelections.count());
//        else
//            Helper.crashlyticsLog(CLASS_NAME, "setAllSelections",
//                    "mCallback == null");

    }

    private void updateSelectionMode() {

        mIsSelectionMode = getSelectionCount() > 0;
        if (mCallback != null)
            mCallback.onSelectionChange(getSelectionCount());

    }


    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private ImageView mImageView;
        private View mItemView;
        private CheckBox mCheckBox;
        private View mContainer;
        private ProgressBar mProgressBar;

        public GalleryViewHolder(View itemView) {

            super(itemView);

            mItemView = itemView;
            mImageView = itemView.findViewById(R.id.page_imageview);
            mContainer = itemView.findViewById(R.id.page_container);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            mCheckBox = itemView.findViewById(R.id.page_checkbox);
            mProgressBar = itemView.findViewById(R.id.page_progressbar);

        }


        @Override
        public void onClick(View view) {

            int position = getAdapterPosition();

            if (!mIsSelectionMode) {

                if (position != RecyclerView.NO_POSITION) {

                    //                Tell the GalleryActivity that no file change has been done yet:
                    GalleryActivity.resetFileManipulation();

                    //                Start the image viewer:
                    Intent intent = new Intent(mContext, PageSlideActivity.class);
                    intent.putExtra(mContext.getString(R.string.key_document_file_name), mDocumentName);
                    Log.d(CLASS_NAME, "onClick: " + mDocumentName);
                    intent.putExtra(mContext.getString(R.string.key_page_position), position);
                    ((Activity) mContext).startActivityForResult(intent, LAUNCH_VIEWER_REQUEST);
//                    mContext.startActivity(intent);
                }
            }
            else {
                if (mSelections != null) {
                    mSelections.put(position, !mSelections.get(position, false));
                    mCheckBox.setChecked(mSelections.get(position, false));
                }
                updateSelectionMode();
//                if (getSelectionCount() == 0)
//                    notifyDataSetChanged();
//                else
                notifyItemChanged(position);
//                if (mCallback != null)
//                    mCallback.onSelectionChange(getSelectionCount());
            }
        }

        @Override
        public boolean onLongClick(View view) {

//            mIsSelectionMode = true;

            int position = getAdapterPosition();
            if (mSelections != null) {
                mSelections.put(position, !mSelections.get(position, false));
                mCheckBox.setChecked(mSelections.get(position, false));
            }
//            if (mCallback != null)
//                mCallback.onSelectionChange(getSelectionCount());

            updateSelectionMode();
            notifyDataSetChanged();

            return true;
        }
    }

    private double aspectRatioForIndex(int i) {

        if (mDocument == null || mDocument.getPages() == null || mDocument.getPages().size() <= i) {
            Helper.crashlyticsLog(CLASS_NAME, "aspectRatioForIndex",
                    "could not retrieve aspect ratio");
            return 1.0;
        }

        String fileName = mDocument.getPages().get(i).getFile().getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);
        int width = options.outWidth;
        int height = options.outHeight;

        try {
            int orientation = Helper.getExifOrientation(mDocument.getPages().get(i).getFile());
            int angle = Helper.getAngleFromExif(orientation);
            if ((angle == 90) || (angle == 270)) {
                int tmp = width;
                width = height;
                height = tmp;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double ratio = width / (double) height;
        return ratio;

    }


    public interface ImagesAdapterCallback {
        void onSelectionChange(int selectionCount);
        void onImageLoaded();
        void onScrollImageLoaded();
    }

    /**
     * Class that extends SparseBooleanArray with a function for counting the true elements.
     */
    private class CountableBooleanArray extends SparseBooleanArray {

        private int count() {

            if (mDocument == null || mDocument.getPages() == null)
                return 0;

            int sum = 0;

            for (int i = 0; i < mDocument.getPages().size(); i++) {
                if (this.get(i))
                    sum++;
            }

            return sum;

        }

    }


}
