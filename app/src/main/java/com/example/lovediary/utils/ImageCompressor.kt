package com.example.lovediary.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * 图片压缩工具类
 */
object ImageCompressor {
    private const val MAX_WIDTH = 1080
    private const val MAX_HEIGHT = 1920
    private const val MAX_FILE_SIZE = 2 * 1024 * 1024 // 2MB

    /**
     * 压缩图片
     * @param context 上下文
     * @param uri 图片URI
     * @param outputFile 输出文件
     * @return 压缩是否成功
     */
    fun compressImage(context: Context, uri: Uri, outputFile: File): Boolean {
        return try {
            // 读取图片
            val bitmap = decodeSampledBitmapFromUri(context, uri, MAX_WIDTH, MAX_HEIGHT)
                ?: return false

            // 修正图片方向
            val rotatedBitmap = rotateBitmap(bitmap, getOrientation(context, uri))

            // 压缩并保存
            compressAndSaveImage(rotatedBitmap, outputFile)
        } catch (e: Exception) {
            Log.e("ImageCompressor", "压缩图片失败", e)
            false
        }
    }

    /**
     * 从URI解码图片
     */
    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // 第一次解码，获取图片尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // 计算采样率
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // 第二次解码，获取压缩后的图片
            options.inJustDecodeBounds = false
            val newInputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, options)
            newInputStream.close()

            bitmap
        } catch (e: Exception) {
            Log.e("ImageCompressor", "解码图片失败", e)
            null
        }
    }

    /**
     * 计算采样率
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 获取图片方向
     */
    private fun getOrientation(context: Context, uri: Uri): Int {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return 0
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            inputStream.close()
            orientation
        } catch (e: IOException) {
            Log.e("ImageCompressor", "获取图片方向失败", e)
            0
        }
    }

    /**
     * 旋转图片
     */
    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 压缩并保存图片
     */
    private fun compressAndSaveImage(bitmap: Bitmap, outputFile: File): Boolean {
        var quality = 90
        var success = false

        while (!success && quality > 0) {
            try {
                FileOutputStream(outputFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                }

                if (outputFile.length() <= MAX_FILE_SIZE) {
                    success = true
                } else {
                    quality -= 10
                }
            } catch (e: Exception) {
                Log.e("ImageCompressor", "保存图片失败", e)
                break
            }
        }

        return success
    }
}
