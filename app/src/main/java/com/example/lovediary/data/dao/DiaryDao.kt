package com.example.lovediary.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lovediary.data.entity.Diary
import kotlinx.coroutines.flow.Flow

/**
 * 日记数据访问对象
 */
@Dao
interface DiaryDao {
    /**
     * 获取所有日记，按创建时间倒序排列
     */
    @Query("SELECT * FROM diaries ORDER BY createTime DESC")
    fun getAllDiaries(): Flow<List<Diary>>

    /**
     * 根据ID获取日记
     */
    @Query("SELECT * FROM diaries WHERE id = :id")
    suspend fun getDiaryById(id: String): Diary?

    /**
     * 插入日记
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: Diary)

    /**
     * 插入多个日记
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaries(diaries: List<Diary>)

    /**
     * 更新日记
     */
    @Update
    suspend fun updateDiary(diary: Diary)

    /**
     * 删除日记
     */
    @Delete
    suspend fun deleteDiary(diary: Diary)

    /**
     * 删除多个日记
     */
    @Delete
    suspend fun deleteDiaries(diaries: List<Diary>)

    /**
     * 删除所有日记
     */
    @Query("DELETE FROM diaries")
    suspend fun deleteAllDiaries()

    /**
     * 获取日记总数
     */
    @Query("SELECT COUNT(*) FROM diaries")
    suspend fun getDiaryCount(): Int

    /**
     * 根据分类获取日记
     */
    @Query("SELECT * FROM diaries WHERE category = :category ORDER BY createTime DESC")
    fun getDiariesByCategory(category: String): Flow<List<Diary>>

    /**
     * 根据标签获取日记
     */
    @Query("SELECT * FROM diaries WHERE tags LIKE '%' || :tag || '%' ORDER BY createTime DESC")
    fun getDiariesByTag(tag: String): Flow<List<Diary>>

    /**
     * 获取所有分类
     */
    @Query("SELECT DISTINCT category FROM diaries")
    suspend fun getAllCategories(): List<String>

    /**
     * 获取所有标签
     */
    @Query("SELECT DISTINCT tags FROM diaries")
    suspend fun getAllTags(): List<String>
}
