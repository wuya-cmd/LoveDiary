package com.example.lovediary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 日记ViewModel
 */
class DiaryViewModel(
    val diaryRepository: DiaryRepository
) : ViewModel() {
    // 日记列表状态
    private val _diaries = MutableStateFlow<List<Diary>>(emptyList())
    val diaries: StateFlow<List<Diary>> = _diaries.asStateFlow()

    // 当前选中的日记
    private val _currentDiary = MutableStateFlow<Diary?>(null)
    val currentDiary: StateFlow<Diary?> = _currentDiary.asStateFlow()

    // 分类列表
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    // 标签列表
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    // 当前选择的分类
    private val _selectedCategory = MutableStateFlow<String>("默认分类")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // 当前选择的标签
    private val _selectedTag = MutableStateFlow<String>("")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    // 添加日记回调
    var onDiaryAdded: ((String) -> Unit)? = null

    init {
        loadDiaries()
        loadCategories()
        loadTags()
    }

    /**
     * 加载所有日记
     */
    fun loadDiaries() {
        viewModelScope.launch {
            diaryRepository.getAllDiaries().collect {
                _diaries.value = it
            }
        }
    }

    /**
     * 按分类加载日记
     */
    fun loadDiariesByCategory(category: String) {
        viewModelScope.launch {
            _selectedCategory.value = category
            _selectedTag.value = ""
            diaryRepository.getDiariesByCategory(category).collect {
                _diaries.value = it
            }
        }
    }

    /**
     * 按标签加载日记
     */
    fun loadDiariesByTag(tag: String) {
        viewModelScope.launch {
            _selectedTag.value = tag
            diaryRepository.getDiariesByTag(tag).collect {
                _diaries.value = it
            }
        }
    }

    /**
     * 加载所有分类
     */
    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = diaryRepository.getAllCategories()
        }
    }

    /**
     * 加载所有标签
     */
    fun loadTags() {
        viewModelScope.launch {
            _tags.value = diaryRepository.getAllTags()
        }
    }

    /**
     * 获取日记详情
     */
    fun getDiaryById(id: String) {
        viewModelScope.launch {
            _currentDiary.value = diaryRepository.getDiaryById(id)
        }
    }

    /**
     * 添加日记
     */
    fun addDiary(content: String, category: String = "默认分类", tags: String = "", privacyLevel: Int = 0) {
        viewModelScope.launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val diary = Diary(
                id = UUID.randomUUID().toString(),
                content = content,
                createTime = now,
                updateTime = now,
                privacyLevel = privacyLevel,
                category = category,
                tags = tags
            )
            val diaryId = diaryRepository.addDiary(diary)
            onDiaryAdded?.invoke(diaryId)
            loadDiaries()
            loadCategories()
            loadTags()
        }
    }

    // 为支持自定义创建时间添加新的添加方法
    fun addDiaryWithCreateTime(content: String, category: String = "默认分类", tags: String = "", privacyLevel: Int = 0, createTime: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val diary = Diary(
                id = UUID.randomUUID().toString(),
                content = content,
                createTime = createTime,
                updateTime = now,
                privacyLevel = privacyLevel,
                category = category,
                tags = tags
            )
            val diaryId = diaryRepository.addDiary(diary)
            onDiaryAdded?.invoke(diaryId)
            loadDiaries()
            loadCategories()
            loadTags()
        }
    }

    /**
     * 更新日记
     */
    fun updateDiary(id: String, content: String, category: String, tags: String, privacyLevel: Int) {
        viewModelScope.launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val diary = Diary(
                id = id,
                content = content,
                createTime = _currentDiary.value?.createTime ?: now,
                updateTime = now,
                privacyLevel = privacyLevel,
                category = category,
                tags = tags
            )
            diaryRepository.updateDiary(diary)
            loadDiaries()
            loadCategories()
            loadTags()
        }
    }

    // 为支持自定义创建时间添加新的更新方法
    fun updateDiaryWithCreateTime(id: String, content: String, category: String, tags: String, privacyLevel: Int, createTime: String) {
        viewModelScope.launch {
            val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val diary = Diary(
                id = id,
                content = content,
                createTime = createTime,
                updateTime = now,
                privacyLevel = privacyLevel,
                category = category,
                tags = tags
            )
            diaryRepository.updateDiary(diary)
            loadDiaries()
            loadCategories()
            loadTags()
        }
    }

    /**
     * 删除日记
     */
    fun deleteDiary(diary: Diary) {
        viewModelScope.launch {
            diaryRepository.deleteDiary(diary)
            loadDiaries()
            loadCategories()
            loadTags()
        }
    }

    /**
     * 重置当前日记
     */
    fun resetCurrentDiary() {
        _currentDiary.value = null
    }
    
    /**
     * 为日记添加图片
     */
    fun addImagesToDiary(diaryId: String, images: List<com.example.lovediary.data.entity.DiaryImage>) {
        viewModelScope.launch {
            diaryRepository.addImagesToDiary(diaryId, images)
        }
    }
}