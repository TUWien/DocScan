package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.widget.ImageView
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.signature.MediaStoreSignature
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.io.File

/**
 * A helper utility class for glide.
 */
object GlideHelper {

    private val fileHandler: FileHandler by inject(FileHandler::class.java)
    private val app: DocScanApp by inject(DocScanApp::class.java)

    fun loadPageIntoImageView(page: Page?, imageView: ImageView, style: GlideStyles) {
        if (page != null) {
            val file = fileHandler.getFileByPage(page)
            if (file != null) {
                loadIntoView(
                    app,
                    imageView,
                    file,
                    FileType.JPEG,
                    page.rotation,
                    style
                )
                return
            }
        }

        Timber.w("Image file doesn't exist and cannot be shown with Glide!")
        // clear the image view in case that the file or even the page doesn't exist
        GlideApp.with(app).clear(imageView)
    }

    private fun loadIntoView(
        context: Context,
        imageView: ImageView,
        file: File,
        @Suppress("SameParameterValue") fileType: FileType,
        rotation: Rotation,
        style: GlideStyles
    ) {
        // TODO: add a cross fade as default, looks quite nice
        val glideRequest = GlideApp.with(context)
            .load(file)
            .signature(
                MediaStoreSignature(
                    fileType.mimeType,
                    file.lastModified(),
                    rotation.exifOrientation
                )
            )

        val glideTransformRequest = when (style) {
            GlideStyles.CAMERA_THUMBNAIL -> {
                glideRequest.transform(CircleCrop())
            }
            GlideStyles.DOCUMENT_PREVIEW -> {
                glideRequest.transform(
                    CenterCrop(),
                    RoundedCorners(context.resources.getDimensionPixelSize(R.dimen.document_preview_corner_radius))
                )
            }
        }

        glideTransformRequest.into(imageView)
    }

    enum class GlideStyles {
        CAMERA_THUMBNAIL,
        DOCUMENT_PREVIEW
    }
}
