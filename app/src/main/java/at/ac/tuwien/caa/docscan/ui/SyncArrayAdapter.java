package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.sync.SyncInfo;

import static at.ac.tuwien.caa.docscan.sync.SyncInfo.FileSync.STATE_NOT_UPLOADED;
import static at.ac.tuwien.caa.docscan.sync.SyncInfo.FileSync.STATE_UPLOADED;

/**
 * Created by fabian on 22.09.2017.
 */

public class SyncArrayAdapter extends ArrayAdapter<String> {

    private Context mContext;
    private ArrayList<SyncInfo.FileSync> mFileSyncList;
    private ArrayList<String> mList;

    private static SyncArrayAdapter mInstance = null;



    public static SyncArrayAdapter getInstance() {

        return mInstance;

    }

    public static void setInstance(SyncArrayAdapter instance) {

        mInstance = instance;

    }

    public SyncArrayAdapter(Context context, ArrayList<SyncInfo.FileSync> fileSyncList) {

        super(context, R.layout.rowlayout);
        mContext = context;
//        mList = fileSyncList;
        mFileSyncList = fileSyncList;

    }

    @Override
    public int getCount(){
        return mFileSyncList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        final ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

        textView.setText(mFileSyncList.get(position).getFile().getName());

        switch (mFileSyncList.get(position).getState()) {
            case STATE_NOT_UPLOADED:
                imageView.setImageResource(R.drawable.ic_cloud_off_black_24dp);
                break;
            case STATE_UPLOADED:
                imageView.setImageResource(R.drawable.ic_cloud_done_gray_24dp);
                break;
        }


        return rowView;
    }


}
