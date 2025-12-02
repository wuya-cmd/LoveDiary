package com.example.lovediary.utils

import android.content.Context
import android.net.Uri
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import java.io.File
import java.util.UUID

/**
 * 日记图片助手类
 */
object DiaryImageHelper {
    
    /**
     * 保存日记图片
     * @param context 上下文
     * @param viewModel 日记视图模型
     * @param diaryId 日记ID
     * @param imageUris 图片URI列表
     */
    fun saveDiaryImages(
        context: Context,
        viewModel: DiaryViewModel,
        diaryId: String,
        imageUris: List<Uri>
    ) {
        val images = mutableListOf<DiaryImage>()
        
        for (uri in imageUris) {
            val imageId = UUID.randomUUID().toString()
            val imageFile = File(context.filesDir, "images")
            if (!imageFile.exists()) {
                imageFile.mkdirs()
            }
            
            val outputFile = File(imageFile, "$imageId.jpg")
            val success = ImageCompressor.compressImage(context, uri, outputFile)
            
            if (success) {
                val diaryImage = DiaryImage(
                    id = imageId,
                    diaryId = diaryId,
                    imagePath = outputFile.absolutePath
                )
                images.add(diaryImage)
            }
        }
        
        if (images.isNotEmpty()) {
            viewModel.addImagesToDiary(diaryId, images)
        }
    }
}