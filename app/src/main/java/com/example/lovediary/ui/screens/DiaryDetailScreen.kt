package com.example.lovediary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
                                .clip(RoundedCornerShape(16.dp)),
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
                                .padding(horizontal = 8.dp, vertical = 4.dp),  // 减少内边距
                                // 从horizontal = 16.dp, vertical = 8.dp改为horizontal = 8.dp, vertical = 4.dp
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(diaryImages.drop(1)) { image ->  // 跳过第一张主图
                                Card(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
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