package at.ac.tuwien.caa.docscan.ui.syncui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.crop.CropInfo;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.ui.CropViewActivity;

import static at.ac.tuwien.caa.docscan.crop.CropInfo.CROP_INFO_NAME;

/**
 * Created by fabian on 02.10.2017.
 */

public abstract class BaseDocumentAdapter extends BaseExpandableListAdapter {


    protected Context mContext;
    protected ArrayList<File> mDirs;
    protected ArrayList<File[]> mFiles;


    public BaseDocumentAdapter(Context context) {

        super();
        mContext = context;

//        fillLists();

    }

    public File getGroupFile(int position) {

        return mDirs.get(position);
//        return mDirs[position];

    }

    protected void fillLists() {

        File mediaStorageDir = Helper.getMediaStorageDir(mContext.getResources().getString(R.string.app_name));

        if (mediaStorageDir == null)
            return;

        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        File[] dirs = mediaStorageDir.listFiles(directoryFilter);
        mDirs = new ArrayList<>(Arrays.asList(dirs));

//        TODO: sort!
        java.util.Collections.sort(mDirs, new FileComparator());
        mFiles = new ArrayList<>(mDirs.size());
        for (File dir : mDirs) {
            mFiles.add(getFiles(dir));
        }

    }

    class FileComparator implements Comparator<File>
    {
        @Override public int compare(File file1, File file2)
        {

            return file1.getName().compareToIgnoreCase(file2.getName());

        }
    }


//    private void fillLists() {
//
//        File mediaStorageDir = Helper.getMediaStorageDir(mContext.getResources().getString(R.string.app_name));
//
//        if (mediaStorageDir == null)
//            return;
//
//        FileFilter directoryFilter = new FileFilter() {
//            public boolean accept(File file) {
//                return file.isDirectory();
//            }
//        };
//
//        mDirs = mediaStorageDir.listFiles(directoryFilter);
//        Arrays.sort(mDirs);
//        mFiles = new ArrayList<>(mDirs.length);
//        int i = 0;
//        for (File dir : mDirs) {
//            mFiles.add(getFiles(dir));
//            i++;
//        }
//
//
//
//    }

    public static File[] getFiles(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        Arrays.sort(files);

        return files;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        final File file = (File) getChild(groupPosition, childPosition);

        final String childText = file.getName();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(childText);

//        GLIDE TRYOUT
//         Glide test:
        final ImageView imageView;
        imageView = convertView.findViewById(R.id.document_list_image_view);
        // TODO: look at this for the reason why we cannot use GlideApp
        // https://github.com/bumptech/glide/issues/1966
        Glide.with(mContext)
                .load(((File) getChild(groupPosition, childPosition)).getPath())
                .into(imageView);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, CropViewActivity.class);
                ArrayList<PointF> cropPoints = new ArrayList<>();
                cropPoints.add(new PointF(0,0));
                cropPoints.add(new PointF(0,0));
                cropPoints.add(new PointF(0,0));
                cropPoints.add(new PointF(0,0));

                CropInfo r = new CropInfo(cropPoints, file.getAbsolutePath());
                intent.putExtra(CROP_INFO_NAME, r);
                mContext.startActivity(intent);
            }
        });


//        GlideApp
//                .with(myFragment)
//                .load(url)
//                .centerCrop()
//                .placeholder(R.drawable.loading_spinner)
//                .into(myImageView);



        return convertView;
    }



    @Override
    public int getGroupCount() {
        return mDirs.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mFiles.get(groupPosition).length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mDirs.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mFiles.get(groupPosition)[childPosition];
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }


}
