package at.ac.tuwien.caa.docscan.ui.docviewer

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    fun setTitle(title: String) {
        if (isAdded && !isRemoving) {
            requireActivity().title = title
        }
    }
}