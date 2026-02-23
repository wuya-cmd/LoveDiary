package com.example.lovediary.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.data.repository.DiaryRepository
import com.example.lovediary.security.PrivacyLevels
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
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
    companion object {
        // 相识日期的键名
        const val START_DATE_KEY = "anniversary_date"
    }
    
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
            try {
                // 获取所有日记（原始数据，不带隐私过滤）
                val diaries = diaryRepository.getAllDiariesRaw()
                
                // 获取相识日期
                val sharedPreferences = context.getSharedPreferences("love_diary_prefs", Context.MODE_PRIVATE)
                val anniversaryDate = sharedPreferences.getString(START_DATE_KEY, null)
                
                // 创建输出文件
                val outputFile = createBackupFile()
                val outputStream = FileOutputStream(outputFile)
                
                // 流式写入JSON，避免OOM
                outputStream.write("{".toByteArray())
                
                // 写入元数据
                outputStream.write("\"version\":\"1.2\",".toByteArray())
                outputStream.write("\"exportTime\":\"${Date().toISOString()}\",".toByteArray())
                outputStream.write("\"totalCount\":${diaries.size},".toByteArray())
                
                // 添加相识日期
                if (anniversaryDate != null) {
                    outputStream.write("\"$START_DATE_KEY\":\"$anniversaryDate\",".toByteArray())
                }
                
                // 写入日记数组开始
                outputStream.write("\"diaries\":[".toByteArray())
                
                // 逐个处理日记并写入
                for ((index, diary) in diaries.withIndex()) {
                    if (index > 0) {
                        outputStream.write(",".toByteArray())
                    }
                    
                    outputStream.write("{".toByteArray())
                    outputStream.write("\"id\":\"${diary.id}\",".toByteArray())
                    outputStream.write("\"content\":\"${escapeJsonString(diary.content)}\",".toByteArray())
                    outputStream.write("\"createTime\":\"${diary.createTime}\",".toByteArray())
                    outputStream.write("\"updateTime\":\"${diary.updateTime}\",".toByteArray())
                    outputStream.write("\"privacyLevel\":${diary.privacyLevel},".toByteArray())
                    outputStream.write("\"category\":\"${escapeJsonString(diary.category)}\",".toByteArray())
                    outputStream.write("\"tags\":\"${escapeJsonString(diary.tags)}\",".toByteArray())
                    
                    // 处理图片
                    outputStream.write("\"images\":[".toByteArray())
                    if (config.includeImages) {
                        val images = diaryRepository.getImagesByDiaryId(diary.id)
                        for ((imgIndex, image) in images.withIndex()) {
                            try {
                                // 检查文件大小，避免过大文件导致OOM
                                val imageFile = File(image.imagePath)
                                val fileSize = imageFile.length()
                                val maxSize = 10 * 1024 * 1024 // 10MB限制
                                
                                if (fileSize > maxSize) {
                                    Log.w("BackupManager", "Image file too large to export: ${image.imagePath} ($fileSize bytes)")
                                    continue
                                }
                                
                                if (imgIndex > 0) {
                                    outputStream.write(",".toByteArray())
                                }
                                
                                outputStream.write("{".toByteArray())
                                outputStream.write("\"fileName\":\"${File(image.imagePath).name}\",".toByteArray())
                                outputStream.write("\"originalPath\":\"${escapeJsonString(image.originalPath ?: "")}\",".toByteArray())
                                outputStream.write("\"compressed\":${image.compressed},".toByteArray())
                                outputStream.write("\"compressionQuality\":${image.compressionQuality},".toByteArray())
                                outputStream.write("\"originalSize\":${image.originalSize},".toByteArray())
                                outputStream.write("\"compressedSize\":${image.compressedSize},".toByteArray())
                                outputStream.write("\"format\":\"${escapeJsonString(image.format)}\",".toByteArray())
                                
                                // 将图片转换为Base64
                                val base64Data = convertImageToBase64(image.imagePath)
                                if (base64Data.isNotEmpty()) {
                                    outputStream.write("\"base64Data\":\"${escapeJsonString(base64Data)}\"".toByteArray())
                                }
                                outputStream.write("}".toByteArray())
                            } catch (e: OutOfMemoryError) {
                                Log.e("BackupManager", "Out of memory when processing image: ${image.imagePath}", e)
                                // Continue with other images
                            } catch (e: Exception) {
                                Log.e("BackupManager", "Error processing image: ${e.message}", e)
                                // Continue with other images
                            }
                        }
                    }
                    outputStream.write("]".toByteArray()) // 结束images数组
                    
                    outputStream.write("}".toByteArray()) // 结束diary对象
                }
                
                outputStream.write("]}".toByteArray()) // 结束diaries数组和根对象
                
                outputStream.flush()
                outputStream.close()
                
                // 保存到 Downloads 目录
                val publicPath = saveToDownloads(outputFile, outputFile.name)
                
                return@withContext publicPath.ifEmpty { outputFile.absolutePath }
            } catch (e: OutOfMemoryError) {
                Log.e("BackupManager", "Out of memory during export", e)
                throw e
            } catch (e: Exception) {
                Log.e("BackupManager", "导出日记时发生错误", e)
                throw e
            }
        }
    }

    /**
     * 从JSON文件导入日记
     * @param filePath JSON文件路径
     * @return 导入结果
     */
    suspend fun importDiaries(filePath: String): ImportResult {
        return withContext(Dispatchers.IO) {
            Log.d("BackupManager", "Starting streaming import from file: $filePath")
            
            var successCount = 0
            var failedCount = 0
            var imageCount = 0
            var totalCount = 0
            
            try {
                // 使用Jackson进行流式JSON解析
                val jsonFactory = JsonFactory()
                val file = File(filePath)
                Log.d("BackupManager", "File size: ${file.length()} bytes")
                
                FileInputStream(file).use { fis ->
                    val parser = jsonFactory.createParser(fis)
                    
                    // 确保我们从对象开始解析
                    if (parser.nextToken() != JsonToken.START_OBJECT) {
                        throw IllegalStateException("Expected start of object")
                    }
                    
                    // 解析顶层字段
                    while (parser.nextToken() != JsonToken.END_OBJECT) {
                        val fieldName = parser.currentName()
                        parser.nextToken() // 移动到字段值
                        
                        when (fieldName) {
                            START_DATE_KEY -> {
                                val anniversaryDate = parser.valueAsString
                                val sharedPreferences = context.getSharedPreferences("love_diary_prefs", Context.MODE_PRIVATE)
                                sharedPreferences.edit().putString(START_DATE_KEY, anniversaryDate).apply()
                                Log.d("BackupManager", "Anniversary date imported: $anniversaryDate")
                            }
                            "totalCount" -> {
                                totalCount = parser.intValue
                                Log.d("BackupManager", "Total count reported: $totalCount")
                            }
                            "diaries" -> {
                                // 处理日记数组
                                if (parser.currentToken() == JsonToken.START_ARRAY) {
                                    successCount = processDiariesArray(parser, imageCount)
                                }
                            }
                            else -> {
                                // 跳过其他字段
                                parser.skipChildren()
                            }
                        }
                    }
                }
                
                Log.d("BackupManager", "Streaming import completed. Success: $successCount, Failed: $failedCount, Total: $totalCount, Images: $imageCount")
            } catch (e: OutOfMemoryError) {
                Log.e("BackupManager", "Out of memory during streaming import", e)
                throw e
            } catch (e: Exception) {
                Log.e("BackupManager", "导入日记时发生错误", e)
                throw e
            }
            
            return@withContext ImportResult(
                successCount = successCount,
                failedCount = failedCount,
                totalCount = totalCount,
                imageCount = imageCount
            )
        }
    }
    
    /**
     * 处理日记数组
     */
    private suspend fun processDiariesArray(parser: JsonParser, imageCount: Int): Int {
        var successCount = 0
        var batchCount = 0
        
        // 处理数组中的每个日记对象
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() == JsonToken.START_OBJECT) {
                try {
                    val diaryData = parseDiaryObject(parser)
                    if (diaryData != null) {
                        val (diary, images) = diaryData
                        
                        // 处理日记
                        val processedDiary = diaryRepository.privacyManagerPublic.processDiaryForSave(diary)
                        
                        // 检查日记是否已存在
                        val existingDiary = diaryRepository.getDiaryById(diary.id)
                        if (existingDiary != null) {
                            // 日记已存在，跳过
                            Log.d("BackupManager", "Diary with id ${diary.id} already exists, skipping")
                        } else {
                            // 添加日记
                            diaryRepository.addDiary(processedDiary)
                            Log.d("BackupManager", "Successfully imported diary with id: ${diary.id}")
                            successCount++
                            
                            // 处理图片
                            for (imageData in images) {
                                try {
                                    // 将Base64转换为图片文件
                                    val imagePath = convertBase64ToImage(imageData.base64Data, imageData.fileName)
                                    
                                    // 创建图片对象
                                    val diaryImage = DiaryImage(
                                        id = "image_${System.currentTimeMillis()}_${imageCount}",
                                        diaryId = processedDiary.id,
                                        imagePath = imagePath,
                                        originalPath = imageData.originalPath,
                                        compressed = imageData.compressed,
                                        compressionQuality = imageData.compressionQuality,
                                        originalSize = imageData.originalSize,
                                        compressedSize = imageData.compressedSize,
                                        format = imageData.format
                                    )
                                    
                                    // 添加图片
                                    diaryRepository.addImageToDiary(diary.id, diaryImage)
                                    // 注意：这里我们不增加imageCount，因为在函数签名中它是val
                                } catch (e: Exception) {
                                    Log.e("BackupManager", "Error processing image for diary ${diary.id}: ${e.message}", e)
                                }
                            }
                        }
                    }
                    
                    batchCount++
                    // 每处理50个日记就输出一次日志并触发GC
                    if (batchCount % 50 == 0) {
                        Log.d("BackupManager", "Processed $batchCount diaries so far. Triggering GC.")
                        System.gc()
                    }
                } catch (e: Exception) {
                    Log.e("BackupManager", "Error processing diary: ${e.message}", e)
                }
            }
        }
        
        return successCount
    }
    
    /**
     * 解析单个日记对象
     */
    private fun parseDiaryObject(parser: JsonParser): Pair<Diary, List<ImageData>>? {
        var id: String? = null
        var content: String? = null
        var createTime: String? = null
        var updateTime: String? = null
        var privacyLevel = 0
        var category = "默认分类"
        var tags = ""
        val images = mutableListOf<ImageData>()
        
        // 解析日记对象字段
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = parser.currentName()
            parser.nextToken() // 移动到字段值
            
            when (fieldName) {
                "id" -> id = parser.valueAsString
                "content" -> content = parser.valueAsString
                "createTime" -> createTime = parser.valueAsString
                "updateTime" -> updateTime = parser.valueAsString
                "privacyLevel" -> privacyLevel = parser.intValue
                "category" -> category = parser.valueAsString ?: "默认分类"
                "tags" -> tags = parser.valueAsString ?: ""
                "images" -> {
                    // 处理图片数组
                    if (parser.currentToken() == JsonToken.START_ARRAY) {
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            if (parser.currentToken() == JsonToken.START_OBJECT) {
                                val imageData = parseImageObject(parser)
                                if (imageData != null) {
                                    images.add(imageData)
                                }
                            }
                        }
                    }
                }
                else -> {
                    // 跳过其他字段
                    parser.skipChildren()
                }
            }
        }
        
        // 确保必要字段存在
        if (id != null && content != null && createTime != null && updateTime != null) {
            val diary = Diary(
                id = id,
                content = content,
                createTime = createTime,
                updateTime = updateTime,
                privacyLevel = privacyLevel,
                category = category,
                tags = tags
            )
            return Pair(diary, images)
        }
        
        return null
    }
    
    /**
     * 解析单个图片对象
     */
    private fun parseImageObject(parser: JsonParser): ImageData? {
        var fileName = "image.jpg"
        var base64Data = ""
        var originalPath: String? = null
        var compressed = false
        var compressionQuality = 85
        var originalSize = 0L
        var compressedSize = 0L
        var format = "JPEG"
        
        // 解析图片对象字段
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = parser.currentName()
            parser.nextToken() // 移动到字段值
            
            when (fieldName) {
                "fileName" -> fileName = parser.valueAsString ?: "image.jpg"
                "base64Data" -> base64Data = parser.valueAsString ?: ""
                "originalPath" -> originalPath = parser.valueAsString
                "compressed" -> compressed = parser.booleanValue
                "compressionQuality" -> compressionQuality = parser.intValue
                "originalSize" -> originalSize = parser.longValue
                "compressedSize" -> compressedSize = parser.longValue
                "format" -> format = parser.valueAsString ?: "JPEG"
                else -> {
                    // 跳过其他字段
                    parser.skipChildren()
                }
            }
        }
        
        // 确保必要的数据存在
        if (base64Data.isNotEmpty()) {
            return ImageData(
                fileName = fileName,
                base64Data = base64Data,
                originalPath = originalPath,
                compressed = compressed,
                compressionQuality = compressionQuality,
                originalSize = originalSize,
                compressedSize = compressedSize,
                format = format
            )
        }
        
        return null
    }
    
    /**
     * 图片数据类
     */
    private data class ImageData(
        val fileName: String,
        val base64Data: String,
        val originalPath: String?,
        val compressed: Boolean,
        val compressionQuality: Int,
        val originalSize: Long,
        val compressedSize: Long,
        val format: String
    )

    /**
     * 从小程序单个备份文件(.bak)导入日记
     * @param filePath 小程序备份文件路径
     * @return 导入结果
     */
    suspend fun importMiniProgramDiary(filePath: String): ImportResult {
        return withContext(Dispatchers.IO) {
            try {
                // 读取.bak文件
                val inputFile = File(filePath)
                val inputStream = FileInputStream(inputFile)
                val jsonString = inputStream.readBytes().toString(Charsets.UTF_8)
                inputStream.close()

                // 解析JSON
                val diaryJson = JSONObject(jsonString)

                // 创建日记对象
                val diary = Diary(
                    id = diaryJson.optString("id", "miniprogram_${System.currentTimeMillis()}"),
                    content = diaryJson.getString("content"),
                    createTime = diaryJson.getString("createTime"),
                    updateTime = diaryJson.optString("updateTime", diaryJson.getString("createTime")),
                    privacyLevel = diaryJson.optInt("privacyLevel", PrivacyLevels.PUBLIC),
                    category = "默认分类",
                    tags = ""
                )

                // 处理日记
                val processedDiary = diaryRepository.privacyManagerPublic.processDiaryForSave(diary)

                // 检查日记是否已存在
                val existingDiary = diaryRepository.getDiaryById(diary.id)
                if (existingDiary != null) {
                    // 日记已存在，跳过
                    return@withContext ImportResult(
                        successCount = 0,
                        failedCount = 1,
                        totalCount = 1,
                        imageCount = 0
                    )
                }

                // 添加日记
                diaryRepository.addDiary(processedDiary)

                var imageCount = 0
                // 处理图片
                if (diaryJson.has("images")) {
                    val imagesArray = diaryJson.getJSONArray("images")
                    for (j in 0 until imagesArray.length()) {
                        try {
                            val imageJson = imagesArray.getJSONObject(j)

                            // 检查是否有错误
                            if (imageJson.has("error")) {
                                continue
                            }

                            // 获取Base64数据
                            val base64Data = imageJson.getString("base64Data")

                            // 将Base64转换为图片文件
                            val fileName = imageJson.optString("fileName", "image_${System.currentTimeMillis()}_$j.jpg")
                            val imagePath = convertBase64ToImage(base64Data, fileName)

                            // 创建图片对象
                            val diaryImage = DiaryImage(
                                id = "image_${System.currentTimeMillis()}_${j}",
                                diaryId = processedDiary.id,
                                imagePath = imagePath,
                                originalPath = imageJson.optString("originalPath", null),
                                compressed = imageJson.optBoolean("compressed", false),
                                compressionQuality = imageJson.optInt("compressionQuality", 85),
                                originalSize = imageJson.optLong("originalSize", 0),
                                compressedSize = imageJson.optLong("base64Size", 0),
                                format = "JPEG"
                            )

                            // 添加图片
                            diaryRepository.addImageToDiary(diary.id, diaryImage)
                            imageCount++
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // 单个图片失败不影响整个日记导入
                        }
                    }
                }

                return@withContext ImportResult(
                    successCount = 1,
                    failedCount = 0,
                    totalCount = 1,
                    imageCount = imageCount
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext ImportResult(
                    successCount = 0,
                    failedCount = 1,
                    totalCount = 1,
                    imageCount = 0
                )
            }
        }
    }

    /**
     * 从多个小程序备份文件导入日记
     * @param filePaths 小程序备份文件路径列表
     * @return 导入结果
     */
    suspend fun importMiniProgramDiaries(filePaths: List<String>): ImportResult {
        var successCount = 0
        var failedCount = 0
        var imageCount = 0
        val totalCount = filePaths.size

        for (filePath in filePaths) {
            try {
                val result = importMiniProgramDiary(filePath)
                successCount += result.successCount
                failedCount += result.failedCount
                imageCount += result.imageCount
            } catch (e: Exception) {
                e.printStackTrace()
                failedCount++
            }
        }

        return ImportResult(
            successCount = successCount,
            failedCount = failedCount,
            totalCount = totalCount,
            imageCount = imageCount
        )
    }

    /**
     * 将JSON对象写入文件（避免OOM）
     */
    private fun writeJsonToFile(jsonObject: JSONObject, file: File) {
        try {
            val outputStream = FileOutputStream(file)
            // 不使用toString(2)格式化以节省内存
            val jsonString = jsonObject.toString()
            outputStream.write(jsonString.toByteArray())
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            Log.e("BackupManager", "Error writing JSON to file: ${e.message}", e)
            throw e
        }
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * 将图片转换为Base64
     * @param imagePath 图片路径
     * @return Base64编码的图片数据
     */
    private fun convertImageToBase64(imagePath: String): String {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                val bytes = file.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                ""
            }
        } catch (e: OutOfMemoryError) {
            Log.e("BackupManager", "Out of memory when converting image to Base64: $imagePath", e)
            ""
        } catch (e: Exception) {
            Log.e("BackupManager", "Error converting image to Base64: ${e.message}", e)
            ""
        }
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
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "diary_backup_${timestamp}.json"
        
        // 对于 Android 10 及以上版本，使用 MediaStore API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 直接创建临时文件用于写入，后续通过 saveToDownloads 保存到公共目录
            val tempFile = File(context.cacheDir, fileName)
            return tempFile
        }
        
        // 对于较低版本的 Android 使用传统方法
        val outputDir = File(context.getExternalFilesDir(null), "backups")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        return File(outputDir, fileName)
    }
    
    /**
     * 保存备份文件到 Downloads 目录
     * @param file 要保存的文件
     * @param fileName 文件名
     * @return 保存的文件路径
     */
    private fun saveToDownloads(file: File, fileName: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/LoveDiary")
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().copyTo(outputStream)
                    }
                    "已保存到 Downloads/LoveDiary/$fileName"
                } else {
                    file.absolutePath
                }
            } else {
                // 对于 Android 10 以下版本，直接复制到 Downloads 目录
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val loveDiaryDir = File(downloadsDir, "LoveDiary")
                if (!loveDiaryDir.exists()) {
                    loveDiaryDir.mkdirs()
                }
                val destFile = File(loveDiaryDir, fileName)
                file.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Error saving to downloads: ${e.message}", e)
            file.absolutePath
        }
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