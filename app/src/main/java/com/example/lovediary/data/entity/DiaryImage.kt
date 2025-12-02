package com.example.lovediary.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * 日记图片实体类
 * @property id 图片唯一标识符
 * @property diaryId 关联的日记ID
 * @property imagePath 图片在本地存储的路径
 * @property originalPath 原始图片路径（可选）
 * @property compressed 是否经过压缩
 * @property compressionQuality 压缩质量（0-100）
 * @property originalSize 原始图片大小（字节）
 * @property compressedSize 压缩后图片大小（字节）
 * @property format 图片格式（JPEG/PNG/WebP等）
 */
@Entity(
    tableName = "diary_images",
    foreignKeys = [
        ForeignKey(
            entity = Diary::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DiaryImage(
    @PrimaryKey val id: String,
    val diaryId: String,
    val imagePath: String,
    val originalPath: String? = null,
    val compressed: Boolean = false,
    val compressionQuality: Int = 85,
    val originalSize: Long = 0,
    val compressedSize: Long = 0,
    val format: String = "JPEG"
)
