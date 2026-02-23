package com.example.lovediary.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.security.PrivacyManager
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 编辑日记屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDiaryScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    val currentDiary by viewModel.currentDiary.collectAsState()
    var content by remember { mutableStateOf(currentDiary?.content ?: "") }
    var privacyLevel by remember { mutableStateOf(currentDiary?.privacyLevel ?: 0) }
    var isSaving by remember { mutableStateOf(false) } // 防止重复提交
    var selectedDate by remember { 
        mutableStateOf(
            currentDiary?.let { diary ->
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = format.parse(diary.createTime)
                    val calendar = Calendar.getInstance()
                    calendar.time = date ?: Date()
                    calendar
                } catch (e: Exception) {
                    Calendar.getInstance()
                }
            } ?: Calendar.getInstance()
        ) 
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // 日记图片相关状态
    var diaryImages by remember { mutableStateOf<List<DiaryImage>>(emptyList()) }
    val newImages = remember { mutableStateListOf<Uri>() }
    
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    
    val privacyOptions = viewModel.diaryRepository.privacyManagerPublic.getPrivacyLevelOptions()
    
    val context = LocalContext.current

    // 加载现有日记图片
    LaunchedEffect(currentDiary) {
        currentDiary?.let { diary ->
            diaryImages = viewModel.diaryRepository.getImagesByDiaryId(diary.id)
        }
    }
    
    // 图片选择launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            newImages.addAll(uris)
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
    
    // 移除新添加的图片
    fun removeNewImage(uri: Uri) {
        newImages.remove(uri)
    }
    
    // 移除已存在的图片
    fun removeExistingImage(image: DiaryImage) {
        // 在协程中调用挂起函数
        viewModel.viewModelScope.launch {
            viewModel.diaryRepository.deleteImage(image)
            diaryImages = diaryImages.filter { it.id != image.id }
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
                title = { Text(text = "编辑日记") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                                currentDiary?.let {
                                    viewModel.updateDiaryWithCreateTime(it.id, content, "默认分类", "", privacyLevel, dateTime)
                                    
                                    // 保存新增的图片
                                    if (newImages.isNotEmpty()) {
                                        // 这里应该调用一个方法来保存新图片
                                        // 但由于这是一个复杂的操作，我们在实际应用中可能需要重构这部分代码
                                    }
                                    
                                    navController.popBackStack()
                                }
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
        ) {
            // 日记内容
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(text = "日记内容") },
                placeholder = { Text(text = "写下你的心情...") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = 10
            )
            
            // 图片预览区域
            if (diaryImages.isNotEmpty() || newImages.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 显示已有图片
                    items(diaryImages) { image ->
                        Box {
                            Card(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { removeExistingImage(image) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                AsyncImage(
                                    model = File(image.imagePath),
                                    contentDescription = "日记图片",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // 删除按钮
                            IconButton(
                                onClick = { removeExistingImage(image) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "删除图片",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                    
                    // 显示新添加的图片
                    items(newImages) { imageUri ->
                        Box {
                            Card(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { removeNewImage(imageUri) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                                onClick = { removeNewImage(imageUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "删除图片",
                                    tint = Color.Red
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
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Image, contentDescription = "添加图片")
                Spacer(Modifier.width(8.dp))
                Text("添加图片")
            }

            // 时间显示
            Text(
                text = "日记时间",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
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
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("修改日期")
                        }
                        TextButton(onClick = { showTimePicker = true }) {
                            Text("修改时间")
                        }
                    }
                }
            }

            // 隐私级别选择
            Text(
                text = "隐私级别",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // 隐私级别选项 - 优化显示方式
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(privacyOptions) { option ->
                    Card(
                        modifier = Modifier
                            .clickable { privacyLevel = option.value }
                            .padding(4.dp),
                        colors = if (option.value == privacyLevel) {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Lock,
//                                    contentDescription = null,
//                                    modifier = Modifier.size(20.dp)
//                                )
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = option.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // 显示当前选中的隐私级别
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = privacyOptions.find { it.value == privacyLevel }?.label?: "未知",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}