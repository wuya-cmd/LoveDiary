package com.example.lovediary.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lovediary.data.dao.DiaryDao
import com.example.lovediary.data.dao.DiaryImageDao
import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.entity.DiaryImage

/**
 * 日记数据库
 * 版本号：1
 * 实体类：Diary, DiaryImage
 * DAO接口：DiaryDao, DiaryImageDao
 */
@Database(
    entities = [Diary::class, DiaryImage::class],
    version = 1,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    // DAO接口实例
    abstract fun diaryDao(): DiaryDao
    abstract fun diaryImageDao(): DiaryImageDao

    companion object {
        // 单例模式，避免重复创建数据库实例
        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        /**
         * 获取数据库实例
         */
        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    "love_diary_database"
                )
                    .fallbackToDestructiveMigration() // 数据库版本升级时销毁旧数据（生产环境应使用迁移策略）
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
