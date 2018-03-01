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
import android.widget.GridLayout;
import android.widget.ImageView;

import com.bumptech.glide.signature.MediaStoreSignature;
import com.fivehundredpx.greedolayout.GreedoLayoutSizeCalculator;
import com.fivehundredpx.greedolayout.Size;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.io.IOException;
import java.util.Random;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity;

/**
 * Created by fabian on 2/6/2018.
 */

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>
        implements GreedoLayoutSizeCalculator.SizeCalculatorDelegate, ItemTouchHelperAdapter {

    private Document mDocument;
    private Context mContext;

    private String mFileName;
    private CountableBooleanArray mSelections;

    // Callback to listen to selection changes:
    private GalleryAdapterCallback mCallback;
    private GreedoLayoutSizeCalculator mSizeCalculator;

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

    public void setSizeCalculator(GreedoLayoutSizeCalculator sizeCalculator) {
        mSizeCalculator = sizeCalculator;;
    }

    @Override
    public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the layout
        View photoView = inflater.inflate(R.layout.gallery_item, parent, false);
        ViewGroup.LayoutParams p = photoView.getLayoutParams();

        GalleryViewHolder viewHolder = new GalleryViewHolder(photoView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(GalleryViewHolder holder, int position) {
//
//        GridLayout.LayoutParams params =
//                new GridLayout.LayoutParams(holder.mItemView.getLayoutParams());
//
//        params.rowSpec = GridLayout.spec(position / 2, 2);    // First cell in first row use rowSpan 2.
//        params.columnSpec = GridLayout.spec(0, 2); // First cell in first column use columnSpan 2.
////        itemView.setLayoutParams(params);

//        int r = getRandomIntInRange(200,0);



//        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.mItemView.getLayoutParams();
//
//        if (position % 4 < 2) { // first row
//            if (position % 2 == 1) // second column
//                p.leftMargin = -200;
//        }



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
        checkBox.setText(page.getTitle());
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

//        Set up the caching strategy: i.e. reload the image after the orientation has changed:
        int exifOrientation = -1;
        try {
            exifOrientation =  Helper.getExifOrientation(mDocument.getPages().get(position).getFile());

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (exifOrientation != -1) {
            GlideApp.with(mContext)
                    .load(page.getFile().getPath())
                    .signature(new MediaStoreSignature("", 0, exifOrientation))
                    .into(imageView);
        }
        else {
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

    @Override
    public void onItemMove(int fromPosition, int toPosition) {

    }

    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onDragStart() {

        mCallback.onDragStart();

    }

//    @Override
//    public double aspectRatioForIndex(int i) {
//
//////        return 0.5;
////        int v = i % 3;
////        if (v == 0)
////            return 2;
////        else if (v == 1)
////            return .5;
////        else
////            return 1;
//
//
//        if (mDocument.getPages().size() <= i)
//            return 1;
//
//        String fileName = mDocument.getPages().get(i).getFile().getAbsolutePath();
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(fileName, options);
//        int width = options.outWidth;
//        int height = options.outHeight;
//
//        try {
//            int orientation = Helper.getExifOrientation(mDocument.getPages().get(i).getFile());
//            int angle = Helper.getAngleFromExif(orientation);
//            if ((angle == 90) || (angle == 270)) {
//                int tmp = width;
//                width = height;
//                height = width;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        double ratio = width / (double) height;
//        return ratio;
//
//    }

    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mImageView;
        private View mItemView;
        private CheckBox mCheckBox;

        public GalleryViewHolder(View itemView) {

            super(itemView);

            mItemView = itemView;

//            int r = getRandomIntInRange(200,0);
//
//            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
//            p.leftMargin = -r;
            mImageView = itemView.findViewById(R.id.page_imageview);

//            ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
//            if (lp instanceof FlexboxLayoutManager.LayoutParams) {
//                FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
//                flexboxLp.setFlexGrow(1.0f);
//                flexboxLp.setAlignSelf(AlignSelf.FLEX_END);
//            }


//
//            flexboxLp.setFlexGrow(1.0f);

            itemView.setOnClickListener(this);
            mCheckBox = itemView.findViewById(R.id.page_checkbox);



        }

        // Custom method to get a random number between a range
        protected int getRandomIntInRange(int max, int min){
            return new Random().nextInt((max-min)+min)+min;
        }

        @Override
        public void onClick(View view) {

            int position = getAdapterPosition();
            if(position != RecyclerView.NO_POSITION) {
                Intent intent = new Intent(mContext, PageSlideActivity.class);

                intent.putExtra("DOCUMENT_FILE_NAME", mFileName);
                intent.putExtra("PAGE_POSITION", position);
                mContext.startActivity(intent);
            }
        }
    }

    @Override
    public double aspectRatioForIndex(int i) {

////        return 0.5;
//        int v = i % 3;
//        if (v == 0)
//            return 2;
//        else if (v == 1)
//            return .5;
//        else
//            return 1;


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
        void onDragStart();
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
