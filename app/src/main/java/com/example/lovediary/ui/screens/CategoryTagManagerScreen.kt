package com.example.lovediary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lovediary.ui.viewmodel.DiaryViewModel

/**
 * 分类和标签管理屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTagManagerScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val categories by viewModel.categories.collectAsState()
    val tags by viewModel.tags.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var newItem by remember { mutableStateOf("") }
    
    // 返回操作
    fun goBack() {
        if (onNavigateBack != null) {
            onNavigateBack()
        } else {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "分类和标签管理") },
                navigationIcon = {
                    IconButton(onClick = { 
                        // 直接使用navController.popBackStack()确保返回功能正常工作
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
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
            // 标签页
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(text = "分类") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(text = "标签") }
                )
            }

            // 内容区域
            when (selectedTab) {
                0 -> {
                    // 分类管理
                    CategoryManager(
                        categories = categories,
                        newCategory = newItem,
                        onNewCategoryChange = { newItem = it },
                        onAddCategory = {
                            if (it.isNotBlank() && !categories.contains(it)) {
                                // 添加新分类（通过添加一篇使用该分类的日记来实现）
                                viewModel.addDiary(
                                    content = "",
                                    category = it,
                                    tags = "",
                                    privacyLevel = 0
                                )
                                newItem = ""
                            }
                        },
                        onDeleteCategory = {
                            // 删除分类（这里只是演示，实际应用中需要处理关联的日记）
                            // 可以将使用该分类的日记移到默认分类
                        }
                    )
                }
                1 -> {
                    // 标签管理
                    TagManager(
                        tags = tags,
                        newTag = newItem,
                        onNewTagChange = { newItem = it },
                        onAddTag = {
                            if (it.isNotBlank() && !tags.contains(it)) {
                                // 添加新标签（通过添加一篇使用该标签的日记来实现）
                                viewModel.addDiary(
                                    content = "",
                                    category = "默认分类",
                                    tags = it,
                                    privacyLevel = 0
                                )
                                newItem = ""
                            }
                        },
                        onDeleteTag = {
                            // 删除标签（这里只是演示，实际应用中需要处理关联的日记）
                            // 可以从所有日记中移除该标签
                        }
                    )
                }
            }
        }
    }
}

/**
 * 分类管理组件
 */
@Composable
fun CategoryManager(
    categories: List<String>,
    newCategory: String,
    onNewCategoryChange: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 添加新分类
        TextField(
            value = newCategory,
            onValueChange = onNewCategoryChange,
            label = { Text(text = "新分类") },
            placeholder = { Text(text = "输入新分类名称") },
            trailingIcon = {
                IconButton(onClick = { onAddCategory(newCategory) }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // 分类列表
        LazyColumn {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    onDelete = { onDeleteCategory(category) }
                )
            }
        }
    }
}

/**
 * 分类项组件
 */
@Composable
fun CategoryItem(
    category: String,
    onDelete: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* 编辑分类 */ },
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除")
        }
    }
}

/**
 * 标签管理组件
 */
@Composable
fun TagManager(
    tags: List<String>,
    newTag: String,
    onNewTagChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 添加新标签
        TextField(
            value = newTag,
            onValueChange = onNewTagChange,
            label = { Text(text = "新标签") },
            placeholder = { Text(text = "输入新标签名称") },
            trailingIcon = {
                IconButton(onClick = { onAddTag(newTag) }) {
                    Icon(Icons.Filled.Add, contentDescription = "添加")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // 标签列表
        LazyColumn {
            items(tags) { tag ->
                TagItem(
                    tag = tag,
                    onDelete = { onDeleteTag(tag) }
                )
            }
        }
    }
}

/**
 * 标签项组件
 */
@Composable
fun TagItem(
    tag: String,
    onDelete: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { /* 编辑标签 */ },
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "删除")
        }
    }
}