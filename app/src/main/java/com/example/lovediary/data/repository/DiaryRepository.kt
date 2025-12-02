package com.example.lovediary.data.repository

import com.example.lovediary.data.dao.DiaryDao
import com.example.lovediary.data.dao.DiaryImageDao
import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.security.PrivacyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 日记仓库类
 * 统一管理日记和图片的数据访问操作
 */
class DiaryRepository(
    private val diaryDao: DiaryDao,
    private val diaryImageDao: DiaryImageDao,
    private val privacyManager: PrivacyManager
) {
    /**
     * 获取所有日记，按创建时间倒序排列，带隐私过滤
     */
    fun getAllDiaries(): Flow<List<Diary>> {
        return diaryDao.getAllDiaries().map {
            privacyManager.filterDiaries(it)
        }
    }

    /**
     * 根据ID获取日记，带隐私过滤
     */
    suspend fun getDiaryById(id: String): Diary? {
        val diary = diaryDao.getDiaryById(id)
        return diary?.let {
            privacyManager.filterDiaries(listOf(it)).firstOrNull()
        }
    }

    /**
     * 添加日记
     * @param diary 日记对象
     * @return 添加后的日记ID
     */
    suspend fun addDiary(diary: Diary): String {
        // 处理隐私（加密）
        val processedDiary = privacyManager.processDiaryForSave(diary)
        // 插入数据库
        diaryDao.insertDiary(processedDiary)
        return processedDiary.id
    }

    /**
     * 更新日记
     * @param diary 日记对象
     */
    suspend fun updateDiary(diary: Diary) {
        // 处理隐私（加密）
        val processedDiary = privacyManager.processDiaryForSave(diary)
        // 更新数据库
        diaryDao.updateDiary(processedDiary)
    }

    /**
     * 删除日记
     * @param diary 日记对象
     */
    suspend fun deleteDiary(diary: Diary) {
        // 删除日记（级联删除相关图片）
        diaryDao.deleteDiary(diary)
    }

    /**
     * 为日记添加图片
     * @param diaryId 日记ID
     * @param image 图片对象
     */
    suspend fun addImageToDiary(diaryId: String, image: DiaryImage) {
        diaryImageDao.insertImage(image)
    }

    /**
     * 为日记添加多张图片
     * @param diaryId 日记ID
     * @param images 图片列表
     */
    suspend fun addImagesToDiary(diaryId: String, images: List<DiaryImage>) {
        diaryImageDao.insertImages(images)
    }

    /**
     * 删除日记的图片
     * @param image 图片对象
     */
    suspend fun deleteImage(image: DiaryImage) {
        diaryImageDao.deleteImage(image)
    }

    /**
     * 删除日记的多张图片
     * @param images 图片列表
     */
    suspend fun deleteImages(images: List<DiaryImage>) {
        diaryImageDao.deleteImages(images)
    }

    /**
     * 获取日记的所有图片
     * @param diaryId 日记ID
     */
    suspend fun getImagesByDiaryId(diaryId: String): List<DiaryImage> {
        return diaryImageDao.getImagesByDiaryId(diaryId)
    }

    /**
     * 获取所有日记（原始数据，无隐私过滤）
     */
    suspend fun getAllDiariesRaw(): List<Diary> {
        return diaryDao.getAllDiaries().first()
    }

    /**
     * 批量插入日记（用于导入）
     * @param diaries 日记列表
     */
    suspend fun insertDiaries(diaries: List<Diary>) {
        diaryDao.insertDiaries(diaries)
    }

    /**
     * 根据分类获取日记，带隐私过滤
     */
    fun getDiariesByCategory(category: String): Flow<List<Diary>> {
        return diaryDao.getDiariesByCategory(category).map {
            privacyManager.filterDiaries(it)
        }
    }

    /**
     * 根据标签获取日记，带隐私过滤
     */
    fun getDiariesByTag(tag: String): Flow<List<Diary>> {
        return diaryDao.getDiariesByTag(tag).map {
            privacyManager.filterDiaries(it)
        }
    }

    /**
     * 获取所有分类
     */
    suspend fun getAllCategories(): List<String> {
        return diaryDao.getAllCategories()
    }

    /**
     * 获取所有标签
     */
    suspend fun getAllTags(): List<String> {
        val allTags = diaryDao.getAllTags()
        // 处理标签字符串，分割并去重
        return allTags.flatMap { it.split("，", ",", " ") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // 公共访问器，允许外部访问隐私管理器
    val privacyManagerPublic: PrivacyManager get() = privacyManager
    
}
