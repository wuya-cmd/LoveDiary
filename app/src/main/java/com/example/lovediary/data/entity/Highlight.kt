package com.example.lovediary.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 精选图片实体类（心情展示页历史合集条目）
 * @property id 唯一标识符
 * @property imagePath 图片本地存储路径
 * @property title 标题文字
 * @property createTime 创建时间（ISO格式字符串）
 */
@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey val id: String,
    val imagePath: String,
    val title: String,
    val createTime: String
)
