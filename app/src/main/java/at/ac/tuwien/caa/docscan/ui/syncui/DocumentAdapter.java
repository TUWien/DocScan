package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.ui.gallery.GalleryActivity;

/**
 * Created by fabian on 4/5/2018.
 */

public class DocumentAdapter extends ArrayAdapter<Document> {

    protected Context mContext;
    private List<Document> mDocuments;
    private DocumentAdapterCallback mCallback;

    public DocumentAdapter(@NonNull Context context, int resource, @NonNull List<Document> documents) {
        super(context, resource, documents);

        mContext = context;
        mDocuments = documents;
        mCallback = (DocumentAdapterCallback) context;

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


        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.layout_listview_row, null);

//            convertView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mCallback.onSelectionChange();
//                }
//            });

        }


        TextView titleTextView = convertView.findViewById(R.id.layout_listview_row_title);
        if (titleTextView != null && mDocuments != null) {
            String text = mDocuments.get(position).getTitle();
            titleTextView.setText(text);
        }

        TextView descriptionTextView = convertView.findViewById(R.id.layout_listview_row_description);
        if (descriptionTextView != null && mDocuments != null) {
            int num = mDocuments.get(position).getPages().size();
            String desc = mContext.getResources().getString(R.string.sync_pages_text);
            desc += " " + Integer.toString(num);
            descriptionTextView.setText(desc);
        }


//        Load the first image in the document into the thumbnail preview:
        ImageView thumbNail = convertView.findViewById(R.id.layout_listview_row_thumbnail);
        if (thumbNail != null && mDocuments != null) {

            if (mDocuments.get(position).getPages().size() >= 1) {
                Log.d(getClass().getName(), "position: " + position);
                final File file = mDocuments.get(position).getPages().get(0).getFile();
                GlideApp.with(mContext)
                        .load(file.getAbsolutePath())
                        .into(thumbNail);

                thumbNail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(mContext, GalleryActivity.class);
                        intent.putExtra("DOCUMENT_FILE_NAME", file.getParent());
                        mContext.startActivity(intent);
                    }
                });
            }
            else {
                thumbNail.setImageResource(R.drawable.ic_folder_open_black_24dp);
                thumbNail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
            }
        }

        return convertView;

    }


    public interface DocumentAdapterCallback {
        void onSelectionChange();
    }


}
