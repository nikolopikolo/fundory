package com.fundory.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fundory.app.data.local.dao.ExchangeRateDao
import com.fundory.app.data.local.dao.GoalDao
import com.fundory.app.data.local.dao.GoalSnapshotDao
import com.fundory.app.data.local.dao.ParcelDao
import com.fundory.app.data.local.dao.PriceHistoryDao
import com.fundory.app.data.local.dao.ProductDao
import com.fundory.app.data.local.dao.TransactionDao
import com.fundory.app.data.local.entity.ExchangeRateEntity
import com.fundory.app.data.local.entity.GoalEntity
import com.fundory.app.data.local.entity.GoalSnapshotEntity
import com.fundory.app.data.local.entity.ParcelEntity
import com.fundory.app.data.local.entity.PriceHistoryEntity
import com.fundory.app.data.local.entity.ProductEntity
import com.fundory.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        GoalEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        ExchangeRateEntity::class,
        GoalSnapshotEntity::class,
        PriceHistoryEntity::class,
        ParcelEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FundoryDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun goalSnapshotDao(): GoalSnapshotDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun parcelDao(): ParcelDao

    companion object {
        const val DATABASE_NAME = "fundory_database.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN originalTargetAmount TEXT NOT NULL DEFAULT '0.00'")
                db.execSQL("ALTER TABLE goals ADD COLUMN targetCurrency TEXT NOT NULL DEFAULT 'GEL'")

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Recreate products table to safely support nullable goalId (wishlist) and new tracking fields
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS products_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        goalId TEXT,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        originalPrice TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        productUrl TEXT,
                        imageUrl TEXT,
                        shippingCost TEXT NOT NULL,
                        shippingCurrency TEXT NOT NULL,
                        taxesAndFees TEXT NOT NULL,
                        taxesCurrency TEXT NOT NULL,
                        manualOverrideGel TEXT,
                        sortOrder INTEGER NOT NULL,
                        dateAddedEpochMillis INTEGER NOT NULL,
                        lastEditedEpochMillis INTEGER NOT NULL,
                        purchaseStatus TEXT NOT NULL DEFAULT 'NOT_PURCHASED',
                        actualPrice TEXT,
                        actualPriceCurrency TEXT,
                        shippingCarrier TEXT,
                        trackingNumber TEXT,
                        expectedDeliveryDate TEXT,
                        actualDeliveryDate TEXT,
                        trackingNotes TEXT,
                        alternativeGroupId TEXT,
                        isSelectedAlternative INTEGER NOT NULL DEFAULT 1,
                        parcelId TEXT,
                        marketplace TEXT,
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO products_new (
                        id, goalId, name, description, originalPrice, currency, quantity,
                        productUrl, imageUrl, shippingCost, shippingCurrency, taxesAndFees, taxesCurrency,
                        manualOverrideGel, sortOrder, dateAddedEpochMillis, lastEditedEpochMillis
                    )
                    SELECT 
                        id, goalId, name, description, originalPrice, currency, quantity,
                        productUrl, imageUrl, shippingCost, shippingCurrency, taxesAndFees, taxesCurrency,
                        manualOverrideGel, sortOrder, dateAddedEpochMillis, lastEditedEpochMillis
                    FROM products
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE products")
                db.execSQL("ALTER TABLE products_new RENAME TO products")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_goalId ON products(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_alternativeGroupId ON products(alternativeGroupId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_parcelId ON products(parcelId)")

                // 2. Create price_history table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS price_history (
                        id TEXT NOT NULL PRIMARY KEY,
                        productId TEXT NOT NULL,
                        price TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        recordedAtEpochMillis INTEGER NOT NULL,
                        note TEXT,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_price_history_productId ON price_history(productId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_price_history_recordedAtEpochMillis ON price_history(recordedAtEpochMillis)")

                // 3. Create parcels table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS parcels (
                        id TEXT NOT NULL PRIMARY KEY,
                        goalId TEXT,
                        name TEXT NOT NULL,
                        carrier TEXT NOT NULL,
                        method TEXT NOT NULL,
                        weightKg REAL NOT NULL,
                        lengthCm REAL,
                        widthCm REAL,
                        heightCm REAL,
                        shippingCostGel TEXT NOT NULL,
                        isConfirmed INTEGER NOT NULL DEFAULT 0,
                        trackingNumber TEXT,
                        createdAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_parcels_goalId ON parcels(goalId)")
            }
        }
    }
}
