package com.example.marketintelligence.data.source.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
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
public final class HoldingDao_Impl implements HoldingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HoldingEntity> __insertionAdapterOfHoldingEntity;

  public HoldingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHoldingEntity = new EntityInsertionAdapter<HoldingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `holdings` (`id`,`symbol`,`quantity`,`avgPrice`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HoldingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSymbol());
        statement.bindDouble(3, entity.getQuantity());
        statement.bindDouble(4, entity.getAvgPrice());
      }
    };
  }

  @Override
  public Object insert(final HoldingEntity holding, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHoldingEntity.insert(holding);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HoldingEntity>> getAllHoldings() {
    final String _sql = "SELECT * FROM holdings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"holdings"}, new Callable<List<HoldingEntity>>() {
      @Override
      @NonNull
      public List<HoldingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfAvgPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "avgPrice");
          final List<HoldingEntity> _result = new ArrayList<HoldingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HoldingEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final double _tmpAvgPrice;
            _tmpAvgPrice = _cursor.getDouble(_cursorIndexOfAvgPrice);
            _item = new HoldingEntity(_tmpId,_tmpSymbol,_tmpQuantity,_tmpAvgPrice);
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
