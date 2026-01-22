package com.example.redxchartlibrary.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DrawingDao_Impl implements DrawingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Drawing> __insertionAdapterOfDrawing;

  private final DrawingTypeConverter __drawingTypeConverter = new DrawingTypeConverter();

  private final EntityDeletionOrUpdateAdapter<Drawing> __deletionAdapterOfDrawing;

  public DrawingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDrawing = new EntityInsertionAdapter<Drawing>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `drawings` (`id`,`tool`) VALUES (nullif(?, 0),?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Drawing entity) {
        statement.bindLong(1, entity.getId());
        final String _tmp = __drawingTypeConverter.fromDrawingTool(entity.getTool());
        statement.bindString(2, _tmp);
      }
    };
    this.__deletionAdapterOfDrawing = new EntityDeletionOrUpdateAdapter<Drawing>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `drawings` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Drawing entity) {
        statement.bindLong(1, entity.getId());
      }
    };
  }

  @Override
  public Object insertDrawing(final Drawing drawing, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDrawing.insert(drawing);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDrawing(final Drawing drawing, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDrawing.handle(drawing);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Drawing>> getAllDrawings() {
    final String _sql = "SELECT * FROM drawings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"drawings"}, new Callable<List<Drawing>>() {
      @Override
      @NonNull
      public List<Drawing> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTool = CursorUtil.getColumnIndexOrThrow(_cursor, "tool");
          final List<Drawing> _result = new ArrayList<Drawing>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Drawing _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final DrawingTool _tmpTool;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTool);
            _tmpTool = __drawingTypeConverter.toDrawingTool(_tmp);
            _item = new Drawing(_tmpId,_tmpTool);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
