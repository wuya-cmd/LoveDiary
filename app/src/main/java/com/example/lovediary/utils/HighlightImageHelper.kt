package com.example.lovediary.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.UUID

/**
 * 精选图片存储助手
 * 负责将用户选择的图片压缩后存入应用私有目录 highlights/
 */
object HighlightImageHelper {
    private const val TAG = "HighlightImageHelper"
    private const val DIR_NAME = "highlights"

    /**
     * 保存精选图片到本地
     * @param context 上下文
     * @param uri 图片URI
     * @return 保存后的本地文件路径，失败返回 null
     */
    fun saveHighlightImage(context: Context, uri: Uri): String? {
        val imageId = UUID.randomUUID().toString()
        val imageDir = File(context.filesDir, DIR_NAME)
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        val outputFile = File(imageDir, "$imageId.jpg")
        val success = ImageCompressor.compressImage(context, uri, outputFile)

        return if (success) {
            outputFile.absolutePath
        } else {
            Log.e(TAG, "精选图片压缩保存失败: $uri")
            null
        }
    }

    /**
     * 删除精选图片文件
     * @param imagePath 图片本地路径
     */
    fun deleteHighlightImage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "精选图片文件删除失败: $imagePath")
        }
    }
}
