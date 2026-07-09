package com.example.lovediary.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import com.example.lovediary.utils.DiaryImageHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 添加日记屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDiaryScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var privacyLevel by remember { mutableStateOf(0) }
    var isSaving by remember { mutableStateOf(false) } // 防止重复提交
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val images = remember { mutableStateListOf<Uri>() }
    val privacyOptions = viewModel.diaryRepository.privacyManagerPublic.getPrivacyLevelOptions()
    
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    
    // 返回操作
    fun goBack() {
        if (onNavigateBack != null) {
            onNavigateBack()
        } else {
            navController.popBackStack()
        }
    }
    
    // 图片选择launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            images.addAll(uris)
        }
    )
    
    // 权限请求launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // 权限已授予，启动图片选择器
                imagePickerLauncher.launch("image/*")
            } else {
                // 权限被拒绝，显示提示信息
                Toast.makeText(context, "需要存储权限才能添加图片", Toast.LENGTH_SHORT).show()
            }
        }
    )
    
    // 检查并请求权限
    fun checkPermissionAndPickImage() {
        when {
            // Android 13 (API 33)及以上版本使用新的权限系统
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 已经拥有权限，直接启动图片选择器
                    imagePickerLauncher.launch("image/*")
                } else {
                    // 请求权限
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            // Android 12 (API 32)及以下版本使用旧的权限系统
            else -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 已经拥有权限，直接启动图片选择器
                    imagePickerLauncher.launch("image/*")
                } else {
                    // 请求权限
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    
    // 移除图片
    fun removeImage(uri: Uri) {
        images.remove(uri)
    }
    
    // 设置日记添加回调
    DisposableEffect(viewModel) {
        viewModel.onDiaryAdded = { diaryId ->
            // 保存图片
            if (images.isNotEmpty()) {
                DiaryImageHelper.saveDiaryImages(context, viewModel, diaryId, images.toList())
            }
            // 保存完成后返回
            isSaving = false
            goBack()
        }
        
        onDispose {
            viewModel.onDiaryAdded = null
        }
    }
    
    // 日期选择对话框
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = millis
                            // 保持当前的时间部分
                            val hour = selectedDate.get(Calendar.HOUR_OF_DAY)
                            val minute = selectedDate.get(Calendar.MINUTE)
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            selectedDate = calendar
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // 时间选择对话框
    if (showTimePicker) {
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val calendar = selectedDate.clone() as Calendar
                        calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        calendar.set(Calendar.MINUTE, timePickerState.minute)
                        selectedDate = calendar
                        showTimePicker = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF5FA2),  // 改为粉色
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "添加日记",
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF5FA2),  // 改为粉色
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (!isSaving && content.isNotBlank()) {
                                isSaving = true
                                // 格式化日期为ISO格式
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                val dateTime = dateFormat.format(selectedDate.time)
                                viewModel.addDiaryWithCreateTime(content, "默认分类", "", privacyLevel, dateTime)
                                // 注意：这里暂时保留分类和标签参数，但不提供UI控件
                                // 保存操作将在DisposableEffect中完成后的回调里返回
                            }
                        },
                        enabled = content.isNotBlank() && !isSaving
                    ) {
                        Text(text = if (isSaving) "保存中..." else "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 日记内容
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(4.dp))
                        Text(text = "日记内容")
                    }
                },
                placeholder = { Text(text = "记下美好时光...") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10,
                shape = RoundedCornerShape(12.dp)
            )

            // 图片预览区域
            if (images.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { imageUri ->
                        Box {
                            Card(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { removeImage(imageUri) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "选择的图片",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // 删除按钮
                            IconButton(
                                onClick = { removeImage(imageUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "删除图片",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 添加图片按钮
            OutlinedButton(
                onClick = { checkPermissionAndPickImage() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Image, contentDescription = "添加图片")
                Spacer(Modifier.width(8.dp))
                Text("添加回忆图片")
            }

            // 时间选择
            Text(
                text = "日记时间",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()).format(selectedDate.time),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { showDatePicker = true },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("修改日期")
                        }
                        TextButton(
                            onClick = { showTimePicker = true },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("修改时间")
                        }
                    }
                }
            }

            // 隐私级别选择
            Text(
                text = "隐私级别",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // 显示当前选中的隐私级别
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = privacyOptions.find { it.value == privacyLevel }?.label ?: "未知",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            
            // 隐私级别选项 - 优化显示方式
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(privacyOptions) { option ->
                    Card(
                        modifier = Modifier
                            .clickable { privacyLevel = option.value }
                            .padding(2.dp),
                        colors = if (option.value == privacyLevel) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (option.value == privacyLevel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
