package com.example.lovediary.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lovediary.data.dao.DiaryDao
import com.example.lovediary.data.dao.DiaryImageDao
import com.example.lovediary.data.dao.HighlightDao
import com.example.lovediary.data.entity.Diary
import com.example.lovediary.data.entity.DiaryImage
import com.example.lovediary.data.entity.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 日记数据库
 * 版本号：2
 * 实体类：Diary, DiaryImage, Highlight
 * DAO接口：DiaryDao, DiaryImageDao, HighlightDao
 */
@Database(
    entities = [Diary::class, DiaryImage::class, Highlight::class],
    version = 2,
    exportSchema = false
)
abstract class DiaryDatabase : RoomDatabase() {
    // DAO接口实例
    abstract fun diaryDao(): DiaryDao
    abstract fun diaryImageDao(): DiaryImageDao
    abstract fun highlightDao(): HighlightDao

    companion object {
        private const val NEW_DB_NAME = "love_diary_database"
        private const val OLD_DB_NAME = "diary_database"
        private const val TAG = "DiaryDatabase"

        @Volatile
        private var INSTANCE: DiaryDatabase? = null

        // 迁移专用协程作用域，IO线程 + SupervisorJob（子协程异常不影响父级）
        private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * v1 -> v2 迁移：新增 highlights 表（精选合集）
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS highlights (
                        id TEXT NOT NULL PRIMARY KEY,
                        imagePath TEXT NOT NULL,
                        title TEXT NOT NULL,
                        createTime TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * 获取数据库实例，自动检查并迁移旧数据库数据
         */
        fun getDatabase(context: Context): DiaryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiaryDatabase::class.java,
                    NEW_DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // 检查是否存在旧数据库，存在则异步迁移
                migrateOldDatabaseIfNeeded(context.applicationContext, instance)
                instance
            }
        }

        /**
         * 迁移旧数据库数据到新数据库
         * 检查旧数据库文件是否存在，若存在则读取数据并写入新数据库，完成后删除旧文件
         */
        private fun migrateOldDatabaseIfNeeded(context: Context, newDb: DiaryDatabase) {
            val dbPath = context.getDatabasePath(OLD_DB_NAME)
            if (!dbPath.exists()) return

            Log.d(TAG, "发现旧数据库文件，开始迁移数据...")
            migrationScope.launch {
                try {
                    val oldDb = Room.databaseBuilder(
                        context,
                        DiaryDatabase::class.java,
                        OLD_DB_NAME
                    )
                        .addMigrations(MIGRATION_1_2)
                        .build()

                    val oldDiaries = oldDb.diaryDao().getAllDiaries().first()
                    if (oldDiaries.isNotEmpty()) {
                        // 迁移日记数据
                        newDb.diaryDao().insertDiaries(oldDiaries)

                        // 迁移每条日记关联的图片
                        for (diary in oldDiaries) {
                            val images = oldDb.diaryImageDao().getImagesByDiaryId(diary.id)
                            if (images.isNotEmpty()) {
                                newDb.diaryImageDao().insertImages(images)
                            }
                        }
                        Log.d(TAG, "数据迁移完成：共迁移 ${oldDiaries.size} 条日记")
                    } else {
                        Log.d(TAG, "旧数据库无数据，跳过迁移")
                    }

                    oldDb.close()

                    // 删除旧数据库文件（含WAL和SHM临时文件）
                    context.getDatabasePath(OLD_DB_NAME).delete()
                    context.getDatabasePath("${OLD_DB_NAME}-wal").delete()
                    context.getDatabasePath("${OLD_DB_NAME}-shm").delete()
                    Log.d(TAG, "旧数据库文件已清理")
                } catch (e: Exception) {
                    Log.e(TAG, "数据迁移失败: ${e.message}", e)
                }
            }
        }
    }
}
