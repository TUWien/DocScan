package at.ac.tuwien.caa.docscan.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.signature.MediaStoreSignature;
import com.fivehundredpx.greedolayout.GreedoLayoutSizeCalculator;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.io.IOException;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;
import at.ac.tuwien.caa.docscan.ui.gallery.PageSlideActivity;

/**
 * Created by fabian on 2/13/2018.
 */

public class GalleryTestAdapter extends RecyclerView.Adapter<GalleryTestAdapter.GalleryViewHolder>
        implements GreedoLayoutSizeCalculator.SizeCalculatorDelegate {

    private Document mDocument;
    private Context mContext;

    private String mFileName;
//    private GalleryAdapter.CountableBooleanArray mSelections;

    // Callback to listen to selection changes:
//    private GalleryAdapter.GalleryAdapterCallback mCallback;

    public GalleryTestAdapter(Context context, Document document) {

        mContext = context;
        mDocument = document;

    }

    @Override
    public GalleryTestAdapter.GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the layout
        View photoView = inflater.inflate(R.layout.gallery_test_item, parent, false);

        GalleryTestAdapter.GalleryViewHolder viewHolder = new GalleryTestAdapter.GalleryViewHolder(photoView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(GalleryViewHolder holder, int position) {


        Page page = mDocument.getPages().get(position);

//        Show the image:
        initImageView(holder, position, page);

//      Set the title and init the OnClickListener:
//        initCheckBox(holder, position, page);

    }

    public void setFileName(String fileName) {
        mFileName = fileName;
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

    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mImageView;
//        private CheckBox mCheckBox;

        public GalleryViewHolder(View itemView) {

            super(itemView);
            mImageView = itemView.findViewById(R.id.page_imageview);
            itemView.setOnClickListener(this);
//            mCheckBox = itemView.findViewById(R.id.page_checkbox);

            ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
            if (lp instanceof FlexboxLayoutManager.LayoutParams) {
                FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
                flexboxLp.setFlexGrow(1.0f);
            }


        }

        @Override
        public void onClick(View view) {

            int position = getAdapterPosition();
            if(position != RecyclerView.NO_POSITION) {
                Intent intent = new Intent(mContext, PageSlideActivity.class);

                intent.putExtra(mContext.getString(R.string.key_document_file_name), mFileName);
                intent.putExtra(mContext.getString(R.string.key_page_position), position);
                mContext.startActivity(intent);
            }
        }
    }

}
