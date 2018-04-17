package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;

/**
 * Created by fabian on 4/5/2018.
 */

public class DocumentUploadAdapter extends DocumentAdapter {

    protected Context mContext;
    private List<Document> mDocuments;
    private DocumentAdapterCallback mCallback;

    public DocumentUploadAdapter(@NonNull Context context, int resource, @NonNull List<Document> documents) {
        super(context, resource, documents);

        mContext = context;
        mDocuments = documents;
//        mCallback = (DocumentAdapterCallback) context;

    }


//    public DocumentAdapter(@NonNull Context context, int resource) {
//        super(context, resource);
//
//        mContext = context;
//
//        fillList();
//    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        convertView = super.getView(position, convertView, parent);

//        if (convertView == null) {
//            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            convertView = inflater.inflate(R.layout.layout_listview_row, null);
//
////            convertView.setOnClickListener(new View.OnClickListener() {
////                @Override
////                public void onClick(View v) {
////                    mCallback.onSelectionChange();
////                }
////            });
//
//        }
//
        Document document = mDocuments.get(position);
        if (document == null)
            return convertView;

//        TextView titleTextView = convertView.findViewById(R.id.layout_listview_row_title);
//        if (titleTextView != null) {
//            String text = document.getTitle();
//            titleTextView.setText(text);
//        }
//
//        TextView descriptionTextView = convertView.findViewById(R.id.layout_listview_row_description);
//        if (descriptionTextView != null) {
//            int num = document.getPages().size();
//            String desc = mContext.getResources().getString(R.string.sync_pages_text);
//            desc += " " + Integer.toString(num);
//            descriptionTextView.setText(desc);
//        }




//        Show the upload status in the icon:
        ImageView iconView = convertView.findViewById(R.id.layout_listview_row_icon);
        if (iconView != null) {
            if (document.isUploaded())
                iconView.setImageResource(R.drawable.ic_cloud_done_black_24dp);
            else if (document.isAwaitingUpload()) {
                iconView.setImageResource(R.drawable.ic_cloud_upload_black_24dp);
                TextView textView = convertView.findViewById(R.id.layout_listview_row_description);
//                Write that the upload is pending:
                if (textView != null)
                    textView.append(" " + mContext.getResources().getString(R.string.sync_dir_pending_text));
////                Show the animation:
//                ProgressBar progressBar = convertView.findViewById(R.id.layout_listview_row_icon_progressbar);
//                if (progressBar != null)
//                    progressBar.setVisibility(View.VISIBLE);
            }
            else
                iconView.setImageResource(R.drawable.ic_cloud_queue_black_24dp);

        }

        return convertView;

    }


}
