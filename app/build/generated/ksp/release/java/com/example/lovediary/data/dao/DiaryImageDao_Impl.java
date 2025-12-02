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
import com.example.lovediary.data.entity.DiaryImage;
import java.lang.Class;
import java.lang.Exception;
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

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DiaryImageDao_Impl implements DiaryImageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DiaryImage> __insertionAdapterOfDiaryImage;

  private final EntityDeletionOrUpdateAdapter<DiaryImage> __deletionAdapterOfDiaryImage;

  private final EntityDeletionOrUpdateAdapter<DiaryImage> __updateAdapterOfDiaryImage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteImagesByDiaryId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllImages;

  public DiaryImageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDiaryImage = new EntityInsertionAdapter<DiaryImage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `diary_images` (`id`,`diaryId`,`imagePath`,`originalPath`,`compressed`,`compressionQuality`,`originalSize`,`compressedSize`,`format`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DiaryImage entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDiaryId());
        statement.bindString(3, entity.getImagePath());
        if (entity.getOriginalPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getOriginalPath());
        }
        final int _tmp = entity.getCompressed() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCompressionQuality());
        statement.bindLong(7, entity.getOriginalSize());
        statement.bindLong(8, entity.getCompressedSize());
        statement.bindString(9, entity.getFormat());
      }
    };
    this.__deletionAdapterOfDiaryImage = new EntityDeletionOrUpdateAdapter<DiaryImage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `diary_images` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DiaryImage entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfDiaryImage = new EntityDeletionOrUpdateAdapter<DiaryImage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `diary_images` SET `id` = ?,`diaryId` = ?,`imagePath` = ?,`originalPath` = ?,`compressed` = ?,`compressionQuality` = ?,`originalSize` = ?,`compressedSize` = ?,`format` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DiaryImage entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDiaryId());
        statement.bindString(3, entity.getImagePath());
        if (entity.getOriginalPath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getOriginalPath());
        }
        final int _tmp = entity.getCompressed() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCompressionQuality());
        statement.bindLong(7, entity.getOriginalSize());
        statement.bindLong(8, entity.getCompressedSize());
        statement.bindString(9, entity.getFormat());
        statement.bindString(10, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteImagesByDiaryId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM diary_images WHERE diaryId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllImages = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM diary_images";
        return _query;
      }
    };
  }

  @Override
  public Object insertImage(final DiaryImage image, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDiaryImage.insert(image);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertImages(final List<DiaryImage> images,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDiaryImage.insert(images);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteImage(final DiaryImage image, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDiaryImage.handle(image);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteImages(final List<DiaryImage> images,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDiaryImage.handleMultiple(images);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateImage(final DiaryImage image, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDiaryImage.handle(image);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteImagesByDiaryId(final String diaryId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteImagesByDiaryId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, diaryId);
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
          __preparedStmtOfDeleteImagesByDiaryId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllImages(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllImages.acquire();
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
          __preparedStmtOfDeleteAllImages.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getImagesByDiaryId(final String diaryId,
      final Continuation<? super List<DiaryImage>> $completion) {
    final String _sql = "SELECT * FROM diary_images WHERE diaryId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, diaryId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DiaryImage>>() {
      @Override
      @NonNull
      public List<DiaryImage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDiaryId = CursorUtil.getColumnIndexOrThrow(_cursor, "diaryId");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfOriginalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPath");
          final int _cursorIndexOfCompressed = CursorUtil.getColumnIndexOrThrow(_cursor, "compressed");
          final int _cursorIndexOfCompressionQuality = CursorUtil.getColumnIndexOrThrow(_cursor, "compressionQuality");
          final int _cursorIndexOfOriginalSize = CursorUtil.getColumnIndexOrThrow(_cursor, "originalSize");
          final int _cursorIndexOfCompressedSize = CursorUtil.getColumnIndexOrThrow(_cursor, "compressedSize");
          final int _cursorIndexOfFormat = CursorUtil.getColumnIndexOrThrow(_cursor, "format");
          final List<DiaryImage> _result = new ArrayList<DiaryImage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DiaryImage _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDiaryId;
            _tmpDiaryId = _cursor.getString(_cursorIndexOfDiaryId);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpOriginalPath;
            if (_cursor.isNull(_cursorIndexOfOriginalPath)) {
              _tmpOriginalPath = null;
            } else {
              _tmpOriginalPath = _cursor.getString(_cursorIndexOfOriginalPath);
            }
            final boolean _tmpCompressed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompressed);
            _tmpCompressed = _tmp != 0;
            final int _tmpCompressionQuality;
            _tmpCompressionQuality = _cursor.getInt(_cursorIndexOfCompressionQuality);
            final long _tmpOriginalSize;
            _tmpOriginalSize = _cursor.getLong(_cursorIndexOfOriginalSize);
            final long _tmpCompressedSize;
            _tmpCompressedSize = _cursor.getLong(_cursorIndexOfCompressedSize);
            final String _tmpFormat;
            _tmpFormat = _cursor.getString(_cursorIndexOfFormat);
            _item = new DiaryImage(_tmpId,_tmpDiaryId,_tmpImagePath,_tmpOriginalPath,_tmpCompressed,_tmpCompressionQuality,_tmpOriginalSize,_tmpCompressedSize,_tmpFormat);
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
  public Object getImageById(final String id, final Continuation<? super DiaryImage> $completion) {
    final String _sql = "SELECT * FROM diary_images WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DiaryImage>() {
      @Override
      @Nullable
      public DiaryImage call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDiaryId = CursorUtil.getColumnIndexOrThrow(_cursor, "diaryId");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfOriginalPath = CursorUtil.getColumnIndexOrThrow(_cursor, "originalPath");
          final int _cursorIndexOfCompressed = CursorUtil.getColumnIndexOrThrow(_cursor, "compressed");
          final int _cursorIndexOfCompressionQuality = CursorUtil.getColumnIndexOrThrow(_cursor, "compressionQuality");
          final int _cursorIndexOfOriginalSize = CursorUtil.getColumnIndexOrThrow(_cursor, "originalSize");
          final int _cursorIndexOfCompressedSize = CursorUtil.getColumnIndexOrThrow(_cursor, "compressedSize");
          final int _cursorIndexOfFormat = CursorUtil.getColumnIndexOrThrow(_cursor, "format");
          final DiaryImage _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDiaryId;
            _tmpDiaryId = _cursor.getString(_cursorIndexOfDiaryId);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final String _tmpOriginalPath;
            if (_cursor.isNull(_cursorIndexOfOriginalPath)) {
              _tmpOriginalPath = null;
            } else {
              _tmpOriginalPath = _cursor.getString(_cursorIndexOfOriginalPath);
            }
            final boolean _tmpCompressed;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfCompressed);
            _tmpCompressed = _tmp != 0;
            final int _tmpCompressionQuality;
            _tmpCompressionQuality = _cursor.getInt(_cursorIndexOfCompressionQuality);
            final long _tmpOriginalSize;
            _tmpOriginalSize = _cursor.getLong(_cursorIndexOfOriginalSize);
            final long _tmpCompressedSize;
            _tmpCompressedSize = _cursor.getLong(_cursorIndexOfCompressedSize);
            final String _tmpFormat;
            _tmpFormat = _cursor.getString(_cursorIndexOfFormat);
            _result = new DiaryImage(_tmpId,_tmpDiaryId,_tmpImagePath,_tmpOriginalPath,_tmpCompressed,_tmpCompressionQuality,_tmpOriginalSize,_tmpCompressedSize,_tmpFormat);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
