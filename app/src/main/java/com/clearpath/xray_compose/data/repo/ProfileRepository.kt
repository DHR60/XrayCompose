package com.clearpath.xray_compose.data.repo

import android.content.Context
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.db.AppDatabase
import com.clearpath.xray_compose.data.db.dao.ProfileDao
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.profileRepository: ProfileRepository
    get() = ProfileRepository.getInstance(this)

val profileRepository: ProfileRepository
    get() = ProfileRepository.getInstance()

class ProfileRepository private constructor(context: Context) {
    private val profileDao: ProfileDao = AppDatabase.getDatabase(context).profileDao()

    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(context: Context): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getInstance(): ProfileRepository {
            return INSTANCE
                ?: error("ProfileRepository is not initialized. Please call getInstance(context) first.")
        }
    }

    suspend fun insertProfile(profile: ProfileModel) {
        profileDao.insertProfile(profile.toProfileItem())
    }

    suspend fun updateProfile(profile: ProfileModel) {
        profileDao.updateProfile(profile.toProfileItem())
    }

    suspend fun deleteProfile(profile: ProfileModel) {
        val profileItem = profile.toProfileItem()
        profileDao.deleteProfile(profileItem)
        try {
            profileDao.deleteProfileTestById(profileItem.id)
            profileDao.deleteProfileStatsById(profileItem.id)
        } catch (_: Exception) {
            // Ignore if related tests or stats do not exist
        }
    }

    suspend fun deleteProfileById(id: String) {
        val uuid = UuidCreator.fromString(id)
        profileDao.deleteProfileById(uuid)
        try {
            profileDao.deleteProfileTestById(uuid)
            profileDao.deleteProfileStatsById(uuid)
        } catch (_: Exception) {
            // Ignore if related tests or stats do not exist
        }
    }

    suspend fun getAllProfiles(): List<ProfileModel> =
        profileDao.getAllProfiles().map { ProfileModel.fromProfileItem(it) }

    suspend fun getProfileById(id: UUID): ProfileModel? =
        profileDao.getProfileById(id)?.let { ProfileModel.fromProfileItem(it) }

    suspend fun getAllProfilesBySubid(subid: String): List<ProfileModel> =
        profileDao.getAllProfilesBySubid(subid).map { ProfileModel.fromProfileItem(it) }

    suspend fun getAllProfilesOrdered(): List<ProfileModel> =
        profileDao.getAllProfilesOrdered().map { ProfileModel.fromProfileItem(it) }

    suspend fun getAllProfilesBySubidOrdered(subid: String): List<ProfileModel> =
        profileDao.getAllProfilesBySubidOrdered(subid).map { ProfileModel.fromProfileItem(it) }

    suspend fun findFirstProfileByRemark(remark: String): ProfileModel? =
        profileDao.findFirstProfileByRemark(remark)?.let { ProfileModel.fromProfileItem(it) }

    fun observeProfileById(id: UUID): Flow<ProfileModel?> =
        profileDao.observeProfileById(id)
            .map { it?.let { ProfileModel.fromProfileItem(it) } }

    fun observeAllProfilesOrdered(): Flow<List<ProfileModel>> =
        profileDao.observeAllProfilesOrdered()
            .map { list -> list.map { ProfileModel.fromProfileItem(it) } }

    fun observeAllProfilesBySubidOrdered(subid: String): Flow<List<ProfileModel>> =
        profileDao.observeAllProfilesBySubidOrdered(subid)
            .map { list -> list.map { ProfileModel.fromProfileItem(it) } }
}
