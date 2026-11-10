package com.fundory.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fundory.app.data.local.dao.ExchangeRateDao
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.ExchangeRateEntity
import com.fundory.app.data.local.entity.GoalEntity
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.data.local.entity.ProductEntity
import com.fundory.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        GoalEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        ExchangeRateEntity::class,
        GoalSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FundoryDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun goalSnapshotDao(): GoalSnapshotDao

    companion object {
        const val DATABASE_NAME = "fundory_database.db"
    }
}
