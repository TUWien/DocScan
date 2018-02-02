package at.ac.tuwien.caa.docscan.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import at.ac.tuwien.caa.docscan.R;

/**
 * Created by fabian on 09.12.2016.
 */
public class ShootModeAdapter  extends ArrayAdapter<String> {

    private Context mContext;
    private Integer[] mIcons;
    private String[] mTexts;

    public ShootModeAdapter(Context context, int textViewResourceId, String[] texts, Integer[] icons) {

        super(context, textViewResourceId, texts);
        mContext = context;
        mIcons = icons;
        mTexts = texts;

    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        //return super.getView(position, convertView, parent);

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View row = inflater.inflate(R.layout.spinner_row, parent, false);
        TextView label = (TextView) row.findViewById(R.id.spinner_row_textview);
        label.setText(mTexts[position]);

        ImageView imageView = (ImageView) row.findViewById(R.id.spinner_row_imageview);

//        We need setImageResource instead of setBackgroundResource for vector drawables on pre-lollipop devices:
        imageView.setImageResource(mIcons[position]);
//        imageView.setBackgroundResource(mIcons[position]);

//        if (DayOfWeek[position]=="Sunday"){
//            icon.setImageResource(R.drawable.icon);
//        }
//        else{
//            icon.setImageResource(R.drawable.icongray);
//        }

        return row;
    }
}
