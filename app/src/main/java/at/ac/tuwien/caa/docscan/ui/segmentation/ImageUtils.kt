/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.ac.tuwien.caa.docscan.ui.segmentation

import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Collection of image reading and manipulation utilities in the form of static functions.
 */
abstract class ImageUtils {
    companion object {

        /**
         * Helper function used to convert an EXIF orientation enum into a transformation matrix
         * that can be applied to a bitmap.
         *
         * @param orientation - One of the constants from [ExifInterface]
         */
        private fun decodeExifOrientation(orientation: Int): Matrix {
            val matrix = Matrix()

            // Apply transformation corresponding to declared EXIF orientation
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED -> Unit
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1F, 1F)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1F, -1F)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postScale(-1F, 1F)
                    matrix.postRotate(270F)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postScale(-1F, 1F)
                    matrix.postRotate(90F)
                }

                // Error out if the EXIF orientation is invalid
                else -> throw IllegalArgumentException("Invalid orientation: $orientation")
            }

            // Return the resulting matrix
            return matrix
        }

        /**
         * sets the Exif orientation of an image.
         * this method is used to fix the exit of pictures taken by the camera
         *
         * @param filePath - The image file to change
         * @param value - the orientation of the file
         */
        fun setExifOrientation(
            filePath: String,
            value: String
        ) {
            val exif = ExifInterface(filePath)
            exif.setAttribute(
                ExifInterface.TAG_ORIENTATION, value
            )
            exif.saveAttributes()
        }

        /** Transforms rotation and mirroring information into one of the [ExifInterface] constants */
        fun computeExifOrientation(rotationDegrees: Int, mirrored: Boolean) = when {
            rotationDegrees == 0 && !mirrored -> ExifInterface.ORIENTATION_NORMAL
            rotationDegrees == 0 && mirrored -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
            rotationDegrees == 180 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_180
            rotationDegrees == 180 && mirrored -> ExifInterface.ORIENTATION_FLIP_VERTICAL
            rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
            rotationDegrees == 90 && !mirrored -> ExifInterface.ORIENTATION_ROTATE_90
            rotationDegrees == 90 && mirrored -> ExifInterface.ORIENTATION_TRANSPOSE
            rotationDegrees == 270 && mirrored -> ExifInterface.ORIENTATION_ROTATE_270
            rotationDegrees == 270 && !mirrored -> ExifInterface.ORIENTATION_TRANSVERSE
            else -> ExifInterface.ORIENTATION_UNDEFINED
        }

        /**
         * Decode a bitmap from a file and apply the transformations described in its EXIF data
         *
         * @param file - The image file to be read using [BitmapFactory.decodeFile]
         */
        fun decodeBitmap(file: File): Bitmap {
            // First, decode EXIF data and retrieve transformation matrix
            val exif = ExifInterface(file.absolutePath)
            val transformation =
                decodeExifOrientation(
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90
                    )
                )

            // Read bitmap using factory methods, and transform it using EXIF data
            val options = BitmapFactory.Options()
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            return Bitmap.createBitmap(
                BitmapFactory.decodeFile(file.absolutePath),
                0, 0, bitmap.width, bitmap.height, transformation, true
            )
        }

        fun scaleBitmapAndKeepRatio(
            targetBmp: Bitmap,
            reqHeightInPixels: Int,
            reqWidthInPixels: Int
        ): Bitmap {
            if (targetBmp.height == reqHeightInPixels && targetBmp.width == reqWidthInPixels) {
                return targetBmp
            }
            val matrix = Matrix()
            matrix.setRectToRect(
                RectF(
                    0f, 0f,
                    targetBmp.height.toFloat(),
                    targetBmp.width.toFloat()
                ),
                RectF(
                    0f, 0f,
                    reqHeightInPixels.toFloat(),
                    reqWidthInPixels.toFloat()
                ),
                Matrix.ScaleToFit.CENTER
            )

            val scaled = Bitmap.createBitmap(
                targetBmp,
                0,
                0,
                targetBmp.width,
                targetBmp.height,
                matrix,
                true
            )

            val background =
                Bitmap.createBitmap(reqWidthInPixels, reqHeightInPixels, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(background)
            // apply black background
            canvas.drawColor(Color.BLACK);
//            val newScaled = if(scaled.width > reqWidthInPixels){
//                Bitmap.createScaledBitmap(scaled)
//            }else if(scaled.height > reqHeightInPixels){
//
//            }

            var left = 0F
            var top = 0F

            if (scaled.height < background.height) {
                left = 0F
                top = (background.height / 2F) - (scaled.height / 2F)
            }

            if (scaled.width < background.width) {
                left = (background.width / 2F) - (scaled.width / 2F)
                top = 0F
            }

            canvas.drawBitmap(scaled, left, top, null)


            return background
        }

        /**
         * Pre-Conditions:
         * - width and height of [bitmap] both equals to [size]
         *
         * @return a byte buffer holding the quantized (uint8) image of [bitmap]
         */
        fun bitmapToByteBuffer(
            bitmap: Bitmap,
            size: Int
        ): ByteBuffer {
            // alloc
            val inputImage = ByteBuffer.allocateDirect(1 * size * size * 3)
            inputImage.order(ByteOrder.nativeOrder())
            inputImage.rewind()

            val intValues = IntArray(size * size)
            bitmap.getPixels(intValues, 0, size, 0, 0, size, size)
            var pixel = 0
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val value = intValues[pixel++]

                    inputImage.put((value shr 16 and 0xFF).toByte())
                    inputImage.put((value shr 8 and 0xFF).toByte())
                    inputImage.put((value and 0xFF).toByte())
                }
            }

            inputImage.rewind()
            return inputImage
        }

        fun convertByteBufferMaskToBitmap(
            tensorOutputBufferuint32: ByteBuffer,
            size: Int,
            colors: IntArray
        ): Bitmap {
            val maskBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            tensorOutputBufferuint32.rewind()
            // loop through all pixels
            for (y in 0 until size) {
                for (x in 0 until size) {
                    // get class prediction for value
                    val value = tensorOutputBufferuint32.getInt((y * size + x) * 4)
                    val color = colors[value]
                    maskBitmap.setPixel(x, y, color)
                }
            }
            return maskBitmap
        }
    }
}
