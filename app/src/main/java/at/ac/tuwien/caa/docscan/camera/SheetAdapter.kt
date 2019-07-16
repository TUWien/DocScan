package at.ac.tuwien.caa.docscan.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import kotlinx.android.synthetic.main.sheet_action_list_item.view.*

class SheetAdapter(private val mSheetActionList: ArrayList<CameraSheetDialog.SheetAction>) :
        RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.sheet_action_list_item, parent, false))
    }

    override fun getItemCount(): Int {

        return mSheetActionList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

       val sheetAction = mSheetActionList[position]

        with (holder) {

//            GlideApp.with(holder.mImageView)
//                    .load(sheetAction.mIcon)
//                    .into(holder.mImageView)

            mTextView.text = sheetAction.mText
        }

    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val mTextView = view.sheet_item_textview
        val mImageView = view.sheet_item_imageview
    }
}