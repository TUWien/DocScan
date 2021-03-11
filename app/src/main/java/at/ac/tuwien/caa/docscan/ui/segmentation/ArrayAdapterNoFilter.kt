package at.ac.tuwien.caa.docscan.ui.segmentation

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * A simple workaround for the auto-complete textview in order to preserve the list while
 * performing a onrotation.
 *
 * @author matejbart
 */
class ArrayAdapterNoFilter<T>(context: Context, resource: Int, objects: List<T>) :
    ArrayAdapter<T>(
        context,
        resource,
        objects
    ) {

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(
                charSequence: CharSequence?,
                filterResults: FilterResults
            ) {
                if (this@ArrayAdapterNoFilter.count > 0) {
//                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                return FilterResults()
            }
        }
    }
}
