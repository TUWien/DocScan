package at.ac.tuwien.caa.docscan.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp
import kotlinx.android.synthetic.main.sheet_action_list_item.view.*

class SheetAdapter(private val actionList: ArrayList<ActionSheet.SheetAction>,
                   private val listener: (ActionSheet.SheetAction) -> Unit) :
        RecyclerView.Adapter<SheetAdapter.ViewHolder>() {

    val CLASS_NAME = "SheetAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(layoutInflater.inflate(R.layout.sheet_action_list_item, parent, false))
    }

    override fun getItemCount(): Int {

        return actionList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

       val sheetAction = actionList[position]

        with (holder) {

            GlideApp.with(imageView)
                    .load(sheetAction.mIcon)
                    .into(imageView)
            textView.text = sheetAction.mText
            holder.itemView.setOnClickListener{
                listener(sheetAction)
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val textView: TextView = view.sheet_item_textview
        val imageView: ImageView = view.sheet_item_imageview

    }
}