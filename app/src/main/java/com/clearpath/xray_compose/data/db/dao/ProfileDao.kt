package com.clearpath.xray_compose.data.db.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Upsert
import com.clearpath.xray_compose.data.db.entities.ProfileItem
import com.clearpath.xray_compose.data.db.entities.ProfileStatsItem
import com.clearpath.xray_compose.data.db.entities.ProfileTestItem
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileItem>)

    @Update
    suspend fun updateProfile(profile: ProfileItem)

    @Upsert
    suspend fun upsertProfile(profile: ProfileItem)

    @Upsert
    suspend fun upsertProfiles(profiles: List<ProfileItem>)

    @Delete
    suspend fun deleteProfile(profile: ProfileItem)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: UUID)

    @Query("DELETE FROM profiles WHERE issub = 1 AND subid = :subid")
    suspend fun deleteSubProfiles(subid: String)

    @Query("DELETE FROM profiles WHERE subid = :subid")
    suspend fun deleteProfilesBySubid(subid: String)

    @Query("SELECT COUNT(*) FROM profiles WHERE subid = :subid")
    suspend fun getProfileCountForSub(subid: String): Int

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<ProfileItem>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: UUID): ProfileItem?

    @Query("SELECT * FROM profiles WHERE subid = :subid")
    suspend fun getAllProfilesBySubid(subid: String): List<ProfileItem>

    @Query("SELECT * FROM profiles ORDER BY sortOrder ASC")
    suspend fun getAllProfilesOrdered(): List<ProfileItem>

    @Query("SELECT * FROM profiles WHERE subid = :subid ORDER BY sortOrder ASC")
    suspend fun getAllProfilesBySubidOrdered(subid: String): List<ProfileItem>

    @Query("SELECT * FROM profiles WHERE remark = :remark LIMIT 1")
    suspend fun findFirstProfileByRemark(remark: String): ProfileItem?

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeProfileById(id: UUID): Flow<ProfileItem?>

    @Query("SELECT * FROM profiles ORDER BY sortOrder ASC")
    fun observeAllProfilesOrdered(): Flow<List<ProfileItem>>

    @Query("SELECT * FROM profiles WHERE subid = :subid ORDER BY sortOrder ASC")
    fun observeAllProfilesBySubidOrdered(subid: String): Flow<List<ProfileItem>>

    // ProfileStatsItem
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileStats(stats: ProfileStatsItem)

    @Update
    suspend fun updateProfileStats(stats: ProfileStatsItem)

    @Delete
    suspend fun deleteProfileStats(stats: ProfileStatsItem)

    @Query("DELETE FROM profile_stats WHERE id = :id")
    suspend fun deleteProfileStatsById(id: UUID)

    @Query("SELECT * FROM profile_stats WHERE id = :id")
    suspend fun getProfileStatsById(id: UUID): ProfileStatsItem?

    @Query("DELETE FROM profile_stats WHERE id NOT IN (SELECT id FROM profiles)")
    suspend fun cleanOldProfileStats()

    // ProfileTestItem
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileTest(test: ProfileTestItem)

    @Update
    suspend fun updateProfileTest(test: ProfileTestItem)

    @Delete
    suspend fun deleteProfileTest(test: ProfileTestItem)

    @Query("DELETE FROM profile_test WHERE id = :id")
    suspend fun deleteProfileTestById(id: UUID)

    @Query("SELECT * FROM profile_test WHERE id = :id")
    suspend fun getProfileTestById(id: UUID): ProfileTestItem?

    @Query("SELECT * FROM profile_test WHERE id = :id")
    fun observeProfileTestById(id: UUID): Flow<ProfileTestItem?>

    @Query("SELECT * FROM profile_test")
    fun observeAllProfileTests(): Flow<List<ProfileTestItem>>

    @Query("DELETE FROM profile_test WHERE id NOT IN (SELECT id FROM profiles)")
    suspend fun cleanOldProfileTests()
}