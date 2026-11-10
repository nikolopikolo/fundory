package com.fundory.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN originalTargetAmount TEXT NOT NULL DEFAULT '0.00'")
                db.execSQL("ALTER TABLE goals ADD COLUMN targetCurrency TEXT NOT NULL DEFAULT 'GEL'")

                // Recreate transactions table to safely allow nullable goalId
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        goalId TEXT,
                        type TEXT NOT NULL,
                        amountGel TEXT NOT NULL,
                        targetGoalId TEXT,
                        timestampEpochMillis INTEGER NOT NULL,
                        note TEXT,
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO transactions_new (id, goalId, type, amountGel, targetGoalId, timestampEpochMillis, note)
                    SELECT id, goalId, type, amountGel, targetGoalId, timestampEpochMillis, note FROM transactions
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_goalId ON transactions(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestampEpochMillis ON transactions(timestampEpochMillis)")
            }
        }
    }
}
