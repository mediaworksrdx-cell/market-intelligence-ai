package com.example.marketintelligence.data.source.local;

import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.marketintelligence.data.model.ExecutedTradeEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ExecutedTradeDao_Impl implements ExecutedTradeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ExecutedTradeEntity> __insertionAdapterOfExecutedTradeEntity;

  public ExecutedTradeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExecutedTradeEntity = new EntityInsertionAdapter<ExecutedTradeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `executed_trades` (`id`,`symbol`,`quantity`,`price`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExecutedTradeEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSymbol());
        statement.bindDouble(3, entity.getQuantity());
        statement.bindDouble(4, entity.getPrice());
        statement.bindLong(5, entity.getTimestamp());
      }
    };
  }

  @Override
  public Object insertTrade(final ExecutedTradeEntity trade,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExecutedTradeEntity.insert(trade);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
