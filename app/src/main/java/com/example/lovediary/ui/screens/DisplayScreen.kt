package com.example.lovediary.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.lovediary.utils.DiaryImageHelper

/**
 * 美图展示屏幕
 */
@Composable
fun DisplayScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    val displayDescription by viewModel.displayDescription.collectAsState()
    val displayImageUri by viewModel.displayImageUri.collectAsState()
    val context = LocalContext.current
    var pendingImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    // 使用Photo Picker（Android 13+无需权限）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            viewModel.setDisplayImageUri(uri)
        }
    )
    
    // 设置日记添加成功后的回调，用于保存图片
    DisposableEffect(viewModel) {
        viewModel.onDiaryAdded = { diaryId ->
            pendingImageUri?.let { uri ->
                DiaryImageHelper.saveDiaryImages(
                    context = context,
                    viewModel = viewModel,
                    diaryId = diaryId,
                    imageUris = listOf(uri)
                )
                pendingImageUri = null
            }
            isSaving = false
        }
        
        onDispose {
            viewModel.onDiaryAdded = null
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "记录此刻心情",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        TextField(
            value = displayDescription,
            onValueChange = { viewModel.setDisplayDescription(it) },
            label = { Text("写下你的心情...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            minLines = 3
        )
        
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("选择图片")
        }
        
        displayImageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "选中的图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 16.dp),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (displayDescription.isNotBlank()) {
                    isSaving = true
                    pendingImageUri = displayImageUri
                    viewModel.addDiary(
                        content = displayDescription,
                        privacyLevel = com.example.lovediary.security.PrivacyLevels.PUBLIC
                    )
                    // 清除状态
                    viewModel.setDisplayDescription("")
                    viewModel.setDisplayImageUri(null)
                    isSaving = false
                    Toast.makeText(context, "心情已保存", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "请输入内容", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && displayDescription.isNotBlank()
        ) {
            Text("保存心情")
        }
    }
}