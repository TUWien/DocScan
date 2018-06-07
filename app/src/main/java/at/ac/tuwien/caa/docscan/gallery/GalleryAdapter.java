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

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.fivehundredpx.greedolayout.GreedoLayoutSizeCalculator;
import com.fivehundredpx.greedolayout.Size;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import at.ac.tuwien.caa.docscan.R;
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

    private String mFileName;
    private CountableBooleanArray mSelections;

    // Callback to listen to selection changes:
    private GalleryAdapterCallback mCallback;
    private GalleryLayoutSizeCalculator mSizeCalculator;

    public GalleryAdapter(Context context, Document document) {

        mContext = context;

        mCallback = (GalleryAdapterCallback) context;
        mDocument = document;


        // Stores the checkbox states
        mSelections = new CountableBooleanArray();

    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public void setSizeCalculator(GalleryLayoutSizeCalculator sizeCalculator) {
        mSizeCalculator = sizeCalculator;
    }

    @Override
    public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the layout
        View photoView = inflater.inflate(R.layout.gallery_item, parent, false);

        GalleryViewHolder viewHolder = new GalleryViewHolder(photoView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(GalleryViewHolder holder, int position) {

        Page page = mDocument.getPages().get(position);

//        Show the image:
        initImageView(holder, position, page);

//      Set the title and init the OnClickListener:
        initCheckBox(holder, position, page);

        Size imageViewSize = mSizeCalculator.sizeForChildAtPosition(position);
        holder.itemView.getLayoutParams().width = imageViewSize.getWidth();
        holder.itemView.getLayoutParams().height = imageViewSize.getHeight();


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

        float strokeWidth = mContext.getResources().getDimension(R.dimen.page_stroke_width);
        int strokeColor = mContext.getResources().getColor(R.color.hud_page_rect_color);


        if (exifOrientation != -1) {
//            Draw the page detection border:
            if (!isCropped) {
                GlideApp.with(mContext)
                        .load(page.getFile().getPath())
                        //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                        .signature(new MediaStoreSignature("", file.lastModified(), exifOrientation))
                        // TODO: enable disk caching!
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .skipMemoryCache(true)
                        .transform(new CropRectTransform(fileName, strokeColor, strokeWidth))
                        .into(imageView);
            }
//            Image is already cropped, draw no border:
            else {
                GlideApp.with(mContext)
                        .load(page.getFile().getPath())
                        //        Set up the caching strategy: i.e. reload the image after the orientation has changed:
                        .signature(new MediaStoreSignature("", file.lastModified(), exifOrientation))
                        // TODO: enable disk caching!
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .skipMemoryCache(true)
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

        public GalleryViewHolder(View itemView) {

            super(itemView);

            mItemView = itemView;

            mImageView = itemView.findViewById(R.id.page_imageview);
            itemView.setOnClickListener(this);

            mCheckBox = itemView.findViewById(R.id.page_checkbox);

        }


        @Override
        public void onClick(View view) {

            int position = getAdapterPosition();
            if(position != RecyclerView.NO_POSITION) {

//                Tell the GalleryActivity that no file change has been done yet:
                GalleryActivity.resetFileManipulation();

//                Start the image viewer:
                Intent intent = new Intent(mContext, PageSlideActivity.class);
                intent.putExtra(mContext.getString(R.string.key_document_file_name), mFileName);
                intent.putExtra(mContext.getString(R.string.key_page_position), position);
                mContext.startActivity(intent);
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
