package com.example.lovediary.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lovediary.data.entity.DiaryImage

/**
 * 日记图片数据访问对象
 */
@Dao
interface DiaryImageDao {
    /**
     * 根据日记ID获取所有图片
     */
    @Query("SELECT * FROM diary_images WHERE diaryId = :diaryId")
    suspend fun getImagesByDiaryId(diaryId: String): List<DiaryImage>

    /**
     * 根据ID获取图片
     */
    @Query("SELECT * FROM diary_images WHERE id = :id")
    suspend fun getImageById(id: String): DiaryImage?

    /**
     * 插入图片
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: DiaryImage)

    /**
     * 插入多个图片
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<DiaryImage>)

    /**
     * 更新图片
     */
    @Update
    suspend fun updateImage(image: DiaryImage)

    /**
     * 删除图片
     */
    @Delete
    suspend fun deleteImage(image: DiaryImage)

    /**
     * 删除多个图片
     */
    @Delete
    suspend fun deleteImages(images: List<DiaryImage>)

    /**
     * 根据日记ID删除所有图片
     */
    @Query("DELETE FROM diary_images WHERE diaryId = :diaryId")
    suspend fun deleteImagesByDiaryId(diaryId: String)

    /**
     * 删除所有图片
     */
    @Query("DELETE FROM diary_images")
    suspend fun deleteAllImages()
}
