package com.fundory.app.data.repository

import com.fundory.app.data.local.dao.ParcelDao
import com.fundory.app.data.local.entity.ParcelEntity
import com.fundory.app.domain.model.Parcel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParcelRepository @Inject constructor(
    private val parcelDao: ParcelDao
) {

    fun getParcelsByGoalFlow(goalId: String): Flow<List<Parcel>> {
        return parcelDao.getParcelsByGoalFlow(goalId).map { list -> list.map { it.toDomain() } }
    }

    fun getAllParcelsFlow(): Flow<List<Parcel>> {
        return parcelDao.getAllParcelsFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getParcelById(id: String): Parcel? {
        return parcelDao.getParcelById(id)?.toDomain()
    }

    suspend fun saveParcel(parcel: Parcel) {
        parcelDao.insertParcel(ParcelEntity.fromDomain(parcel))
    }

    suspend fun deleteParcel(id: String) {
        parcelDao.deleteParcel(id)
    }
}

