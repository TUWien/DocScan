package at.ac.tuwien.caa.docscan.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import at.ac.tuwien.caa.docscan.R;
import at.ac.tuwien.caa.docscan.logic.Document;
import at.ac.tuwien.caa.docscan.logic.Helper;
import at.ac.tuwien.caa.docscan.logic.Page;

/**
 * Created by fabian on 01.02.2018.
 */

public class DocumentViewActivity extends BaseNoNavigationActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_document_view);

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 2);
        RecyclerView recyclerView = findViewById(R.id.images_recyclerview);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

//        dummy document - start
        String fileName = getIntent().getStringExtra("DOCUMENT_FILE_NAME");

        Document document = new Document();
        ArrayList<File> fileList = getFileList(fileName);
        ArrayList<Page> pages = filesToPages(fileList);
        document.setPages(pages);
        File file = new File(fileName);
        document.setTitle(file.getName());
//        dummy document - end

        initToolbarTitle(document.getTitle());

        ImageGalleryAdapter adapter = new ImageGalleryAdapter(this, document);
        recyclerView.setAdapter(adapter);
    }

//    TODO: temporary helper methods copied from BaseDocumentAdapter. Replace them.

    private ArrayList<File> getFileList(String dir) {

        File[] files = getFiles(new File(dir));

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private ArrayList<File> getFileList(Context context) {

        File mediaStorageDir = Helper.getMediaStorageDir(context.getResources().getString(R.string.app_name));

        if (mediaStorageDir == null)
            return null;

        FileFilter directoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        File[] dirs = mediaStorageDir.listFiles(directoryFilter);
        File[] files = getFiles(dirs[27]);

        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));

        return fileList;

    }

    private ArrayList<Page> filesToPages(ArrayList<File> files) {

        ArrayList<Page> pages = new ArrayList<>(files.size());

        for (File file : files) {
            pages.add(new Page(file));
        }

        return pages;

    }

    private File[] getFiles(File dir) {

        FileFilter filesFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory();
            }
        };
        File[] files = dir.listFiles(filesFilter);
        Arrays.sort(files);

        return files;
    }


    private class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.MyViewHolder>  {

        private Document mDocument;

        public ImageGalleryAdapter(Context context, Document document) {

            mContext = context;
            mDocument = document;

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
            ImageView imageView = holder.mImageView;

            Glide.with(mContext)
                    .load(page.getFile().getPath())
                    .into(imageView);
        }

        @Override
        public int getItemCount() {
            return mDocument.getPages().size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            public ImageView mImageView;

            public MyViewHolder(View itemView) {

                super(itemView);
                mImageView = itemView.findViewById(R.id.page_imageview);
                itemView.setOnClickListener(this);

            }

            @Override
            public void onClick(View view) {

//                int position = getAdapterPosition();
//                if(position != RecyclerView.NO_POSITION) {
//                    SpacePhoto spacePhoto = mSpacePhotos[position];
//
//                    Intent intent = new Intent(mContext, SpacePhotoActivity.class);
//                    intent.putExtra(SpacePhotoActivity.EXTRA_SPACE_PHOTO, spacePhoto);
//                    startActivity(intent);
//                }
            }
        }

//        private SpacePhoto[] mSpacePhotos;
        private Context mContext;


    }
}
