package com.example.lovediary.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.lovediary.data.entity.Diary;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DiaryDao_Impl implements DiaryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Diary> __insertionAdapterOfDiary;

  private final EntityDeletionOrUpdateAdapter<Diary> __deletionAdapterOfDiary;

  private final EntityDeletionOrUpdateAdapter<Diary> __updateAdapterOfDiary;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllDiaries;

  public DiaryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDiary = new EntityInsertionAdapter<Diary>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `diaries` (`id`,`content`,`createTime`,`updateTime`,`privacyLevel`,`category`,`tags`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Diary entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getContent());
        statement.bindString(3, entity.getCreateTime());
        statement.bindString(4, entity.getUpdateTime());
        statement.bindLong(5, entity.getPrivacyLevel());
        statement.bindString(6, entity.getCategory());
        statement.bindString(7, entity.getTags());
      }
    };
    this.__deletionAdapterOfDiary = new EntityDeletionOrUpdateAdapter<Diary>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `diaries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Diary entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfDiary = new EntityDeletionOrUpdateAdapter<Diary>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `diaries` SET `id` = ?,`content` = ?,`createTime` = ?,`updateTime` = ?,`privacyLevel` = ?,`category` = ?,`tags` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Diary entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getContent());
        statement.bindString(3, entity.getCreateTime());
        statement.bindString(4, entity.getUpdateTime());
        statement.bindLong(5, entity.getPrivacyLevel());
        statement.bindString(6, entity.getCategory());
        statement.bindString(7, entity.getTags());
        statement.bindString(8, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllDiaries = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM diaries";
        return _query;
      }
    };
  }

  @Override
  public Object insertDiary(final Diary diary, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDiary.insert(diary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertDiaries(final List<Diary> diaries,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDiary.insert(diaries);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDiary(final Diary diary, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDiary.handle(diary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDiaries(final List<Diary> diaries,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDiary.handleMultiple(diaries);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDiary(final Diary diary, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDiary.handle(diary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllDiaries(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllDiaries.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllDiaries.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Diary>> getAllDiaries() {
    final String _sql = "SELECT * FROM diaries ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"diaries"}, new Callable<List<Diary>>() {
      @Override
      @NonNull
      public List<Diary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfUpdateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTime");
          final int _cursorIndexOfPrivacyLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "privacyLevel");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final List<Diary> _result = new ArrayList<Diary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Diary _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpCreateTime;
            _tmpCreateTime = _cursor.getString(_cursorIndexOfCreateTime);
            final String _tmpUpdateTime;
            _tmpUpdateTime = _cursor.getString(_cursorIndexOfUpdateTime);
            final int _tmpPrivacyLevel;
            _tmpPrivacyLevel = _cursor.getInt(_cursorIndexOfPrivacyLevel);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            _item = new Diary(_tmpId,_tmpContent,_tmpCreateTime,_tmpUpdateTime,_tmpPrivacyLevel,_tmpCategory,_tmpTags);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getDiaryById(final String id, final Continuation<? super Diary> $completion) {
    final String _sql = "SELECT * FROM diaries WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Diary>() {
      @Override
      @Nullable
      public Diary call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfUpdateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTime");
          final int _cursorIndexOfPrivacyLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "privacyLevel");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final Diary _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpCreateTime;
            _tmpCreateTime = _cursor.getString(_cursorIndexOfCreateTime);
            final String _tmpUpdateTime;
            _tmpUpdateTime = _cursor.getString(_cursorIndexOfUpdateTime);
            final int _tmpPrivacyLevel;
            _tmpPrivacyLevel = _cursor.getInt(_cursorIndexOfPrivacyLevel);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            _result = new Diary(_tmpId,_tmpContent,_tmpCreateTime,_tmpUpdateTime,_tmpPrivacyLevel,_tmpCategory,_tmpTags);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDiaryCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM diaries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Diary>> getDiariesByCategory(final String category) {
    final String _sql = "SELECT * FROM diaries WHERE category = ? ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"diaries"}, new Callable<List<Diary>>() {
      @Override
      @NonNull
      public List<Diary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfUpdateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTime");
          final int _cursorIndexOfPrivacyLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "privacyLevel");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final List<Diary> _result = new ArrayList<Diary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Diary _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpCreateTime;
            _tmpCreateTime = _cursor.getString(_cursorIndexOfCreateTime);
            final String _tmpUpdateTime;
            _tmpUpdateTime = _cursor.getString(_cursorIndexOfUpdateTime);
            final int _tmpPrivacyLevel;
            _tmpPrivacyLevel = _cursor.getInt(_cursorIndexOfPrivacyLevel);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            _item = new Diary(_tmpId,_tmpContent,_tmpCreateTime,_tmpUpdateTime,_tmpPrivacyLevel,_tmpCategory,_tmpTags);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Diary>> getDiariesByTag(final String tag) {
    final String _sql = "SELECT * FROM diaries WHERE tags LIKE '%' || ? || '%' ORDER BY createTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tag);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"diaries"}, new Callable<List<Diary>>() {
      @Override
      @NonNull
      public List<Diary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfCreateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "createTime");
          final int _cursorIndexOfUpdateTime = CursorUtil.getColumnIndexOrThrow(_cursor, "updateTime");
          final int _cursorIndexOfPrivacyLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "privacyLevel");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final List<Diary> _result = new ArrayList<Diary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Diary _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpCreateTime;
            _tmpCreateTime = _cursor.getString(_cursorIndexOfCreateTime);
            final String _tmpUpdateTime;
            _tmpUpdateTime = _cursor.getString(_cursorIndexOfUpdateTime);
            final int _tmpPrivacyLevel;
            _tmpPrivacyLevel = _cursor.getInt(_cursorIndexOfPrivacyLevel);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            _item = new Diary(_tmpId,_tmpContent,_tmpCreateTime,_tmpUpdateTime,_tmpPrivacyLevel,_tmpCategory,_tmpTags);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllCategories(final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT DISTINCT category FROM diaries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllTags(final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT DISTINCT tags FROM diaries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
