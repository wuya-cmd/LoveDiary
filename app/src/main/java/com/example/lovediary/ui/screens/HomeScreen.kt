package com.example.lovediary.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage

import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.security.AuthStatus
import com.example.lovediary.security.PrivacyLevels
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import com.example.lovediary.utils.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 主屏幕
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    val diaries by viewModel.diaries.collectAsState()
    val context = LocalContext.current
    
    // 存储每个日记的图片列表
    val diaryImages = remember { mutableStateMapOf<String, List<DiaryImage>>() }
    
    // 相识日期相关状态
    var showAnniversaryDialog by remember { mutableStateOf(false) }
    var anniversaryInput by remember { mutableStateOf("") }
    val sharedPreferences = context.getSharedPreferences("love_diary_prefs", Context.MODE_PRIVATE)
    var anniversaryDate by remember { mutableStateOf(sharedPreferences.getString(BackupManager.START_DATE_KEY, null)) }
    
    // 加载每个日记的图片
    LaunchedEffect(diaries) {
        diaries.forEach { diary ->
            if (!diaryImages.containsKey(diary.id)) {
                val images = viewModel.diaryRepository.getImagesByDiaryId(diary.id)
                diaryImages[diary.id] = images
            }
        }
    }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                importDiariesFromFile(context, viewModel, it)
            }
        }
    )

    // 多文件选择器（用于小程序备份文件）
    val multipleFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                importMiniProgramDiaries(context, viewModel, uris)
            }
        }
    )

    Scaffold(
// 1. 让 Scaffold 不再自动消费系统栏 insets
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // 2. 把 TopAppBar 高度压到 56 dp（或 48 dp）
        topBar = {
            TopAppBar(
                //modifier = Modifier.height(56.dp),          // ← 关键
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 左侧爱心
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.Unspecified,            // 用渐变
                            modifier = Modifier
                                .size(18.dp)
                                .drawWithContent {
                                    val brush = Brush.linearGradient(
                                        listOf(Color(0xFFFF5FA2), Color(0xFFFF8E9E))
                                    )
                                    drawContent()
                                    drawRect(brush, blendMode = BlendMode.SrcAtop)
                                }
                        )
                        Spacer(Modifier.width(10.dp))
                        // 渐变「朝暮」
                        Text(
                            text = "朝暮",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFFFF5FA2), Color(0xFFFF8E9E))
                                ),
                                shadow = Shadow(
                                    color = Color(0x55FF5FA2),
                                    offset = Offset(0f, 3f),
                                    blurRadius = 6f
                                )
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        // 右侧爱心
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(18.dp)
                                .drawWithContent {
                                    val brush = Brush.linearGradient(
                                        listOf(Color(0xFFFF5FA2), Color(0xFFFF8E9E))
                                    )
                                    drawContent()
                                    drawRect(brush, blendMode = BlendMode.SrcAtop)
                                }
                        )
                    }
                },
                actions = {
                    // 身份验证状态指示器和控制按钮
                    IconButton(onClick = {
                        if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED) {
                            // 已验证则退出登录
                            viewModel.diaryRepository.privacyManagerPublic.logout()
                            // 重新加载日记列表以刷新状态
                            viewModel.loadDiaries()
                        } else {
                            // 未验证则跳转到登录界面
                            navController.navigate("login")
                        }
                    }) {
                        Icon(
                            imageVector = if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED) "已验证" else "未验证",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // 导出按钮
                    IconButton(onClick = {
                        exportDiaries(context, viewModel)
                    }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "导出", modifier = Modifier.size(20.dp))
                    }
                    
                    // 导入按钮（标准JSON）
                    IconButton(onClick = {
                        // 启动文件选择器
                        filePickerLauncher.launch("application/json")
                    }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "导入", modifier = Modifier.size(20.dp))
                    }
                    
                    // 灵犀同步按钮
                    IconButton(onClick = {
                        navController.navigate("linxi_sync")
                    }) {
                        Icon(Icons.Filled.Sync, contentDescription = "灵犀同步", modifier = Modifier.size(20.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 身份验证状态显示
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED) {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED) "已验证身份" else "访客模式",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        )
                        Text(
                            text = if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED)
                                "您可以查看所有私密日记" 
                            else 
                                "部分日记内容已被隐藏",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )
                    }
                    
                    TextButton(
                        onClick = {
                            if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED) {
                                // 已验证则退出登录
                                viewModel.diaryRepository.privacyManagerPublic.logout()
                                // 重新加载日记列表以刷新状态
                                viewModel.loadDiaries()
                            } else {
                                // 未验证则跳转到登录界面
                                navController.navigate("login")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED) 
                                "退出登录" 
                            else 
                                "登录",
                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                        )
                    }
                }
            }
            
            // 相识天数显示卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),  // 增大圆角使外观更柔和
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFE4E1)  // 使用浅粉色背景营造温馨氛围
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF5FA2),  // 改为粉色
                            modifier = Modifier
                                .size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "我们已相识",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize.times(1.2f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF5FA2),  // 改为粉色
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (anniversaryDate != null) {
                                calculateDaysTogether(anniversaryDate!!)
                            } else {
                                "尚未设置日期"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Blue,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize.times(1.5f)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
                
                if (anniversaryDate == null) {
                    TextButton(
                        onClick = { showAnniversaryDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("设置相识日期", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                }
            }
            
            // 日记列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // 按年份分组显示日记
                val groupedDiaries = diaries.groupBy { diary ->
                    try {
                        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(diary.createTime)
                        SimpleDateFormat("yyyy年", Locale.getDefault()).format(date)
                    } catch (e: Exception) {
                        "未知年份"
                    }
                }.toList().sortedByDescending { it.first }

                groupedDiaries.forEach { (year, yearDiaries) ->
                    item {
                        var expanded by remember { mutableStateOf(true) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 2.dp, bottom = 1.dp)
                                .clickable { expanded = !expanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = if (expanded) "收起" else "展开",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            Text(
                                text = year,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 6.dp),
                                fontSize = MaterialTheme.typography.titleMedium.fontSize
                            )
                        }
                        
                        if (expanded) {
                            yearDiaries.sortedByDescending { it.createTime }.forEach { diary ->
                                DiaryCard(
                                    diary = diary,
                                    diaryImages = diaryImages,
                                    authStatus = viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus(),
                                    onClick = {
                                        // 检查是否有权限访问该日记
                                        val canAccess = when (diary.privacyLevel) {
                                            PrivacyLevels.PUBLIC -> true
                                            PrivacyLevels.PRIVATE -> viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus() == AuthStatus.AUTHORIZED
                                            else -> false
                                        }
                                        
                                        if (canAccess) {
                                            viewModel.getDiaryById(diary.id)
                                            navController.navigate("diary_detail")
                                        } else {
                                            // 显示提示信息
                                            Toast.makeText(context, "此日记为私密内容，请先验证身份", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 设置相识日期对话框
    if (showAnniversaryDialog) {
        AlertDialog(
            onDismissRequest = { showAnniversaryDialog = false },
            title = { Text("设置相识日期") },
            text = {
                Column {
                    Text("请输入你们的相识日期 (格式: yyyy-MM-dd)")
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = anniversaryInput,
                        onValueChange = { anniversaryInput = it },
                        placeholder = { Text("例如: 2020-01-01 ") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isValidDateFormat(anniversaryInput)) {
                            // 保存日期到 SharedPreferences
                            sharedPreferences.edit()
                                .putString(BackupManager.START_DATE_KEY, anniversaryInput)
                                .apply()
                            anniversaryDate = anniversaryInput
                            showAnniversaryDialog = false
                        } else {
                            Toast.makeText(context, "日期格式不正确，请使用 yyyy-MM-dd 格式，日期月份不够两位的要添加 0 前缀", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnniversaryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 日记卡片
 */
@Composable
fun DiaryCard(
    diary: Diary,
    diaryImages: Map<String, List<DiaryImage>>,
    authStatus: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)  // 浅灰白色背景，柔和舒适
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 检查是否有权限访问该日记内容
            val canAccessContent = when (diary.privacyLevel) {
                PrivacyLevels.PUBLIC -> true
                PrivacyLevels.PRIVATE -> authStatus == AuthStatus.AUTHORIZED
                else -> authStatus == AuthStatus.AUTHORIZED
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (diary.privacyLevel == PrivacyLevels.PRIVATE) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (canAccessContent) {
                        diary.content.take(50) + if (diary.content.length > 50) "..." else ""
                    } else {
                        "🔒 此日记为私密内容，请先验证身份查看"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            
            // 只有在有权限的情况下才显示图片
            if (canAccessContent) {
                // 显示日记图片缩略图
                diaryImages[diary.id]?.let { images ->
                    if (images.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(images.take(3)) { image -> // 只显示前3张图片
                                ElevatedCard(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    AsyncImage(
                                        model = File(image.imagePath),
                                        contentDescription = "日记图片",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            
                            // 如果还有更多图片，显示数量指示器
                            if (images.size > 3) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+${images.size - 3}",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 显示隐私等级而不是分类
                val privacyText = when (diary.privacyLevel) {
                    0 -> "🌐 公开"
                    1 -> "🔒 私密"
                    else -> "❓未知"
                }
                Text(
                    text = privacyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize.times(0.8f)
                )
                // 修改日期显示样式，使其更加显眼
                Text(
                    text = try {
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(diary.createTime)
                        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(date)
                    } catch (e: Exception) {
                        diary.createTime.substring(0, 10)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            }
            if (diary.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.size(1.dp))
                Text(
                    text = diary.tags,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize.times(0.8f)
                )
            }
        }
    }
}

/**
 * 导出日记
 */
private fun exportDiaries(context: Context, viewModel: DiaryViewModel) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val backupManager = BackupManager(context, viewModel.diaryRepository)
            val config = BackupManager.BackupConfig()
            val filePath = backupManager.exportDiaries(config)
            
            // 在主线程中显示结果
            CoroutineScope(Dispatchers.Main).launch {
                if (filePath.startsWith("已保存到")) {
                    Toast.makeText(
                        context,
                        "导出成功: $filePath",
                        Toast.LENGTH_LONG
                    ).show()
//                    Toast.makeText(
//                        context,
//                        "请在文件管理器中查看",
//                        Toast.LENGTH_LONG
//                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "导出成功，文件保存在：$filePath",
                        Toast.LENGTH_LONG
                    ).show()
                    Toast.makeText(
                        context,
                        "这是应用私有目录，建议在设置中启用\"保存到公共目录\"选项以便访问",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            
            // 在主线程中显示错误
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "导出失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * 从文件导入日记
 */
private fun importDiariesFromFile(context: Context, viewModel: DiaryViewModel, fileUri: Uri) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val backupManager = BackupManager(context, viewModel.diaryRepository)
            
            // 将Uri文件复制到临时文件
            val tempFile = File.createTempFile("import_", ".json", context.cacheDir)
            context.contentResolver.openInputStream(fileUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            val result = backupManager.importDiaries(tempFile.absolutePath)
            
            // 删除临时文件
            tempFile.delete()
            
            // 在主线程中显示结果
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "导入完成: 成功${result.successCount}个，失败${result.failedCount}个，共${result.totalCount}个日记",
                    Toast.LENGTH_LONG
                ).show()
                
                // 刷新日记列表
                viewModel.loadDiaries()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            
            // 在主线程中显示错误
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "导入失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * 从小程序备份文件导入日记
 */
private fun importMiniProgramDiaries(context: Context, viewModel: DiaryViewModel, fileUris: List<Uri>) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val backupManager = BackupManager(context, viewModel.diaryRepository)
            
            // 将Uri文件复制到临时文件并收集路径
            val tempFilePaths = mutableListOf<String>()
            fileUris.forEach { uri ->
                try {
                    val extension = if (uri.toString().endsWith(".bak")) ".bak" else ".tmp"
                    val tempFile = File.createTempFile("miniprogram_import_", extension, context.cacheDir)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFilePaths.add(tempFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val result = backupManager.importMiniProgramDiaries(tempFilePaths)
            
            // 删除临时文件
            tempFilePaths.forEach { path ->
                try {
                    File(path).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 在主线程中显示结果
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "小程序备份导入完成: 成功${result.successCount}个，失败${result.failedCount}个，共${result.totalCount}个日记，导入了${result.imageCount}张图片",
                    Toast.LENGTH_LONG
                ).show()
                
                // 刷新日记列表
                viewModel.loadDiaries()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            
            // 在主线程中显示错误
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "导入失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

/**
 * 计算在一起的天数
 */
fun calculateDaysTogether(startDateStr: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = format.parse(startDateStr)
        val currentDate = Date()
        val diff = currentDate.time - startDate!!.time
        val days = diff / (1000 * 60 * 60 * 24)
        "${days} 天"
    } catch (e: Exception) {
        "日期错误"
    }
}

/**
 * 验证日期格式是否正确
 */
fun isValidDateFormat(dateStr: String): Boolean {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.isLenient = false
        format.parse(dateStr)
        true
    } catch (e: Exception) {
        false
    }
}