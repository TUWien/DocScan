package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.gallery.PageSlideActivity;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Page;

/**
 * Created by fabian on 2/6/2018.
 */

public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.MyViewHolder>  {

    private Document mDocument;
    private Context mContext;

    private String mFileName;
    private SparseBooleanArray mSelections;

    public ImageGalleryAdapter(Context context, Document document) {

        mContext = context;
        mDocument = document;

        // Stores the checkbox states
        mSelections = new SparseBooleanArray();

    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    @Override
    public ImageGalleryAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the layout
        View photoView = inflater.inflate(R.layout.page_list_item, parent, false);

        ImageGalleryAdapter.MyViewHolder viewHolder = new ImageGalleryAdapter.MyViewHolder(photoView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ImageGalleryAdapter.MyViewHolder holder, int position) {


        Page page = mDocument.getPages().get(position);

//        Show the image:
        ImageView imageView = holder.mImageView;
        Glide.with(mContext)
                .load(page.getFile().getPath())
                .into(imageView);

        CheckBox checkBox = holder.mCheckBox;
        final int pos = position;
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelections.put(pos, !mSelections.get(pos, false));
                ((CheckBox)v).setChecked(mSelections.get(pos, false));
            }
        });

        checkBox.setChecked(mSelections.get(position, false));

    }

    @Override
    public int getItemCount() {
        return mDocument.getPages().size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {



        private ImageView mImageView;
        private CheckBox mCheckBox;

        public MyViewHolder(View itemView) {

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


}
