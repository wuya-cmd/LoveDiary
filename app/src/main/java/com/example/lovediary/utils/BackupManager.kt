package com.example.lovediary.utils

import android.content.Context
import android.util.Base64
import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.data.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 备份管理器
 * 处理日记的导出和导入功能
 */
class BackupManager(
    private val context: Context,
    private val diaryRepository: DiaryRepository
) {
    /**
     * 备份配置
     */
    data class BackupConfig(
        val includeImages: Boolean = true, // 是否包含图片
        val compressImages: Boolean = true, // 是否压缩图片
        val compressionQuality: Int = 85 // 图片压缩质量
    )

    /**
     * 导出日记为JSON文件
     * @param config 备份配置
     * @return 导出的文件路径
     */
    suspend fun exportDiaries(config: BackupConfig = BackupConfig()): String {
        return withContext(Dispatchers.IO) {
            // 获取所有日记（原始数据，不带隐私过滤）
            val diaries = diaryRepository.getAllDiariesRaw()
            
            // 创建JSON对象
            val jsonObject = JSONObject()
            jsonObject.put("version", "1.0")
            jsonObject.put("exportTime", Date().toISOString())
            jsonObject.put("totalCount", diaries.size)
            
            // 创建日记数组
            val diariesArray = JSONArray()
            
            // 处理每个日记
            for (diary in diaries) {
                val diaryJson = JSONObject()
                diaryJson.put("id", diary.id)
                diaryJson.put("content", diary.content)
                diaryJson.put("createTime", diary.createTime)
                diaryJson.put("updateTime", diary.updateTime)
                diaryJson.put("privacyLevel", diary.privacyLevel)
                diaryJson.put("category", diary.category)
                diaryJson.put("tags", diary.tags)
                
                // 处理图片
                val imagesArray = JSONArray()
                if (config.includeImages) {
                    val images = diaryRepository.getImagesByDiaryId(diary.id)
                    for (image in images) {
                        val imageJson = JSONObject()
                        imageJson.put("fileName", File(image.imagePath).name)
                        imageJson.put("originalPath", image.originalPath)
                        imageJson.put("compressed", image.compressed)
                        imageJson.put("compressionQuality", image.compressionQuality)
                        imageJson.put("originalSize", image.originalSize)
                        imageJson.put("compressedSize", image.compressedSize)
                        imageJson.put("format", image.format)
                        
                        // 将图片转换为Base64
                        val base64Data = convertImageToBase64(image.imagePath)
                        imageJson.put("base64Data", base64Data)
                        
                        imagesArray.put(imageJson)
                    }
                }
                diaryJson.put("images", imagesArray)
                diariesArray.put(diaryJson)
            }
            
            jsonObject.put("diaries", diariesArray)
            
            // 创建输出文件
            val outputFile = createBackupFile()
            val outputStream = FileOutputStream(outputFile)
            outputStream.write(jsonObject.toString(2).toByteArray())
            outputStream.flush()
            outputStream.close()
            
            return@withContext outputFile.absolutePath
        }
    }

    /**
     * 从JSON文件导入日记
     * @param filePath JSON文件路径
     * @return 导入结果
     */
    suspend fun importDiaries(filePath: String): ImportResult {
        return withContext(Dispatchers.IO) {
            // 读取JSON文件
            val inputFile = File(filePath)
            val inputStream = FileInputStream(inputFile)
            val jsonString = inputStream.readBytes().toString(Charsets.UTF_8)
            inputStream.close()
            
            // 解析JSON
            val jsonObject = JSONObject(jsonString)
            val diariesArray = jsonObject.getJSONArray("diaries")
            
            var successCount = 0
            var failedCount = 0
            var imageCount = 0
            
            // 处理每个日记
            for (i in 0 until diariesArray.length()) {
                try {
                    val diaryJson = diariesArray.getJSONObject(i)
                    
                    // 创建日记对象
                    val diary = Diary(
                        id = diaryJson.getString("id"),
                        content = diaryJson.getString("content"),
                        createTime = diaryJson.getString("createTime"),
                        updateTime = diaryJson.getString("updateTime"),
                        privacyLevel = diaryJson.getInt("privacyLevel"),
                        category = diaryJson.optString("category", "默认分类"),
                        tags = diaryJson.optString("tags", "")
                    )
                    
                    // 检查日记是否已存在
                    val existingDiary = diaryRepository.getDiaryById(diary.id)
                    if (existingDiary != null) {
                        // 日记已存在，跳过
                        failedCount++
                        continue
                    }
                    
                    // 添加日记
                    diaryRepository.addDiary(diary)
                    
                    // 处理图片
                    val imagesArray = diaryJson.getJSONArray("images")
                    for (j in 0 until imagesArray.length()) {
                        val imageJson = imagesArray.getJSONObject(j)
                        
                        // 获取Base64数据
                        val base64Data = imageJson.getString("base64Data")
                        
                        // 将Base64转换为图片文件
                        val imagePath = convertBase64ToImage(base64Data, imageJson.getString("fileName"))
                        
                        // 创建图片对象
                        val diaryImage = DiaryImage(
                            id = "image_${System.currentTimeMillis()}_${j}",
                            diaryId = diary.id,
                            imagePath = imagePath,
                            originalPath = imageJson.optString("originalPath", null),
                            compressed = imageJson.optBoolean("compressed", false),
                            compressionQuality = imageJson.optInt("compressionQuality", 85),
                            originalSize = imageJson.optLong("originalSize", 0),
                            compressedSize = imageJson.optLong("compressedSize", 0),
                            format = imageJson.optString("format", "JPEG")
                        )
                        
                        // 添加图片
                        diaryRepository.addImageToDiary(diary.id, diaryImage)
                        imageCount++
                    }
                    
                    successCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                    failedCount++
                }
            }
            
            return@withContext ImportResult(
                successCount = successCount,
                failedCount = failedCount,
                totalCount = diariesArray.length(),
                imageCount = imageCount
            )
        }
    }

    /**
     * 将图片转换为Base64
     * @param imagePath 图片路径
     * @return Base64编码的图片数据
     */
    private fun convertImageToBase64(imagePath: String): String {
        val file = File(imagePath)
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 将Base64转换为图片文件
     * @param base64Data Base64编码的图片数据
     * @param fileName 文件名
     * @return 图片路径
     */
    private fun convertBase64ToImage(base64Data: String, fileName: String): String {
        val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
        val outputDir = File(context.getExternalFilesDir(null), "imported_images")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, fileName)
        val outputStream = FileOutputStream(outputFile)
        outputStream.write(bytes)
        outputStream.flush()
        outputStream.close()
        return outputFile.absolutePath
    }

    /**
     * 创建备份文件
     * @return 备份文件
     */
    private fun createBackupFile(): File {
        val outputDir = File(context.getExternalFilesDir(null), "backups")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "diary_backup_${timestamp}.json"
        return File(outputDir, fileName)
    }

    /**
     * 导入结果
     */
    data class ImportResult(
        val successCount: Int, // 成功导入的日记数量
        val failedCount: Int, // 导入失败的日记数量
        val totalCount: Int, // 总日记数量
        val imageCount: Int // 导入的图片数量
    )

    /**
     * 将Date转换为ISO格式字符串
     */
    private fun Date.toISOString(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(this)
    }
}
