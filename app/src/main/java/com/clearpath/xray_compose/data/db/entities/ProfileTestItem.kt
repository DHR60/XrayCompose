package com.clearpath.xray_compose.data.db.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.github.f4b6a3.uuid.UuidCreator
import java.util.UUID

@Entity(tableName = "profile_test")
data class ProfileTestItem(
    @PrimaryKey
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    val delay: Int = 0,
    val speedMB: Double = 0.0,
    val testResult: String = "",
    val message: String = "",
)
