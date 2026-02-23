package com.example.lovediary.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.lovediary.R
import com.example.lovediary.ui.viewmodel.DiaryViewModel
import com.example.lovediary.utils.LinxiSyncManager
import java.net.NetworkInterface
import java.util.*

/**
 * 灵犀同步界面
 * 实现情侣间日记的双向同步功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinxiSyncScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    val context = LocalContext.current
    var syncStatus by remember { mutableStateOf(LinxiSyncManager.SyncStatus.IDLE) }
    var syncMessage by remember { mutableStateOf("准备就绪") }
    var progress by remember { mutableStateOf(Pair(0, 0)) } // current to total
    var hostMode by remember { mutableStateOf(false) }
    var clientIpAddress by remember { mutableStateOf("" as String) }
    
    // 初始化时获取本地IP地址
    LaunchedEffect(Unit) {
        clientIpAddress = getLocalIpAddressFunc()
    }
    var showIpDialog by remember { mutableStateOf(false) }
    var autoDetectNetwork by remember { mutableStateOf(true) }

    val syncManager = remember { 
        LinxiSyncManager(context, viewModel.diaryRepository) 
    }

    LaunchedEffect(syncStatus) {
        // 当同步完成时，刷新日记列表
        if (syncStatus == LinxiSyncManager.SyncStatus.COMPLETED) {
            viewModel.loadDiaries()
        }
    }

    // 自动检测网络连接状态
    LaunchedEffect(autoDetectNetwork) {
        if (autoDetectNetwork && syncStatus == LinxiSyncManager.SyncStatus.IDLE) {
            val localIp = getLocalIpAddressFunc()
            if (localIp.isNotEmpty() && localIp != "127.0.0.1") {
                clientIpAddress = localIp
                // 可以在这里添加自动连接逻辑
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("灵犀同步")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 状态显示区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (syncStatus) {
                            LinxiSyncManager.SyncStatus.IDLE -> Icons.Default.Sync
                            LinxiSyncManager.SyncStatus.CONNECTING -> Icons.Default.Wifi
                            LinxiSyncManager.SyncStatus.EXCHANGING_INDEX -> Icons.AutoMirrored.Filled.CompareArrows
                            LinxiSyncManager.SyncStatus.SYNCING -> Icons.Default.Sync
                            LinxiSyncManager.SyncStatus.COMPLETED -> Icons.Default.CheckCircle
                            LinxiSyncManager.SyncStatus.ERROR -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = when (syncStatus) {
                            LinxiSyncManager.SyncStatus.ERROR -> MaterialTheme.colorScheme.error
                            LinxiSyncManager.SyncStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when (syncStatus) {
                            LinxiSyncManager.SyncStatus.IDLE -> "准备就绪"
                            LinxiSyncManager.SyncStatus.CONNECTING -> "正在连接..."
                            LinxiSyncManager.SyncStatus.EXCHANGING_INDEX -> "正在交换日记索引..."
                            LinxiSyncManager.SyncStatus.SYNCING -> "正在同步日记..."
                            LinxiSyncManager.SyncStatus.COMPLETED -> "同步完成!"
                            LinxiSyncManager.SyncStatus.ERROR -> "同步出错"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = syncMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (progress.second > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (progress.first.toFloat() / progress.second),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${progress.first}/${progress.second}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    // 显示当前IP地址
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当前IP地址: $clientIpAddress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 操作按钮区域
            if (syncStatus == LinxiSyncManager.SyncStatus.IDLE) {
                // 角色选择
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "选择您的角色",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // 主机模式（等待连接）
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Button(
                                    onClick = {
                                        hostMode = true
                                        startSync(syncManager, true, "", viewModel, 
                                            onStatusChange = { status -> syncStatus = status },
                                            onMessageChange = { message -> syncMessage = message },
                                            onProgressChange = { current, total -> progress = current to total }
                                        )
                                    },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(Icons.Default.WifiTethering, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("等待连接")
                                }
                                Text(
                                    text = "作为主机等待对方连接",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            
                            // 客户端模式（连接主机）
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Button(
                                    onClick = { showIpDialog = true },
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("连接对方")
                                }
                                Text(
                                    text = "连接到对方设备",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                        
                        // 自动检测开关
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoDetectNetwork,
                                onCheckedChange = { autoDetectNetwork = it }
                            )
                            Text(
                                text = "自动检测网络状态",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // 一键同步按钮（适用于已连接网络的情况）
                Button(
                    onClick = {
                        // 在已知网络连接的情况下，尝试直接连接到常见主机IP
                        hostMode = false
                        startSync(syncManager, false, clientIpAddress, viewModel,
                            onStatusChange = { status -> syncStatus = status },
                            onMessageChange = { message -> syncMessage = message },
                            onProgressChange = { current, total -> progress = current to total }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("快速同步（已连接网络时使用）")
                }
            } else if (syncStatus != LinxiSyncManager.SyncStatus.COMPLETED) {
                // 同步进行中，显示取消按钮
                Button(
                    onClick = { 
                        syncManager.stopSync()
                        syncStatus = LinxiSyncManager.SyncStatus.IDLE
                        syncMessage = "同步已取消"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("取消同步")
                }
            } else {
                // 同步完成，显示完成和重新开始按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { navController.popBackStack() }
                    ) {
                        Text("返回主页")
                    }
                    
                    Button(
                        onClick = {
                            syncStatus = LinxiSyncManager.SyncStatus.IDLE
                            syncMessage = "准备就绪"
                            progress = 0 to 0
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重新同步")
                    }
                }
            }
            
            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "1. 确保两台设备已通过WiFi连接在同一网络中\n" +
                              "2. 一方选择「等待连接」，另一方选择「连接对方」\n" +
                              "3. 若已连接热点，可直接使用「快速同步」\n" +
                              "4. 连接成功后，系统将自动交换日记索引\n" +
                              "5. 比对完成后，自动同步缺失的日记\n" +
                              "6. 同步完成后，双方将拥有完整的日记内容",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // IP地址输入对话框
    if (showIpDialog) {
        var ipInput by remember { mutableStateOf("" as String) }
        
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            title = { Text("输入对方IP地址") },
            text = {
                Column {
                    Text("请输入对方设备的IP地址:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("IP地址") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "提示：通常热点主机的IP地址是192.168.43.1或其他类似地址",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clientIpAddress = ipInput
                        showIpDialog = false
                        
                        hostMode = false
                        startSync(syncManager, false, ipInput, viewModel,
                            onStatusChange = { status -> syncStatus = status },
                            onMessageChange = { message -> syncMessage = message },
                            onProgressChange = { current, total -> progress = current to total }
                        )
                    }
                ) {
                    Text("连接")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 获取本地IP地址
 */
private fun getLocalIpAddressFunc(): String {
    try {
        val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address.hostAddress?.contains(":") == false) {
                    return address.hostAddress ?: ""
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return ""
}

/**
 * 开始同步过程
 */
fun startSync(
    syncManager: LinxiSyncManager,
    isHost: Boolean,
    ipAddress: String,
    viewModel: DiaryViewModel,
    onStatusChange: (LinxiSyncManager.SyncStatus) -> Unit,
    onMessageChange: (String) -> Unit,
    onProgressChange: (Int, Int) -> Unit
) {
    onStatusChange(LinxiSyncManager.SyncStatus.CONNECTING)
    onMessageChange(if (isHost) "正在等待连接..." else "正在连接到对方...")
    
    val callback = object : LinxiSyncManager.SyncCallback {
        override fun onStatusChanged(status: LinxiSyncManager.SyncStatus) {
            onStatusChange(status)
        }
        
        override fun onProgress(progress: Int, total: Int) {
            onProgressChange(progress, total)
        }
        
        override fun onComplete(success: Boolean, message: String) {
            onMessageChange(message)
            if (success) {
                onStatusChange(LinxiSyncManager.SyncStatus.COMPLETED)
            } else {
                onStatusChange(LinxiSyncManager.SyncStatus.ERROR)
            }
        }
    }
    
    if (isHost) {
        syncManager.startAsHost(callback)
    } else {
        syncManager.startAsClient(ipAddress, callback)
    }
}
