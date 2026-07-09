package com.example.lovediary.utils

import android.content.Context
import android.util.Log
import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.data.repository.DiaryRepository
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*

/**
 * 灵犀同步管理器
 * 实现情侣间日记的双向同步功能
 * 基于已建立的网络连接（如WiFi热点）进行数据交换
 */
class LinxiSyncManager(
    private val context: Context,
    private val diaryRepository: DiaryRepository
) {
    companion object {
        const val PORT = 8888
        const val SOCKET_TIMEOUT = 30000
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    enum class SyncRole {
        HOST, CLIENT, NONE
    }

    enum class SyncStatus {
        IDLE, CONNECTING, EXCHANGING_INDEX, SYNCING, COMPLETED, ERROR
    }

    data class DiaryIndex(
        val id: String,
        val updateTime: String
    )

    interface SyncCallback {
        fun onStatusChanged(status: SyncStatus)
        fun onProgress(progress: Int, total: Int)
        fun onComplete(success: Boolean, message: String)
    }

    private var syncCallback: SyncCallback? = null
    private var currentSyncRole: SyncRole = SyncRole.NONE
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var isSyncActive = false
    private var indicesToReceive: List<DiaryIndex> = emptyList()
    private var diariesToSend: List<Diary> = emptyList()

    /**
     * 开始作为主机等待连接
     */
    fun startAsHost(callback: SyncCallback) {
        syncCallback = callback
        currentSyncRole = SyncRole.HOST
        isSyncActive = true
        
        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    callback.onStatusChanged(SyncStatus.CONNECTING)
                }
                
                serverSocket = ServerSocket(PORT)
                serverSocket?.soTimeout = SOCKET_TIMEOUT
                
                val socket = serverSocket?.accept()
                clientSocket = socket
                
                if (socket != null) {
                    handleSync(socket)
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onComplete(false, "连接失败")
                    }
                }
            } catch (e: Exception) {
                Log.e("LinxiSyncManager", "等待连接失败", e)
                withContext(Dispatchers.Main) {
                    callback.onComplete(false, "等待连接失败: ${e.message}")
                }
            } finally {
                cleanup()
            }
        }
    }

    /**
     * 开始作为客户端连接主机
     */
    fun startAsClient(hostIp: String, callback: SyncCallback) {
        syncCallback = callback
        currentSyncRole = SyncRole.CLIENT
        isSyncActive = true
        
        scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    callback.onStatusChanged(SyncStatus.CONNECTING)
                }
                
                val socket = Socket()
                val address = InetSocketAddress(hostIp, PORT)
                socket.connect(address, SOCKET_TIMEOUT)
                clientSocket = socket
                
                handleSync(socket)
            } catch (e: Exception) {
                Log.e("LinxiSyncManager", "连接对方失败", e)
                withContext(Dispatchers.Main) {
                    callback.onComplete(false, "连接对方失败: ${e.message}，请确保对方已在等待连接且网络畅通")
                }
            } finally {
                cleanup()
            }
        }
    }

    /**
     * 处理同步过程
     */
    private suspend fun handleSync(socket: Socket) {
        try {
            withContext(Dispatchers.Main) {
                syncCallback?.onStatusChanged(SyncStatus.EXCHANGING_INDEX)
            }
            
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()
            
            exchangeDiaryIndices(inputStream, outputStream)
            
            withContext(Dispatchers.Main) {
                syncCallback?.onStatusChanged(SyncStatus.SYNCING)
            }
            
            syncDiaryData(inputStream, outputStream)
            
            withContext(Dispatchers.Main) {
                syncCallback?.onStatusChanged(SyncStatus.COMPLETED)
                syncCallback?.onComplete(true, "同步完成，数据已更新")
            }
        } catch (e: Exception) {
            Log.e("LinxiSyncManager", "同步失败", e)
            withContext(Dispatchers.Main) {
                syncCallback?.onComplete(false, "同步失败: ${e.message}")
            }
        }
    }

    /**
     * 交换日记索引
     */
    private suspend fun exchangeDiaryIndices(inputStream: InputStream, outputStream: OutputStream) {
        val localIndices = getLocalDiaryIndices()
        
        val localIndicesJson = JSONArray()
        localIndices.forEach { index ->
            val obj = JSONObject()
            obj.put("id", index.id)
            obj.put("updateTime", index.updateTime)
            localIndicesJson.put(obj)
        }
        
        val sendObj = JSONObject()
        sendObj.put("indices", localIndicesJson)
        sendObj.put("deviceId", getDeviceId())
        
        outputStream.write(sendObj.toString().toByteArray())
        outputStream.write("\n".toByteArray())
        outputStream.flush()
        
        val reader = BufferedReader(InputStreamReader(inputStream))
        val receivedLine = reader.readLine()
        val receivedObj = JSONObject(receivedLine)
        
        val remoteIndices = mutableListOf<DiaryIndex>()
        val remoteIndicesJson = receivedObj.getJSONArray("indices")
        for (i in 0 until remoteIndicesJson.length()) {
            val obj = remoteIndicesJson.getJSONObject(i)
            remoteIndices.add(DiaryIndex(
                id = obj.getString("id"),
                updateTime = obj.getString("updateTime")
            ))
        }
        
        indicesToReceive = determineIndicesToReceive(localIndices, remoteIndices)
        diariesToSend = determineDiariesToSend(localIndices, remoteIndices)
    }

    /**
     * 同步日记数据
     */
    private suspend fun syncDiaryData(inputStream: InputStream, outputStream: OutputStream) {
        sendDiaries(outputStream, diariesToSend)
        receiveDiaries(inputStream, indicesToReceive)
    }
    
    /**
     * 获取本地日记索引
     */
    private suspend fun getLocalDiaryIndices(): List<DiaryIndex> {
        val diaries = diaryRepository.getAllDiariesRaw()
        return diaries.map { diary ->
            DiaryIndex(diary.id, diary.updateTime)
        }
    }
    
    /**
     * 确定需要接收的日记索引
     */
    private fun determineIndicesToReceive(localIndices: List<DiaryIndex>, remoteIndices: List<DiaryIndex>): List<DiaryIndex> {
        val localIds = localIndices.map { it.id }.toSet()
        return remoteIndices.filter { !localIds.contains(it.id) }
    }
    
    /**
     * 确定需要发送的日记
     */
    private suspend fun determineDiariesToSend(localIndices: List<DiaryIndex>, remoteIndices: List<DiaryIndex>): List<Diary> {
        val remoteIds = remoteIndices.map { it.id }.toSet()
        val localDiaries = diaryRepository.getAllDiariesRaw()
        return localDiaries.filter { !remoteIds.contains(it.id) }
    }
    
    /**
     * 发送日记
     */
    private suspend fun sendDiaries(outputStream: OutputStream, diaries: List<Diary>) {
        withContext(Dispatchers.Main) {
            syncCallback?.onProgress(0, diaries.size)
        }
        
        diaries.forEachIndexed { index, diary ->
            try {
                val images: List<DiaryImage> = diaryRepository.getImagesByDiaryId(diary.id)
                
                val diaryJson = JSONObject()
                diaryJson.put("id", diary.id)
                diaryJson.put("content", diary.content)
                diaryJson.put("createTime", diary.createTime)
                diaryJson.put("updateTime", diary.updateTime)
                diaryJson.put("privacyLevel", diary.privacyLevel)
                diaryJson.put("category", diary.category)
                diaryJson.put("tags", diary.tags)
                
                val imagesArray = JSONArray()
                images.forEach { image ->
                    val imageJson = JSONObject()
                    imageJson.put("id", image.id)
                    imageJson.put("diaryId", image.diaryId)
                    imageJson.put("imagePath", image.imagePath)
                    imageJson.put("originalPath", image.originalPath ?: "")
                    imageJson.put("compressed", image.compressed)
                    imageJson.put("compressionQuality", image.compressionQuality)
                    imageJson.put("originalSize", image.originalSize)
                    imageJson.put("compressedSize", image.compressedSize)
                    imageJson.put("format", image.format)
                    
                    try {
                            val imageFile = File(image.imagePath)
                            if (imageFile.exists()) {
                                val bytes = imageFile.readBytes()
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                imageJson.put("base64Data", base64)
                            }
                        } catch (e: Exception) {
                            Log.e("LinxiSyncManager", "读取图片失败", e)
                        }
                        
                        imagesArray.put(imageJson)
                    }
                    
                    diaryJson.put("images", imagesArray)
                    
                    val dataToSend = diaryJson.toString()
                    outputStream.write(dataToSend.toByteArray())
                    outputStream.write("\n".toByteArray())
                    outputStream.flush()
            } catch (e: Exception) {
                Log.e("LinxiSyncManager", "发送日记失败", e)
            }
            
            withContext(Dispatchers.Main) {
                syncCallback?.onProgress(index + 1, diaries.size)
            }
        }
    }
    
    /**
     * 接收日记
     */
    private suspend fun receiveDiaries(inputStream: InputStream, indices: List<DiaryIndex>) {
        withContext(Dispatchers.Main) {
            syncCallback?.onProgress(0, indices.size)
        }
        
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        indices.forEachIndexed { index, _ ->
            try {
                val line = reader.readLine()
                if (line != null) {
                    val diaryJson = JSONObject(line)
                    
                    val diary = Diary(
                        id = diaryJson.getString("id"),
                        content = diaryJson.getString("content"),
                        createTime = diaryJson.getString("createTime"),
                        updateTime = diaryJson.getString("updateTime"),
                        privacyLevel = diaryJson.getInt("privacyLevel"),
                        category = diaryJson.getString("category"),
                        tags = diaryJson.getString("tags")
                    )
                    
                    diaryRepository.addDiary(diary)
                    
                    if (diaryJson.has("images")) {
                        val imagesArray = diaryJson.getJSONArray("images")
                        for (i in 0 until imagesArray.length()) {
                            val imageJson = imagesArray.getJSONObject(i)
                            
                            if (imageJson.has("base64Data")) {
                                val base64Data = imageJson.getString("base64Data")
                                val imageData = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
                                
                                val imagesDir = File(context.getExternalFilesDir(null), "images")
                                if (!imagesDir.exists()) {
                                    imagesDir.mkdirs()
                                }
                                
                                val imageFile = File(imagesDir, "${imageJson.getString("id")}.${imageJson.getString("format").lowercase()}")
                                imageFile.writeBytes(imageData)
                                
                                val diaryImage = DiaryImage(
                                    id = imageJson.getString("id"),
                                    diaryId = imageJson.getString("diaryId"),
                                    imagePath = imageFile.absolutePath,
                                    originalPath = if (imageJson.getString("originalPath").isNotEmpty()) 
                                        imageJson.getString("originalPath") else null,
                                    compressed = imageJson.getBoolean("compressed"),
                                    compressionQuality = imageJson.getInt("compressionQuality"),
                                    originalSize = imageJson.getLong("originalSize"),
                                    compressedSize = imageJson.getLong("compressedSize"),
                                    format = imageJson.getString("format")
                                )
                                
                                diaryRepository.addImageToDiary(diary.id, diaryImage)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LinxiSyncManager", "接收日记失败", e)
            }
            
            withContext(Dispatchers.Main) {
                syncCallback?.onProgress(index + 1, indices.size)
            }
        }
    }
    
    /**
     * 获取设备ID
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        )
    }

    /**
     * 清理资源
     */
    private fun cleanup() {
        isSyncActive = false
        scope.cancel()
        
        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("LinxiSyncManager", "关闭Socket失败", e)
        }
        
        clientSocket = null
        serverSocket = null
    }

    /**
     * 停止同步
     */
    fun stopSync() {
        isSyncActive = false
        cleanup()
        syncCallback?.onComplete(false, "同步已取消")
    }
}