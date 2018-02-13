package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.MediaStoreSignature;

import java.io.IOException;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.gallery.ItemTouchHelperAdapter;
import at.ac.tuwien.caa.docscan.gallery.PageSlideActivity;
import at.ac.tuwien.caa.docscan.gallery.SimpleItemTouchHelperCallback;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;

/**
 * Created by fabian on 2/6/2018.
 */

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>
    implements ItemTouchHelperAdapter {

    private Document mDocument;
    private Context mContext;

    private String mFileName;
    private CountableBooleanArray mSelections;

    // Callback to listen to selection changes:
    private GalleryAdapterCallback mCallback;

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

    }

    private void initCheckBox(GalleryViewHolder holder, int position, Page page) {
        CheckBox checkBox = holder.mCheckBox;
        checkBox.setText(page.getFile().getName());
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

    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mImageView;
        private CheckBox mCheckBox;

        public GalleryViewHolder(View itemView) {

            super(itemView);
            mImageView = itemView.findViewById(R.id.page_imageview);
            itemView.setOnClickListener(this);
            mCheckBox = itemView.findViewById(R.id.page_checkbox);

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
