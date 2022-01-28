package at.ac.tuwien.caa.docscan.logic

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize
import java.util.*

@Keep
@Parcelize
data class DocumentPage(val docId: UUID, val pageId: UUID?) : Parcelable
