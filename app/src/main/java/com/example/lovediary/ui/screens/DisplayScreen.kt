package com.example.lovediary.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.lovediary.ui.viewmodel.DiaryViewModel

/**
 * 心情展示屏幕
 */
@Composable
fun DisplayScreen(
    navController: NavHostController,
    viewModel: DiaryViewModel
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "心情展示")
    }
}