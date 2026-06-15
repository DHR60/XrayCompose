package com.clearpath.xray_compose.data.repo

import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.db.dao.ProfileDao
import com.clearpath.xray_compose.data.db.entities.ProfileTestItem
import com.clearpath.xray_compose.data.sanitizer.ProfileSanitizer
import com.clearpath.xray_compose.utils.LogUtil
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val profileDao: ProfileDao) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            try {
                profileDao.cleanOldProfileStats()
            } catch (e: Exception) {
                LogUtil.e("ProfileRepository Error during initialization", e)
            }
        }
        repositoryScope.launch {
            try {
                profileDao.cleanOldProfileTests()
            } catch (e: Exception) {
                LogUtil.e("ProfileRepository Error during initialization", e)
            }
        }
    }

    suspend fun insertProfile(profile: ProfileModel) {
        profileDao.insertProfile(ProfileSanitizer.sanitize(profile).toProfileItem())
    }

    suspend fun insertProfiles(profiles: List<ProfileModel>) {
        profileDao.insertProfiles(ProfileSanitizer.sanitize(profiles).map { it.toProfileItem() })
    }

    // suspend fun updateProfile(profile: ProfileModel) {
    //     profileDao.updateProfile(profile.toProfileItem())
    // }

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

    suspend fun deleteSubProfiles(subid: String) {
        profileDao.deleteSubProfiles(subid)
    }

    suspend fun deleteProfilesBySubid(subid: String) {
        profileDao.deleteProfilesBySubid(subid)
    }

    suspend fun getProfileCountForSub(subid: String): Int {
        return profileDao.getProfileCountForSub(subid)
    }

    suspend fun getAllProfiles(): List<ProfileModel> =
        profileDao.getAllProfiles().map { ProfileModel.fromProfileItem(it) }

    suspend fun getProfileById(id: String): ProfileModel? =
        profileDao.getProfileById(UuidCreator.fromString(id))
            ?.let { ProfileModel.fromProfileItem(it) }

    suspend fun getAllProfilesBySubid(subid: String): List<ProfileModel> =
        profileDao.getAllProfilesBySubid(subid).map { ProfileModel.fromProfileItem(it) }

    suspend fun getAllProfilesOrdered(): List<ProfileModel> =
        profileDao.getAllProfilesOrdered().map { ProfileModel.fromProfileItem(it) }

    suspend fun getAllProfilesBySubidOrdered(subid: String): List<ProfileModel> =
        profileDao.getAllProfilesBySubidOrdered(subid).map { ProfileModel.fromProfileItem(it) }

    suspend fun findFirstProfileByRemark(remark: String): ProfileModel? =
        profileDao.findFirstProfileByRemark(remark)?.let { ProfileModel.fromProfileItem(it) }

    fun observeProfileById(id: String): Flow<ProfileModel?> =
        profileDao.observeProfileById(UuidCreator.fromString(id))
            .map { it?.let { ProfileModel.fromProfileItem(it) } }

    fun observeAllProfilesOrdered(): Flow<List<ProfileModel>> =
        profileDao.observeAllProfilesOrdered()
            .map { list -> list.map { ProfileModel.fromProfileItem(it) } }

    fun observeAllProfilesBySubidOrdered(subid: String): Flow<List<ProfileModel>> =
        profileDao.observeAllProfilesBySubidOrdered(subid)
            .map { list -> list.map { ProfileModel.fromProfileItem(it) } }

    suspend fun insertProfileTest(profileTest: ProfileTestItem) {
        profileDao.insertProfileTest(profileTest)
    }

    suspend fun updateProfileTest(profileTest: ProfileTestItem) {
        profileDao.updateProfileTest(profileTest)
    }

    suspend fun getProfileTestById(id: String): ProfileTestItem? {
        return profileDao.getProfileTestById(UuidCreator.fromString(id))
    }

    fun observeProfileTestById(id: String): Flow<ProfileTestItem?> {
        return profileDao.observeProfileTestById(UuidCreator.fromString(id))
    }

    fun observeAllProfileTests(): Flow<List<ProfileTestItem>> {
        return profileDao.observeAllProfileTests()
    }
}
