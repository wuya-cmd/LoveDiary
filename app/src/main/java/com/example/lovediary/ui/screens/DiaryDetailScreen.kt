package com.example.lovediary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lovediary.R
import com.example.lovediary.security.AuthStatus
import com.example.lovediary.security.PrivacyLevels
import com.example.lovediary.ui.viewmodel.DiaryViewModel

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            currentDiary?.let { diary ->
                // 日记内容
                Text(
                    text = diary.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 隐私级别
                val privacyOption = privacyOptions.find { it.value == diary.privacyLevel }
                if (privacyOption != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = privacyOption.label,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = privacyOption.desc,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                // 如果没有权限且是私密或加密日记，显示提示
                                if (!canEdit) {
                                    Text(
                                        text = "需要身份验证才能编辑此日记",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // 分类
                Text(
                    text = "分类: ${diary.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 标签
                if (diary.tags.isNotEmpty()) {
                    Text(
                        text = "标签: ${diary.tags}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 创建时间
                Text(
                    text = "创建时间: ${diary.createTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // 更新时间
                Text(
                    text = "更新时间: ${diary.updateTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            } ?: run {
                Text(
                    text = "日记不存在",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 身份验证提示对话框
    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("需要身份验证") },
            text = { Text("此日记为私密或加密内容，需要验证身份后才能编辑。") },
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