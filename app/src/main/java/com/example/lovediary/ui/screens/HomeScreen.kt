package com.example.lovediary.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lovediary.R
import com.example.lovediary.security.AuthStatus
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import com.example.lovediary.utils.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "情侣日记")
                },
                actions = {
                    // 身份验证状态指示器和控制按钮
                    val authStatus = viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus()
                    IconButton(onClick = {
                        if (authStatus == AuthStatus.AUTHORIZED) {
                            // 已验证则退出登录
                            viewModel.diaryRepository.privacyManagerPublic.logout()
                        } else {
                            // 未验证则跳转到登录界面
                            navController.navigate("login")
                        }
                    }) {
                        Icon(
                            imageVector = if (authStatus == AuthStatus.AUTHORIZED) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            contentDescription = if (authStatus == AuthStatus.AUTHORIZED) "已验证" else "未验证"
                        )
                    }
                    
                    // 导出按钮
                    IconButton(onClick = {
                        exportDiaries(context, viewModel)
                    }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "导出")
                    }
                    
                    // 导入按钮
                    IconButton(onClick = {
                        // 这里可以添加导入功能
                        Toast.makeText(context, "导入功能待实现", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.FileUpload, contentDescription = "导入")
                    }
                    
                    IconButton(onClick = {
                        navController.navigate("category_tag_manager")
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
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
            val authStatus = viewModel.diaryRepository.privacyManagerPublic.checkAuthStatus()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = if (authStatus == AuthStatus.AUTHORIZED) {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                } else {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (authStatus == AuthStatus.AUTHORIZED) "已验证身份" else "未验证身份",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (authStatus == AuthStatus.AUTHORIZED) 
                                "您可以查看所有私密和加密日记" 
                            else 
                                "部分日记内容已被隐藏",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    TextButton(
                        onClick = {
                            if (authStatus == AuthStatus.AUTHORIZED) {
                                // 已验证则退出登录
                                viewModel.diaryRepository.privacyManagerPublic.logout()
                            } else {
                                // 未验证则跳转到登录界面
                                navController.navigate("login")
                            }
                        }
                    ) {
                        Text(
                            text = if (authStatus == AuthStatus.AUTHORIZED) 
                                stringResource(R.string.diary_logout_button) 
                            else 
                                stringResource(R.string.login_button)
                        )
                    }
                }
            }
            
            // 分类筛选
            Text(
                text = "分类",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    Card(
                        modifier = Modifier
                            .clickable {
                                viewModel.loadDiariesByCategory(category)
                            },
                        colors = if (category == selectedCategory) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp, 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Category,
                                contentDescription = "分类",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = category)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // 标签筛选
            Text(
                text = "标签",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    Card(
                        modifier = Modifier
                            .clickable {
                                viewModel.loadDiariesByTag(tag)
                            },
                        colors = if (tag == selectedTag) {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp, 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Tag,
                                contentDescription = "标签",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = tag)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // 日记列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(diaries) {
                    DiaryCard(
                        diary = it,
                        onClick = {
                            viewModel.getDiaryById(it.id)
                            navController.navigate("diary_detail")
                        }
                    )
                }
            }
        }
    }
}

/**
 * 日记卡片
 */
@Composable
fun DiaryCard(
    diary: com.example.lovediary.data.entity.Diary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = diary.content.take(50) + if (diary.content.length > 50) "..." else "",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = diary.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = diary.createTime.substring(0, 10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (diary.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = diary.tags,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
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
                Toast.makeText(
                    context,
                    "导出成功: $filePath",
                    Toast.LENGTH_LONG
                ).show()
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