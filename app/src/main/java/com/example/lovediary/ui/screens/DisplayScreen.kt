package com.example.lovediary.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import com.example.lovediary.utils.HighlightImageHelper
import java.io.File

/**
 * 心情精选展示屏幕
 * 显示当前精选（一张大图+标题），支持选图、输入标题并保存为精选（写入历史合集）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    val currentHighlight by viewModel.currentHighlight.collectAsState()
    val context = LocalContext.current

    // 编辑态：用户选择的新图片预览URI 和 输入的标题
    var pendingImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var titleInput by remember { mutableStateOf("") }

    // 图片选择器（Android 13+ Photo Picker，低版本回退到 GetContent，均无需权限）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) pendingImageUri = uri
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("心情精选") },
                actions = {
                    IconButton(onClick = { navController.navigate("highlight_collection") }) {
                        Icon(Icons.Default.Collections, contentDescription = "查看合集")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 大图展示区：优先显示用户刚选的预览图，其次显示当前精选
            val imageModel: Any? = pendingImageUri
                ?: currentHighlight?.let { File(it.imagePath) }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "精选图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有精选图片\n点击下方按钮添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标题输入
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 选择图片按钮
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (pendingImageUri == null) "选择图片" else "重新选择")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 保存为精选按钮
            Button(
                onClick = {
                    val uri = pendingImageUri
                    if (uri != null && titleInput.isNotBlank()) {
                        // 压缩保存图片到本地，得到持久化路径
                        val savedPath = HighlightImageHelper.saveHighlightImage(context, uri)
                        if (savedPath != null) {
                            viewModel.saveHighlight(savedPath, titleInput.trim())
                            // 清空编辑态
                            pendingImageUri = null
                            titleInput = ""
                            Toast.makeText(context, "已保存到精选合集", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "请选择图片并输入标题", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pendingImageUri != null && titleInput.isNotBlank()
            ) {
                Text("保存精选")
            }
        }
    }
}
