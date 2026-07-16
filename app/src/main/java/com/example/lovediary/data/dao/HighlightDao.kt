package com.example.lovediary.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lovediary.data.entity.Highlight
import kotlinx.coroutines.flow.Flow

/**
 * 精选图片数据访问对象
 */
@Dao
interface HighlightDao {
    /**
     * 获取所有精选，按创建时间倒序排列（最新的在前）
     */
    @Query("SELECT * FROM highlights ORDER BY createTime DESC")
    fun getAllHighlights(): Flow<List<Highlight>>

    /**
     * 根据ID获取精选
     */
    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun getHighlightById(id: String): Highlight?

    /**
     * 插入精选
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: Highlight)

    /**
     * 删除精选
     */
    @Delete
    suspend fun deleteHighlight(highlight: Highlight)

    /**
     * 根据ID删除精选
     */
    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: String)

    /**
     * 获取精选总数
     */
    @Query("SELECT COUNT(*) FROM highlights")
    suspend fun getHighlightCount(): Int
}
