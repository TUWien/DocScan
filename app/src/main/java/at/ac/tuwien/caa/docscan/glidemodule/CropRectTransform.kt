package at.ac.tuwien.caa.docscan.glidemodule

import android.content.Context
import android.graphics.*
import at.ac.tuwien.caa.docscan.R
import at.ac.tuwien.caa.docscan.db.model.Page
import at.ac.tuwien.caa.docscan.logic.KtHelper
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class CropRectTransform(private val page: Page, context: Context) : BitmapTransformation() {

    companion object {
        private const val ID = "at.ac.tuwien.caa.docscan.gallery.CropRectTransformNew"
        private val ID_BYTES = ID.toByteArray(StandardCharsets.UTF_8)
    }

    private val mQuadPaint: Paint = Paint()
    private val mQuadPath: Path = Path()

    init {
        mQuadPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = context.resources.getDimension(R.dimen.page_gallery_stroke_width)
            color = context.resources.getColor(R.color.hud_page_rect_color)
            isAntiAlias = true
        }
    }

    override fun transform(
        bitmapPool: BitmapPool,
        original: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        val result = bitmapPool[original.width, original.height, Bitmap.Config.ARGB_8888]
        // If no matching Bitmap is in the pool, get will return null, so we should allocate.

        // Create a Canvas backed by the result Bitmap.
        val canvas = Canvas(result)
        val paint = Paint()
        val resized = Bitmap.createScaledBitmap(original, original.width, original.height, true)
        // Draw the original Bitmap onto the result Bitmap with a transformation:
        canvas.drawBitmap(resized, 0f, 0f, paint)
        val pfResult = KtHelper.getScaledCropPoints(page, original.width, original.height)
        drawQuad(canvas, pfResult, mQuadPath, mQuadPaint)

        return result
    }

    private fun drawQuad(canvas: Canvas, points: List<PointF>, path: Path?, paint: Paint?) {

//        initOuterValues(canvas);
        path!!.reset()
        var isStartSet = false
        for (point in points) {
            if (!isStartSet) {
                path.moveTo(point.x, point.y)
                isStartSet = true
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        path.close()
        canvas.drawPath(path, paint!!)
    }

    override fun equals(other: Any?): Boolean {
        if (other is CropRectTransform) {
            return other.page.singlePageBoundary == this.page.singlePageBoundary
        }
        return false
    }

    override fun hashCode(): Int {
        return page.singlePageBoundary.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
        messageDigest.update(page.id.toString().toByteArray(StandardCharsets.UTF_8))
        messageDigest.update(this.hashCode().toString().toByteArray(StandardCharsets.UTF_8))
    }
}
