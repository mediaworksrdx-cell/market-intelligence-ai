package com.marketintelligence.ai.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.marketintelligence.ai.data.source.local.*
import com.marketintelligence.redxchartlibrary.data.local.DrawingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate default indices
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('NIFTY 50', 'NIFTY 50', 'INDEX', 22500.0, 0.67, 256265)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('SENSEX', 'SENSEX', 'INDEX', 74000.0, 0.61, 265)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('BANKNIFTY', 'BANKNIFTY', 'INDEX', 48000.0, 1.04, 260105)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('FINNIFTY', 'FINNIFTY', 'INDEX', 21500.0, 0.47, 257801)")

                // Pre-populate default strategic stocks
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('RELIANCE.NS', 'Reliance Industries', 'STOCK', 2950.0, 1.0, 738561)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('HDFCBANK.NS', 'HDFC Bank', 'STOCK', 1550.0, -0.6, 341249)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('TCS.NS', 'Tata Consultancy', 'STOCK', 2200.0, -2.48, 2953217)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('ICICIBANK.NS', 'ICICI Bank', 'STOCK', 1120.0, 1.35, 1270529)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('SBIN.NS', 'State Bank of India', 'STOCK', 825.0, 0.86, 779521)")

                // Pre-populate top 6 default cryptos (BTC, ETH, DOGE, SOL, BNB, SHIB)
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('BTC', 'Bitcoin', 'CRYPTO', 67450.0, 2.14, 0)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('ETH', 'Ethereum', 'CRYPTO', 3520.0, 1.62, 0)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('DOGE', 'Dogecoin', 'CRYPTO', 0.125, 3.85, 0)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('SOL', 'Solana', 'CRYPTO', 148.50, 4.21, 0)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('BNB', 'BNB', 'CRYPTO', 585.0, -0.45, 0)")
                db.execSQL("INSERT INTO watchlist (symbol, name, type, price, changePercent, instrumentToken) VALUES ('SHIB', 'Shiba Inu', 'CRYPTO', 0.0000185, 2.90, 0)")
            }
        }).build()
    }

    @Provides
    fun provideCandleDao(appDatabase: AppDatabase): CandleDao {
        return appDatabase.candleDao()
    }

    @Provides
    fun provideWatchlistDao(appDatabase: AppDatabase): WatchlistDao {
        return appDatabase.watchlistDao()
    }

    @Provides
    fun provideInstrumentDao(appDatabase: AppDatabase): InstrumentDao {
        return appDatabase.instrumentDao()
    }

    @Provides
    fun provideDrawingDao(appDatabase: AppDatabase): DrawingDao {
        return appDatabase.drawingDao()
    }

    @Provides
    fun provideHoldingDao(appDatabase: AppDatabase): HoldingDao {
        return appDatabase.holdingDao()
    }

    @Provides
    fun provideTransactionDao(appDatabase: AppDatabase): TransactionDao {
        return appDatabase.transactionDao()
    }

    @Provides
    fun provideNotificationDao(appDatabase: AppDatabase): NotificationDao {
        return appDatabase.notificationDao()
    }

    @Provides
    fun provideExecutedTradeDao(appDatabase: AppDatabase): ExecutedTradeDao {
        return appDatabase.executedTradeDao()
    }
}
