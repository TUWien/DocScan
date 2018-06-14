package at.ac.tuwien.caa.docscan.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.signature.MediaStoreSignature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.camera.threads.crop.CropLogger;
import at.ac.tuwien.caa.docscan.camera.threads.crop.PageDetector;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity;

/**
 * Created by fabian on 2/6/2018.
 */

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>
        implements GalleryLayoutSizeCalculator.SizeCalculatorDelegate {

    private Document mDocument;
    private Context mContext;

    private int mWidth;
    private String mFileName;
    private CountableBooleanArray mSelections;

    // Callback to listen to selection changes:
    private GalleryAdapterCallback mCallback;
    private int mPaddingPixel;
    private boolean mIsSelectionMode = false;
    private int mColumnCount;


    public GalleryAdapter(Context context, Document document) {

        mContext = context;

        mCallback = (GalleryAdapterCallback) context;
        mDocument = document;


        // Stores the checkbox states
        mSelections = new CountableBooleanArray();

        int paddingDp = 3;
        float density = mContext.getResources().getDisplayMetrics().density;
        mPaddingPixel = (int)(paddingDp * density);

        mColumnCount = 2;


    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public void setSelectionMode(boolean isSelectionMode) {

        mIsSelectionMode = isSelectionMode;

    }

    public void setColumnCount(int columnCount) {

        mColumnCount = columnCount;

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

        Page page = mDocument.getPages().get(position);

        holder.mItemView.getLayoutParams().width = mWidth;

        double aspectRatio= aspectRatioForIndex(position);
        if (aspectRatio != 0)
            holder.mItemView.getLayoutParams().height = (int) Math.round(mWidth / aspectRatio);
        else
            holder.mItemView.getLayoutParams().height = mWidth;


//        Outer left item:
        if ((position % mColumnCount) == 0)
            holder.itemView.setPadding(0, 0,mPaddingPixel,mPaddingPixel);
//        Outer right item:
        else if ((position % mColumnCount) == (mColumnCount-1))
            holder.itemView.setPadding(mPaddingPixel, 0,0,mPaddingPixel);
//        Middle item:
        else
            holder.itemView.setPadding(mPaddingPixel/2, 0,mPaddingPixel / 2,mPaddingPixel);


//        Show the image:
        initImageView(holder, position, page);


//      Set the title and init the OnClickListener:
        initCheckBox(holder, position, page);



//////        TODO: find out why this slows down the caching:
//        Size imageViewSize = mSizeCalculator.sizeForChildAtPosition(position);
//        holder.itemView.getLayoutParams().width = imageViewSize.getWidth();
//        holder.itemView.getLayoutParams().height = imageViewSize.getHeight();
//
//        holder.itemView.getLayoutParams().width = 400;
//        holder.itemView.getLayoutParams().height = 600;

    }

    private void initCheckBox(GalleryViewHolder holder, int position, Page page) {

        CheckBox checkBox = holder.mCheckBox;
//        checkBox.setText(page.getFile().getName());
//        checkBox.setText(page.getTitle());
        checkBox.setText("#: " + Integer.toString(position+1));

        final int pos = position;
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelections.put(pos, !mSelections.get(pos, false));
                ((CheckBox)v).setChecked(mSelections.get(pos, false));
                mCallback.onSelectionChange(mSelections.count());
            }
        });

        checkBox.setChecked(mSelections.get(position, false));

    }

    private void initImageView(GalleryViewHolder holder, int position, Page page) {

        ImageView imageView = holder.mImageView;

        File file = mDocument.getPages().get(position).getFile();
        String fileName = file.getAbsolutePath();

        int exifOrientation = -1;
        boolean isCropped = false;
        long modified = file.lastModified();

        try {
            exifOrientation =  Helper.getExifOrientation(file);
            isCropped = PageDetector.isCropped(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (CropLogger.isAwaitingMapping(file) || CropLogger.isAwaitingPageDetection(file)) {

            holder.mCheckBox.setEnabled(false);

            GlideApp.with(mContext)
                    .load(page.getFile().getPath())
                    //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                    .signature(new MediaStoreSignature("", modified, exifOrientation))
                    .into(imageView);

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

                float strokeWidth = mContext.getResources().getDimension(R.dimen.page_gallery_stroke_width);
                int strokeColor = mContext.getResources().getColor(R.color.hud_page_rect_color);

                GlideApp.with(mContext)
                        .load(page.getFile().getPath())
                        //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                        .signature(new MediaStoreSignature("", modified, exifOrientation))
                        // TODO: enable disk caching!
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .skipMemoryCache(true)
                        .transform(new CropRectTransform(fileName, strokeColor, strokeWidth))
                        .override(400,400)
                        .into(imageView);
            }
//            Image is already cropped, draw no border:
            else {
                GlideApp.with(mContext)
                        .load(page.getFile().getPath())
                        //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                        .signature(new MediaStoreSignature("", modified, exifOrientation))
                        .into(imageView);
            }
        }
        else {
//            Exif data contains no cropping information, simply show the image:
            GlideApp.with(mContext)
                    .load(page.getFile().getPath())
                    .into(imageView);
        }

    }

    @Override
    public int getItemCount() {

        return mDocument.getPages().size();

    }

    public int[] getSelectionIndices() {

        int[] selectionIndices = new int[getSelectionCount()];
        int index = 0;

        for (int i = 0; i < mDocument.getPages().size(); i++) {
            if (mSelections.get(i)) {
                selectionIndices[index] = i;
                index++;
            }
        }

        return selectionIndices;

    }

    public int getSelectionCount() {

        return mSelections.count();

    }

    public void selectAllItems() {

        setAllSelections(true);

    }

    public void deselectAllItems() {

        setAllSelections(false);

    }

    public void setSelections(ArrayList<Integer> indices) {

        for (int i = 0; i < mDocument.getPages().size(); i++) {
            mSelections.put(i, false);
            for (Integer idx : indices) {
                if (idx == i) {
                    mSelections.put(i, true);
                    break;
                }
            }
        }

//        We need to redraw the check boxes:
        this.notifyDataSetChanged();

//        We need to inform the parent activity that the selection has changed:
        mCallback.onSelectionChange(mSelections.count());


    }

    private void setAllSelections(boolean isSelected) {

        for (int i = 0; i < mDocument.getPages().size(); i++) {
            mSelections.put(i, isSelected);
        }

//        We need to redraw the check boxes:
        this.notifyDataSetChanged();

//        We need to inform the parent activity that the selection has changed:
        mCallback.onSelectionChange(mSelections.count());

    }

    public void clearSelection() {

        for (int i = 0; i < mDocument.getPages().size(); i++)
            mSelections.put(i, false);

//        We need to inform the parent activity that the selection has changed:
        mCallback.onSelectionChange(mSelections.count());

    }

    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mImageView;
        private View mItemView;
        private CheckBox mCheckBox;
        private ProgressBar mProgressBar;

        public GalleryViewHolder(View itemView) {

            super(itemView);

            mItemView = itemView;
            mImageView = itemView.findViewById(R.id.page_imageview);
            itemView.setOnClickListener(this);
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
                    intent.putExtra(mContext.getString(R.string.key_document_file_name), mFileName);
                    intent.putExtra(mContext.getString(R.string.key_page_position), position);
                    mContext.startActivity(intent);
                }
            }
            else {
                mSelections.put(position, !mSelections.get(position, false));
                mCheckBox.setChecked(mSelections.get(position, false));
                mCallback.onSelectionChange(getSelectionCount());
            }
        }
    }

//    Needed by the GreedoLayoutManager:
    @Override
    public double aspectRatioForIndex(int i) {

        if (mDocument.getPages().size() <= i)
            return 1.0;

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


    public interface GalleryAdapterCallback {
        void onSelectionChange(int selectionCount);
    }

    /**
     * Class that extends SparseBooleanArray with a function for counting the true elements.
     */
    private class CountableBooleanArray extends SparseBooleanArray {

        private int count() {

            int sum = 0;

            for (int i = 0; i < mDocument.getPages().size(); i++) {
                if (this.get(i))
                    sum++;
            }

            return sum;

        }

    }


}
