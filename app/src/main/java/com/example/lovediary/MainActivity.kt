package com.example.lovediary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.lovediary.data.dao.DiaryDao
import com.example.lovediary.data.dao.DiaryImageDao
import com.example.lovediary.data.database.DiaryDatabase
import com.example.lovediary.data.repository.DiaryRepository
import com.example.lovediary.security.PrivacyManager
import com.example.lovediary.ui.screens.AddDiaryScreen
import com.example.lovediary.ui.screens.DiaryDetailScreen
import com.example.lovediary.ui.screens.DisplayScreen
import com.example.lovediary.ui.screens.EditDiaryScreen
import com.example.lovediary.ui.screens.HomeScreen
import com.example.lovediary.ui.screens.LoginScreen
import com.example.lovediary.ui.screens.LinxiSyncScreen
import com.example.lovediary.ui.theme.LoveDiaryTheme
import com.example.lovediary.ui.viewmodel.DiaryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化数据库
        val db = Room.databaseBuilder(
            applicationContext,
            DiaryDatabase::class.java,
            "love_diary_database"
        ).build()
        
        // 初始化DAO
        val diaryDao = db.diaryDao()
        val diaryImageDao = db.diaryImageDao()
        
        // 初始化隐私管理器
        val privacyManager = PrivacyManager(applicationContext)
        
        // 初始化仓库
        val diaryRepository = DiaryRepository(
            diaryDao,
            diaryImageDao,
            privacyManager
        )
        
        // 初始化ViewModel
        val diaryViewModel = DiaryViewModel(diaryRepository)
        
        setContent {
            LoveDiaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        navController = rememberNavController(),
                        diaryViewModel = diaryViewModel
                    )
                }
            }
        }
    }
}

/**
 * 应用导航
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    diaryViewModel: DiaryViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                navController = navController,
                viewModel = diaryViewModel
            )
        }
        composable("add_diary") {
            AddDiaryScreen(
                navController = navController,
                viewModel = diaryViewModel,
                onNavigateBack = {
                    navController.popBackStack("main", false)
                }
            )
        }
        composable("diary_detail") {
            DiaryDetailScreen(
                navController = navController,
                viewModel = diaryViewModel
            )
        }
        composable("edit_diary") {
            EditDiaryScreen(
                navController = navController,
                viewModel = diaryViewModel
            )
        }
        composable("login") {
            LoginScreen(
                navController = navController,
                viewModel = diaryViewModel
            )
        }
        composable("linxi_sync") {
            LinxiSyncScreen(
                navController = navController,
                viewModel = diaryViewModel
            )
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Home : BottomNavItem("home", "日记列表", { Icon(Icons.Filled.List, contentDescription = null) })
    object Display : BottomNavItem("display", "最近心情", { Icon(Icons.Filled.Favorite, contentDescription = null) })
    object Add : BottomNavItem("add_diary", "添加日记", { Icon(Icons.Filled.Add, contentDescription = null) })
}

@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Display,
        BottomNavItem.Add
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 10, 0, 0), // 1. 全部方向置 0
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = item.icon,
                        label = { Text(item.label) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> HomeScreen(navController, viewModel)
                1 -> DisplayScreen(navController, viewModel)
                2 -> AddDiaryScreen(
                    navController = navController,
                    viewModel = viewModel,
                    onNavigateBack = { 
                        // 返回主屏幕的第一个选项卡（日记列表）
                        selectedItem = 0
                    }
                )
                else -> HomeScreen(navController, viewModel)
            }
        }
    }
}