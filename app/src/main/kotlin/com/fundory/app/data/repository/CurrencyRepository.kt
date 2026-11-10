package com.fundory.app.data.repository

import com.fundory.app.data.local.dao.ExchangeRateDao
import com.fundory.app.data.local.entity.ExchangeRateEntity
import com.fundory.app.data.preferences.UserPreferencesRepository
import com.fundory.app.data.remote.CurrencyApiService
import com.fundory.app.data.remote.NbgApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

sealed class CurrencySyncResult {
    data object Success : CurrencySyncResult()
    data class Failure(val message: String, val usingCachedData: Boolean) : CurrencySyncResult()
}

@Singleton
class CurrencyRepository @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val currencyApiService: CurrencyApiService,
    private val nbgApiService: NbgApiService,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    // Default offline seed rates (1 Foreign = X GEL)
    private val defaultRatesToGel = mapOf(
        "GEL" to BigDecimal("1.00"),
        "USD" to BigDecimal("2.594"),
        "EUR" to BigDecimal("2.90"),
        "CNY" to BigDecimal("0.388"),
        "GBP" to BigDecimal("3.48"),
        "JPY" to BigDecimal("0.0164"),
        "TRY" to BigDecimal("0.0516")
    )

    val ratesToGelFlow: Flow<Map<String, BigDecimal>> = exchangeRateDao.getAllRatesFlow().map { entities ->
        if (entities.isEmpty()) {
            defaultRatesToGel
        } else {
            val map = defaultRatesToGel.toMutableMap()
            for (entity in entities) {
                map[entity.currencyCode.uppercase()] = entity.rateToGel
            }
            map
        }
    }

    suspend fun getCurrentRatesToGel(): Map<String, BigDecimal> {
        val cached = exchangeRateDao.getAllRates()
        if (cached.isEmpty()) {
            return defaultRatesToGel
        }
        val map = defaultRatesToGel.toMutableMap()
        for (entity in cached) {
            map[entity.currencyCode.uppercase()] = entity.rateToGel
        }
        return map
    }

    suspend fun syncRates(forceRefresh: Boolean = false): CurrencySyncResult {
        val now = System.currentTimeMillis()
        val prefs = userPreferencesRepository.userPreferencesFlow.first()
        val lastSync = prefs.lastExchangeRatesSyncMillis
        val isStale = (now - lastSync) > (12 * 60 * 60 * 1000L) // 12 hours

        if (!forceRefresh && !isStale && lastSync > 0) {
            return CurrencySyncResult.Success
        }

        // Try primary provider: open.er-api
        try {
            val response = currencyApiService.getLatestRates("GEL")
            if (response.result.equals("success", ignoreCase = true) && response.rates.isNotEmpty()) {
                val entities = mutableListOf<ExchangeRateEntity>()
                entities.add(ExchangeRateEntity("GEL", BigDecimal.ONE, now))

                for ((code, rateFromGel) in response.rates) {
                    if (rateFromGel > 0.0) {
                        // rateFromGel is: 1 GEL = X Foreign
                        // Therefore 1 Foreign = 1 / X GEL
                        val rateToGel = BigDecimal.ONE.divide(
                            BigDecimal.valueOf(rateFromGel),
                            6,
                            RoundingMode.HALF_UP
                        )
                        entities.add(ExchangeRateEntity(code.uppercase(), rateToGel, now))
                    }
                }

                if (entities.isNotEmpty()) {
                    exchangeRateDao.insertOrUpdateRates(entities)
                    userPreferencesRepository.setLastExchangeRatesSyncMillis(now)
                    return CurrencySyncResult.Success
                }
            }
        } catch (_: Exception) {
            // Fallback to secondary provider: NBG
        }

        try {
            val nbgContainers = nbgApiService.getLatestNbgRates()
            if (nbgContainers.isNotEmpty() && nbgContainers[0].currencies.isNotEmpty()) {
                val entities = mutableListOf<ExchangeRateEntity>()
                entities.add(ExchangeRateEntity("GEL", BigDecimal.ONE, now))

                for (item in nbgContainers[0].currencies) {
                    if (item.rate > 0.0) {
                        // In NBG API, rate is already 1 Foreign = X GEL!
                        entities.add(
                            ExchangeRateEntity(
                                item.code.uppercase(),
                                BigDecimal.valueOf(item.rate).setScale(6, RoundingMode.HALF_UP),
                                now
                            )
                        )
                    }
                }

                if (entities.isNotEmpty()) {
                    exchangeRateDao.insertOrUpdateRates(entities)
                    userPreferencesRepository.setLastExchangeRatesSyncMillis(now)
                    return CurrencySyncResult.Success
                }
            }
        } catch (_: Exception) {
            // Secondary also failed, continue to check cached
        }

        // Check if we have cached rates in Room
        val existingCached = exchangeRateDao.getAllRates()
        val hasCache = existingCached.isNotEmpty()

        return if (hasCache) {
            CurrencySyncResult.Failure(
                message = "Could not reach exchange rate servers. Using cached rates.",
                usingCachedData = true
            )
        } else {
            // Seed default rates to Room so user can still convert offline safely
            val seedEntities = defaultRatesToGel.map { (code, rate) ->
                ExchangeRateEntity(code, rate, now)
            }
            exchangeRateDao.insertOrUpdateRates(seedEntities)
            CurrencySyncResult.Failure(
                message = "Offline mode: initialized with standard indicative market rates.",
                usingCachedData = false
            )
        }
    }
}

