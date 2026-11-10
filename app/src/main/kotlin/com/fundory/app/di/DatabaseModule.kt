package com.fundory.app.di

import android.content.Context
import androidx.room.Room
import com.fundory.app.data.local.FundoryDatabase
import com.fundory.app.data.local.dao.ExchangeRateDao
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.dao.ParcelDao
import com.fundory.app.data.local.dao.PriceHistoryDao
import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFundoryDatabase(
        @ApplicationContext context: Context
    ): FundoryDatabase {
        return Room.databaseBuilder(
            context,
            FundoryDatabase::class.java,
            FundoryDatabase.DATABASE_NAME
        )
            .addMigrations(FundoryDatabase.MIGRATION_1_2, FundoryDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun provideGoalDao(db: FundoryDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideProductDao(db: FundoryDatabase): ProductDao = db.productDao()

    @Provides
    fun provideTransactionDao(db: FundoryDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideExchangeRateDao(db: FundoryDatabase): ExchangeRateDao = db.exchangeRateDao()

    @Provides
    fun provideGoalSnapshotDao(db: FundoryDatabase): GoalSnapshotDao = db.goalSnapshotDao()

    @Provides
    fun providePriceHistoryDao(db: FundoryDatabase): PriceHistoryDao = db.priceHistoryDao()

    @Provides
    fun provideParcelDao(db: FundoryDatabase): ParcelDao = db.parcelDao()
}
