package com.fundory.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {

    @GET("v6/latest/{base}")
    suspend fun getLatestRates(
        @Path("base") base: String = "GEL"
    ): OpenExchangeRatesResponse
}

interface NbgApiService {

    @GET("gw/api/ct/monetarypolicy/currencies/en/json")
    suspend fun getLatestNbgRates(): List<NbgContainerResponse>
}

