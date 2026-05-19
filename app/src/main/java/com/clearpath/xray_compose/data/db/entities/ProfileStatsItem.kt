package com.clearpath.xray_compose.data.db.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

@Entity(tableName = "profile_stats")
data class ProfileStatsItem(
    @PrimaryKey
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    val totalUp: Long = 0L,
    val totalDown: Long = 0L,
    val todayUp: Long = 0L,
    val todayDown: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis(),
)
