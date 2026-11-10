package com.fundory.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenExchangeRatesResponse(
    @SerialName("result") val result: String = "success",
    @SerialName("base_code") val baseCode: String = "GEL",
    @SerialName("time_last_update_unix") val timeLastUpdateUnix: Long? = null,
    @SerialName("time_last_update_utc") val timeLastUpdateUtc: String? = null,
    @SerialName("rates") val rates: Map<String, Double> = emptyMap()
)

@Serializable
data class NbgContainerResponse(
    @SerialName("date") val date: String? = null,
    @SerialName("currencies") val currencies: List<NbgCurrencyItem> = emptyList()
)

@Serializable
data class NbgCurrencyItem(
    @SerialName("code") val code: String,
    @SerialName("name") val name: String? = null,
    @SerialName("rate") val rate: Double = 0.0,
    @SerialName("diff") val diff: Double = 0.0,
    @SerialName("date") val date: String? = null
)
