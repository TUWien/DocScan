package at.ac.tuwien.caa.docscan.logic

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import at.ac.tuwien.caa.docscan.DocScanApp
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.glidemodule.CropRectTransform
import at.ac.tuwien.caa.docscan.glidemodule.GlideApp
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.signature.MediaStoreSignature
import com.bumptech.glide.signature.ObjectKey
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.io.File

/**
 * A helper utility class for glide for summarizing all loading functions and styles across the app.
 */
object GlideHelper {

    private val fileHandler: FileHandler by inject(FileHandler::class.java)
    private val app: DocScanApp by inject(DocScanApp::class.java)

    fun loadFileIntoImageView(
        file: File,
        rotation: Rotation,
        imageView: ImageView,
        style: GlideStyles,
        onResourceReady: (isFirstResource: Boolean) -> Unit = {},
        onResourceFailed: (isFirstResource: Boolean, e: GlideException?) -> Unit = { _, _ -> }
    ) {
        loadIntoView(
            app,
            imageView,
            null,
            null,
            file,
            PageFileType.JPEG,
            rotation,
            style,
            onResourceReady,
            onResourceFailed
        )
    }

    fun loadPageIntoImageView(
        page: Page?,
        imageView: ImageView,
        style: GlideStyles,
        callback: GlideLegacyCallback
    ) {
        loadPageIntoImageView(
            page,
            imageView,
            style,
            page?.fileHash,
            callback::onResourceReady,
            callback::onResourceFailed
        )
    }

    fun loadPageIntoImageView(
        page: Page?,
        imageView: ImageView,
        style: GlideStyles
    ) {
        loadPageIntoImageView(page, imageView, style, page?.fileHash, {}, { _, _ -> })
    }

    private fun loadPageIntoImageView(
        page: Page?,
        imageView: ImageView,
        style: GlideStyles,
        fileHash: String?,
        onResourceReady: (isFirstResource: Boolean) -> Unit = {},
        onResourceFailed: (isFirstResource: Boolean, e: GlideException?) -> Unit = { _, _ -> }
    ) {
        if (page != null) {
            val file = fileHandler.getFileByPage(page)
            if (file != null) {
                loadIntoView(
                    app,
                    imageView,
                    CropRectTransform(page, imageView.context),
                    fileHash,
                    file,
                    PageFileType.JPEG,
                    page.rotation,
                    style,
                    onResourceReady,
                    onResourceFailed
                )
                return
            }
            onResourceFailed.invoke(false, null)
        }

        Timber.w("Image file doesn't exist and cannot be shown with Glide!")
        // clear the image view in case that the file or even the page doesn't exist
        GlideApp.with(app).clear(imageView)
    }

    /**
     * Loads an image into a imageview with Glide.
     * @param fileHash if available, then this will be used as a key for caching, otherwise it fall
     * backs to [MediaStoreSignature].
     *
     * Please note, that any kind of manipulation of the [file] during an image load may lead to very
     * bad issues, where the app may crash and the file gets corrupted, always ensure that when this
     * function is called, no modifications to the [file] will happen.
     */
    private fun loadIntoView(
        context: Context,
        imageView: ImageView,
        transformation: BitmapTransformation?,
        fileHash: String?,
        file: File,
        @Suppress("SameParameterValue") fileType: PageFileType,
        rotation: Rotation,
        style: GlideStyles,
        onResourceReady: (isFirstResource: Boolean) -> Unit = {},
        onResourceFailed: (isFirstResource: Boolean, e: GlideException?) -> Unit = { _, _ -> }
    ) {

        // Needs to be added via factory to prevent issues with partially transparent images
        // see http://bumptech.github.io/glide/doc/transitions.html#cross-fading-with-placeholders-and-transparent-images
        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(false).build()
        val glideRequest = GlideApp.with(context)
            .load(file)
// disable caching temporarily to test
//            .skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .signature(
                if (fileHash != null) {
                    ObjectKey(fileHash)
                } else {
                    MediaStoreSignature(
                        fileType.mimeType,
                        file.lastModified(),
                        rotation.exifOrientation
                    )
                }
            )
            .listener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    onResourceFailed(isFirstResource, e)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any,
                    target: Target<Drawable?>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    onResourceReady(isFirstResource)
                    return false
                }
            })

        val glideTransformRequest = when (style) {
            GlideStyles.DEFAULT -> {
                glideRequest
            }
            GlideStyles.CAMERA_THUMBNAIL -> {
                glideRequest.transform(CircleCrop()).transition(withCrossFade(factory))
            }
            GlideStyles.DOCUMENT_PREVIEW -> {
                glideRequest.transform(
                    CenterCrop(),
                    RoundedCorners(context.resources.getDimensionPixelSize(R.dimen.document_preview_corner_radius))
                ).transition(withCrossFade(factory))
            }
            GlideStyles.IMAGE_CROPPED -> {
                glideRequest.transition(withCrossFade(factory))
            }
            GlideStyles.IMAGES_UNCROPPED -> {
                transformation?.let {
                    glideRequest.transform(it).transition(withCrossFade(factory))
                } ?: glideRequest.transition(withCrossFade(factory))
            }
        }

        glideTransformRequest.into(imageView)
    }

    enum class GlideStyles {
        DEFAULT,
        CAMERA_THUMBNAIL,
        DOCUMENT_PREVIEW,
        IMAGE_CROPPED,
        IMAGES_UNCROPPED
    }
}

interface GlideLegacyCallback {
    fun onResourceReady(isFirstResource: Boolean)
    fun onResourceFailed(isFirstResource: Boolean, e: GlideException?)
}
