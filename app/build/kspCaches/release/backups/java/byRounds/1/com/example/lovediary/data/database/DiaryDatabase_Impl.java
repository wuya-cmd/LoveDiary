package com.example.lovediary.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.example.lovediary.data.dao.DiaryDao;
import com.example.lovediary.data.dao.DiaryDao_Impl;
import com.example.lovediary.data.dao.DiaryImageDao;
import com.example.lovediary.data.dao.DiaryImageDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DiaryDatabase_Impl extends DiaryDatabase {
  private volatile DiaryDao _diaryDao;

  private volatile DiaryImageDao _diaryImageDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `diaries` (`id` TEXT NOT NULL, `content` TEXT NOT NULL, `createTime` TEXT NOT NULL, `updateTime` TEXT NOT NULL, `privacyLevel` INTEGER NOT NULL, `category` TEXT NOT NULL, `tags` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `diary_images` (`id` TEXT NOT NULL, `diaryId` TEXT NOT NULL, `imagePath` TEXT NOT NULL, `originalPath` TEXT, `compressed` INTEGER NOT NULL, `compressionQuality` INTEGER NOT NULL, `originalSize` INTEGER NOT NULL, `compressedSize` INTEGER NOT NULL, `format` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`diaryId`) REFERENCES `diaries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '2d928d9d9bd7d90e37145a8997814ad2')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `diaries`");
        db.execSQL("DROP TABLE IF EXISTS `diary_images`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsDiaries = new HashMap<String, TableInfo.Column>(7);
        _columnsDiaries.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaries.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaries.put("createTime", new TableInfo.Column("createTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaries.put("updateTime", new TableInfo.Column("updateTime", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaries.put("privacyLevel", new TableInfo.Column("privacyLevel", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaries.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaries.put("tags", new TableInfo.Column("tags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDiaries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDiaries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDiaries = new TableInfo("diaries", _columnsDiaries, _foreignKeysDiaries, _indicesDiaries);
        final TableInfo _existingDiaries = TableInfo.read(db, "diaries");
        if (!_infoDiaries.equals(_existingDiaries)) {
          return new RoomOpenHelper.ValidationResult(false, "diaries(com.example.lovediary.data.entity.Diary).\n"
                  + " Expected:\n" + _infoDiaries + "\n"
                  + " Found:\n" + _existingDiaries);
        }
        final HashMap<String, TableInfo.Column> _columnsDiaryImages = new HashMap<String, TableInfo.Column>(9);
        _columnsDiaryImages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("diaryId", new TableInfo.Column("diaryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("imagePath", new TableInfo.Column("imagePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("originalPath", new TableInfo.Column("originalPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("compressed", new TableInfo.Column("compressed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("compressionQuality", new TableInfo.Column("compressionQuality", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("originalSize", new TableInfo.Column("originalSize", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("compressedSize", new TableInfo.Column("compressedSize", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiaryImages.put("format", new TableInfo.Column("format", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDiaryImages = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysDiaryImages.add(new TableInfo.ForeignKey("diaries", "CASCADE", "NO ACTION", Arrays.asList("diaryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesDiaryImages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDiaryImages = new TableInfo("diary_images", _columnsDiaryImages, _foreignKeysDiaryImages, _indicesDiaryImages);
        final TableInfo _existingDiaryImages = TableInfo.read(db, "diary_images");
        if (!_infoDiaryImages.equals(_existingDiaryImages)) {
          return new RoomOpenHelper.ValidationResult(false, "diary_images(com.example.lovediary.data.entity.DiaryImage).\n"
                  + " Expected:\n" + _infoDiaryImages + "\n"
                  + " Found:\n" + _existingDiaryImages);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "2d928d9d9bd7d90e37145a8997814ad2", "62d9d60be4c98ef9f234f7e9fa22666b");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "diaries","diary_images");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `diaries`");
      _db.execSQL("DELETE FROM `diary_images`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(DiaryDao.class, DiaryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DiaryImageDao.class, DiaryImageDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public DiaryDao diaryDao() {
    if (_diaryDao != null) {
      return _diaryDao;
    } else {
      synchronized(this) {
        if(_diaryDao == null) {
          _diaryDao = new DiaryDao_Impl(this);
        }
        return _diaryDao;
      }
    }
  }

  @Override
  public DiaryImageDao diaryImageDao() {
    if (_diaryImageDao != null) {
      return _diaryImageDao;
    } else {
      synchronized(this) {
        if(_diaryImageDao == null) {
          _diaryImageDao = new DiaryImageDao_Impl(this);
        }
        return _diaryImageDao;
      }
    }
  }
}
