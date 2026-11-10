package com.fundory.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fundory.app.data.repository.CurrencyRepository
import com.fundory.app.data.repository.CurrencySyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ExchangeRateSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val currencyRepository: CurrencyRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (currencyRepository.syncRates(forceRefresh = false)) {
            is CurrencySyncResult.Success -> Result.success()
            is CurrencySyncResult.Failure -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "fundory_exchange_rate_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(
                12, TimeUnit.HOURS,
                1, TimeUnit.HOURS // flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

