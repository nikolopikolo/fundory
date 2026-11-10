package com.fundory.app

import com.fundory.app.data.backup.BackupGoal
import com.fundory.app.data.backup.BackupProduct
import com.fundory.app.data.backup.BackupSnapshot
import com.fundory.app.data.backup.BackupTransaction
import com.fundory.app.data.backup.BackupValidator
import com.fundory.app.data.backup.FundoryBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testValidBackupValidation() {
        val validBackup = FundoryBackup(
            version = 1,
            exportDateEpochMillis = 1731273600000L,
            appName = "Fundory",
            goals = listOf(
                BackupGoal(
                    id = "goal_pc",
                    name = "PC Upgrade",
                    description = "Custom desktop workstation",
                    targetAmountGel = "702.35",
                    savedAmountGel = "40.00",
                    monthlyPlanGel = "100.00",
                    createdDateEpochMillis = 1731270000000L,
                    lastUpdatedEpochMillis = 1731273600000L
                )
            ),
            products = listOf(
                BackupProduct(
                    id = "prod_cpu",
                    goalId = "goal_pc",
                    name = "CPU + Motherboard",
                    originalPrice = "1100.00",
                    currency = "CNY",
                    dateAddedEpochMillis = 1731270000000L,
                    lastEditedEpochMillis = 1731270000000L
                )
            ),
            transactions = listOf(
                BackupTransaction(
                    id = "tx_1",
                    goalId = "goal_pc",
                    amountGel = "40.00",
                    timestampEpochMillis = 1731270000000L,
                    note = "Initial deposit"
                )
            ),
            snapshots = listOf(
                BackupSnapshot(
                    goalId = "goal_pc",
                    timestampEpochMillis = 1731270000000L,
                    savedAmountGel = "40.00",
                    targetAmountGel = "702.35",
                    completionPercentage = 5.7f
                )
            )
        )

        val rawJson = json.encodeToString(validBackup)
        val result = BackupValidator.validate(rawJson, json)

        assertTrue(result.isValid)
        assertEquals(1, result.goalCount)
        assertEquals(1, result.productCount)
        assertEquals(1, result.transactionCount)
        assertEquals(1, result.snapshotCount)
        assertNotNull(result.backupData)
        assertEquals("PC Upgrade", result.backupData?.goals?.first()?.name)
    }

    @Test
    fun testEmptyBackupRejection() {
        val emptyBackup = FundoryBackup(
            version = 1,
            appName = "Fundory",
            goals = emptyList(),
            products = emptyList(),
            transactions = emptyList()
        )

        val rawJson = json.encodeToString(emptyBackup)
        val result = BackupValidator.validate(rawJson, json)

        assertFalse(result.isValid)
        assertEquals("The provided file contains no Fundory records.", result.errorMessage)
    }

    @Test
    fun testOrphanProductDetection() {
        val orphanProductBackup = FundoryBackup(
            version = 1,
            appName = "Fundory",
            goals = listOf(
                BackupGoal(
                    id = "valid_goal",
                    name = "Valid Goal",
                    targetAmountGel = "100.00",
                    savedAmountGel = "0.00",
                    monthlyPlanGel = "50.00",
                    createdDateEpochMillis = 1731270000000L,
                    lastUpdatedEpochMillis = 1731270000000L
                )
            ),
            products = listOf(
                BackupProduct(
                    id = "prod_orphan",
                    goalId = "non_existent_goal_id", // Missing parent goal
                    name = "Ghost Product",
                    originalPrice = "50.00",
                    currency = "USD",
                    dateAddedEpochMillis = 1731270000000L,
                    lastEditedEpochMillis = 1731270000000L
                )
            )
        )

        val rawJson = json.encodeToString(orphanProductBackup)
        val result = BackupValidator.validate(rawJson, json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("orphaned items") == true)
    }

    @Test
    fun testOrphanTransactionDetection() {
        val orphanTxBackup = FundoryBackup(
            version = 1,
            appName = "Fundory",
            goals = listOf(
                BackupGoal(
                    id = "valid_goal",
                    name = "Valid Goal",
                    targetAmountGel = "100.00",
                    savedAmountGel = "0.00",
                    monthlyPlanGel = "50.00",
                    createdDateEpochMillis = 1731270000000L,
                    lastUpdatedEpochMillis = 1731270000000L
                )
            ),
            transactions = listOf(
                BackupTransaction(
                    id = "tx_orphan",
                    goalId = "ghost_goal_id", // Missing parent goal
                    amountGel = "100.00",
                    timestampEpochMillis = 1731270000000L
                )
            )
        )

        val rawJson = json.encodeToString(orphanTxBackup)
        val result = BackupValidator.validate(rawJson, json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("orphaned items") == true)
    }

    @Test
    fun testCorruptedJsonFormatRejection() {
        val corruptedJson = "{ appName: 'Fundory', version: 1, broken json syntax ... "
        val result = BackupValidator.validate(corruptedJson, json)

        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("Invalid JSON format") == true)
    }
}

