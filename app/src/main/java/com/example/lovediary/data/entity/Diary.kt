package com.example.lovediary.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日记实体类
 * @property id 日记唯一标识符
 * @property content 日记内容
 * @property createTime 创建时间（ISO格式字符串）
 * @property updateTime 更新时间（ISO格式字符串）
 * @property privacyLevel 隐私等级：0-公开，1-私密
 */
@Entity(tableName = "diaries")
data class Diary(
    @PrimaryKey val id: String,
    val content: String,
    val createTime: String,
    val updateTime: String,
    val privacyLevel: Int = 0,
    val category: String = "默认分类",
    val tags: String = ""
)
