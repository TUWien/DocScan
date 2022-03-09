package at.ac.tuwien.caa.docscan.export

import android.content.Context
import android.net.Uri
import at.ac.tuwien.caa.docscan.db.model.error.IOErrorCode
import at.ac.tuwien.caa.docscan.db.model.exif.Rotation
import at.ac.tuwien.caa.docscan.extensions.await
import at.ac.tuwien.caa.docscan.logic.Resource
import at.ac.tuwien.caa.docscan.logic.Success
import at.ac.tuwien.caa.docscan.logic.asFailure
import at.ac.tuwien.caa.docscan.logic.calculateImageResolution
import at.ac.tuwien.caa.docscan.ui.crop.ImageMeta
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.Text.TextBlock
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

object PdfCreator {

    suspend fun analyzeFileWithOCR(context: Context, uri: Uri): Resource<Text> {
        @Suppress("BlockingMethodInNonBlockingContext")
        val image = InputImage.fromFilePath(context, uri)
        val task = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
        return task.await()
    }

    // suppressed, since this is expected and mitigated by making this function cooperative
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun savePDF(
        context: Context,
        outputUri: Uri,
        files: List<FileWrapper>,
        ocrResults: List<Text>? = null
    ): Resource<Unit> {
        context.contentResolver.openOutputStream(outputUri, "rw").use { outputStream ->
            return withContext(Dispatchers.IO) {
                try {
                    val first = files[0]
                    var resolution = calculateImageResolution(first.file, first.rotation)
                    val landscapeFirst = isLandscape(resolution)
                    val firstPageSize = getPageSize(resolution, landscapeFirst)
                    val document = Document(firstPageSize, 0F, 0F, 0F, 0F)

                    val writer = PdfWriter.getInstance(document, outputStream)
                    document.open()
                    for (i in files.indices) {

                        // check if the coroutine is still active, if not, close the document and throw a CancellationException
                        if (!isActive) {
                            document.close()
                            throw CancellationException()
                        }

                        var file = files[i]
                        val rotationInDegrees = file.rotation.angle

                        //add the original image to the pdf and set the DPI of it to 600
                        val image = Image.getInstance(file.file.absolutePath)
                        image.setRotationDegrees(-rotationInDegrees.toFloat())
                        if (rotationInDegrees == 0 || rotationInDegrees == 180) image.scaleAbsolute(
                            document.pageSize.width,
                            document.pageSize.height
                        ) else image.scaleAbsolute(
                            document.pageSize.height,
                            document.pageSize.width
                        )
                        image.setDpi(600, 600)
                        document.add(image)
                        if (ocrResults != null) {
                            // the direct content where we write on
                            // directContentUnder instead of directContent, because then the text is in the background)
                            //PdfContentByte cb = writer.getDirectContentUnder();
                            val cb = writer.directContentUnder
                            val bf = BaseFont.createFont()

                            //sort the result based on the y-Axis so that the markup order is correct
                            val sortedBlocks = sortBlocks(ocrResults[i])
                            resolution = calculateImageResolution(file.file, file.rotation)

                            //int j = 0;
                            for (column in sortedBlocks) {
                                for (line in sortLinesInColumn(column)) {
                                    // one FirebaseVisionText.Line corresponds to one line
                                    // the rectangle we want to draw this line corresponds to the lines boundingBox
                                    val boundingBox = line.boundingBox ?: continue
                                    val left =
                                        boundingBox.left.toFloat() / resolution.width.toFloat() * document.pageSize.width
                                    val right =
                                        boundingBox.right.toFloat() / resolution.width.toFloat() * document.pageSize.width
                                    val top =
                                        boundingBox.top.toFloat() / resolution.height.toFloat() * document.pageSize.height
                                    val bottom =
                                        boundingBox.bottom.toFloat() / resolution.height.toFloat() * document.pageSize.height
                                    val rect = Rectangle(
                                        left,
                                        document.pageSize.height - bottom,
                                        right,
                                        document.pageSize.height - top
                                    )
                                    val drawText = line.text
                                    // try to get max font size that fit in rectangle
                                    val textHeightInGlyphSpace =
                                        bf.getAscent(drawText) - bf.getDescent(drawText)
                                    var fontSize = 1000f * rect.height / textHeightInGlyphSpace
                                    while (bf.getWidthPoint(drawText, fontSize) < rect.width) {
                                        fontSize++
                                    }
                                    while (bf.getWidthPoint(drawText, fontSize) > rect.width) {
                                        fontSize -= 0.1f
                                    }
                                    val phrase = Phrase(drawText, Font(bf, fontSize))
                                    // write the text on the pdf
                                    ColumnText.showTextAligned(
                                        cb, Element.ALIGN_CENTER, phrase,  // center horizontally
                                        (rect.left + rect.right) / 2,  // shift baseline based on descent
                                        rect.bottom - bf.getDescentPoint(drawText, fontSize), 0f
                                    )
                                }
                            }
                        }
                        if (i < files.size - 1) {
                            file = files[i + 1]
                            val pageSize = getPageSize(
                                calculateImageResolution(file.file, file.rotation),
                                landscapeFirst
                            )
                            document.pageSize = pageSize
                            document.newPage()
                        }
                    }
                    document.close()
                    return@withContext Success(Unit)
                } catch (e: Exception) {
                    // if this has happened because of a cancellation, then this needs to be re-thrown
                    if (e is CancellationException) {
                        throw e
                    } else {
                        return@withContext IOErrorCode.EXPORT_CREATE_PDF_FAILED.asFailure(e)
                    }
                }
            }
        }
    }

    private fun isLandscape(size: ImageMeta): Boolean {
        return size.width > size.height
    }

    private fun getPageSize(size: ImageMeta, landscape: Boolean): Rectangle {
        val pageSize: Rectangle = if (landscape) {
            val height = PageSize.A4.height / size.width * size.height
            Rectangle(PageSize.A4.height, height)
        } else {
            val height = PageSize.A4.width / size.width * size.height
            Rectangle(PageSize.A4.width, height)
        }
        return pageSize
    }

    private fun sortBlocks(ocrResult: Text?): List<MutableList<TextBlock>> {
        val sortedBlocks: MutableList<MutableList<TextBlock>> = ArrayList()
        val biggestBlocks: MutableList<TextBlock> = ArrayList()
        val blocksSortedByWidth = sortByWidth(ocrResult!!.textBlocks)
        for (block in blocksSortedByWidth) {
            if (block.boundingBox == null) continue
            if (sortedBlocks.isEmpty()) {
                val blocks: MutableList<TextBlock> = ArrayList()
                blocks.add(block)
                biggestBlocks.add(block)
                sortedBlocks.add(blocks)
            } else {
                var added = false
                for (checkBlock in biggestBlocks) {
                    if (checkBlock.boundingBox == null) continue
                    if (block.boundingBox!!.centerX() > checkBlock.boundingBox!!.left &&
                        block.boundingBox!!.centerX() < checkBlock.boundingBox!!.right
                    ) {
                        sortedBlocks[biggestBlocks.indexOf(checkBlock)].add(block)
                        if (block.boundingBox!!.width() > checkBlock.boundingBox!!.width()) {
                            biggestBlocks[biggestBlocks.indexOf(checkBlock)] = block
                        }
                        added = true
                        break
                    }
                }
                if (!added) {
                    val blocks: MutableList<TextBlock> = ArrayList()
                    blocks.add(block)
                    var i = 0
                    while (i < biggestBlocks.size) {
                        if (biggestBlocks[i].boundingBox == null ||
                            block.boundingBox!!.centerX() > biggestBlocks[i].boundingBox!!.centerX()
                        ) {
                            i++
                        } else {
                            break
                        }
                    }
                    biggestBlocks.add(i, block)
                    sortedBlocks.add(i, blocks)
                }
            }
        }
        for (textBlocks in sortedBlocks) {
            sortedBlocks[sortedBlocks.indexOf(textBlocks)] = textBlocks
        }
        return sortedBlocks
    }

    private fun sortByWidth(result: List<TextBlock>): List<TextBlock> {
        val sortedBlocks: MutableList<TextBlock> = ArrayList()
        for (textBlock in result) {
            if (textBlock.boundingBox == null) continue
            if (sortedBlocks.isEmpty()) {
                sortedBlocks.add(textBlock)
            } else {
                var i = 0
                while (i < sortedBlocks.size) {
                    if (sortedBlocks[i].boundingBox == null ||
                        textBlock.boundingBox!!.width() < sortedBlocks[i].boundingBox!!.width()
                    ) {
                        i++
                    } else {
                        break
                    }
                }
                sortedBlocks.add(i, textBlock)
            }
        }
        return sortedBlocks
    }

    private fun sortLinesInColumn(result: List<TextBlock>): List<Text.Line> {
        val sortedLines: MutableList<Text.Line> = ArrayList()
        for (textBlock in result) {
            for (line in textBlock.lines)  //                if (line.getCornerPoints() == null || line.getCornerPoints().length == 0)
            //                    continue;
                if (sortedLines.isEmpty()) {
                    sortedLines.add(line)
                } else {
                    var i = 0
                    while (i < sortedLines.size) {
                        if (line.cornerPoints!![0].y > sortedLines[i].cornerPoints!![0].y) {
                            i++
                        } else {
                            break
                        }
                    }
                    sortedLines.add(i, line)
                }
        }
        return sortedLines
    }

    data class FileWrapper(val file: File, val rotation: Rotation)

}