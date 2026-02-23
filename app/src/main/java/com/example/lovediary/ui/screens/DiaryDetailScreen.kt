package com.example.lovediary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.lovediary.R
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.security.AuthStatus
import com.example.lovediary.security.PrivacyLevels
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * 日记详情屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    val currentDiary by viewModel.currentDiary.collectAsState()
    var isDeleting by remember { mutableStateOf(false) } // 防止重复删除
    var showAuthDialog by remember { mutableStateOf(false) }
    val privacyOptions = viewModel.diaryRepository.privacyManagerPublic.getPrivacyLevelOptions()
    val currentAuthStatus = viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus()
    
    // 日记图片列表
    var diaryImages by remember { mutableStateOf<List<DiaryImage>>(emptyList()) }
    
    // 图片查看器状态
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }
    
    // 加载日记图片
    LaunchedEffect(currentDiary) {
        currentDiary?.let { diary ->
            diaryImages = viewModel.diaryRepository.getImagesByDiaryId(diary.id)
        }
    }

    // 检查是否可以编辑日记
    val canEdit = currentDiary?.let { diary ->
        // 公开日记任何人都可以编辑
        if (diary.privacyLevel == PrivacyLevels.PUBLIC) {
            true
        } else {
            // 私密或加密日记需要验证身份
            currentAuthStatus == AuthStatus.AUTHORIZED
        }
    } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "日记详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (canEdit) {
                                // 编辑日记
                                navController.navigate("edit_diary")
                            } else {
                                // 显示需要身份验证的提示
                                showAuthDialog = true
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(
                        onClick = {
                            // 删除日记
                            if (!isDeleting) {
                                isDeleting = true
                                currentDiary?.let {
                                    viewModel.deleteDiary(it)
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = !isDeleting
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                currentDiary?.let { diary ->
                    // 大图展示区域
                    if (diaryImages.isNotEmpty()) {
                        val firstImage = diaryImages.first()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedImageIndex = 0
                                    showImageViewer = true
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            AsyncImage(
                                model = File(firstImage.imagePath),
                                contentDescription = "日记主图",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    // 日记内容
                    Text(
                        text = diary.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)  // 减少内边距
                            // 从horizontal = 16.dp, vertical = 8.dp改为horizontal = 8.dp, vertical = 4.dp
                    )
                    
                    // 显示日记图片列表
                    if (diaryImages.size > 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(diaryImages.drop(1)) { image ->
                                val imageIndex = diaryImages.indexOf(image)
                                Card(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedImageIndex = imageIndex
                                            showImageViewer = true
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    AsyncImage(
                                        model = File(image.imagePath),
                                        contentDescription = "日记图片",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    // 隐私级别
                    val privacyOption = privacyOptions.find { it.value == diary.privacyLevel }
                    if (privacyOption != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),  // 从16.dp减少到8.dp
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = privacyOption.label,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    // 创建时间
                    Text(
                        text = "创建时间: ${formatDateTime(diary.createTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 1.dp)  // 减少垂直内边距
                            // 从horizontal = 16.dp, vertical = 2.dp改为horizontal = 8.dp, vertical = 1.dp
                    )

                    // 更新时间
                    Text(
                        text = "更新时间: ${formatDateTime(diary.updateTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 1.dp)  // 减少垂直内边距
                            // 从horizontal = 16.dp, vertical = 2.dp改为horizontal = 8.dp, vertical = 1.dp
                    )
                } ?: run {
                    Text(
                        text = "日记不存在",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // 身份验证提示对话框
    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("需要身份验证") },
            text = { Text("此日记为私密内容，需要验证身份后才能编辑。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAuthDialog = false
                        navController.navigate("login")
                    }
                ) {
                    Text(stringResource(R.string.login_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 图片查看器对话框
    if (showImageViewer && selectedImageIndex != null && diaryImages.isNotEmpty()) {
        ImageViewerDialog(
            images = diaryImages,
            initialIndex = selectedImageIndex!!,
            onDismiss = { showImageViewer = false }
        )
    }
}

/**
 * 格式化日期时间显示
 */
fun formatDateTime(dateTime: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateTime)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateTime
    }
}

/**
 * 全屏图片查看器对话框
 * 支持缩放、平移和滑动切换图片
 */
@Composable
fun ImageViewerDialog(
    images: List<DiaryImage>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
        ) {
            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // 图片计数器
            Text(
                text = "${currentIndex + 1} / ${images.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )
            
            // 图片显示区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = max(0.5f, min(scale * zoom, 5f))
                            offset = Offset(
                                x = offset.x + pan.x,
                                y = offset.y + pan.y
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = if (scale != 1f) 1f else 2f
                                offset = Offset.Zero
                            },
                            onTap = {
                                if (scale == 1f) {
                                    onDismiss()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(images[currentIndex].imagePath),
                    contentDescription = "图片查看",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            }
            
            // 左右切换按钮
            if (images.size > 1) {
                // 上一张按钮
                if (currentIndex > 0) {
                    IconButton(
                        onClick = {
                            currentIndex--
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "上一张",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                // 下一张按钮
                if (currentIndex < images.size - 1) {
                    IconButton(
                        onClick = {
                            currentIndex++
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "下一张",
                            tint = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer(rotationZ = 180f)
                        )
                    }
                }
            }
        }
    }
}