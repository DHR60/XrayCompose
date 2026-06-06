package com.clearpath.xray_compose.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import com.clearpath.xray_compose.data.db.converter.EConfigTypeConverter
import com.clearpath.xray_compose.data.db.converter.UuidConverter
import com.clearpath.xray_compose.data.db.dao.ProfileDao
import com.clearpath.xray_compose.data.db.entities.ProfileItem
import com.clearpath.xray_compose.data.db.entities.ProfileStatsItem
import com.clearpath.xray_compose.data.db.entities.ProfileTestItem

@Database(
    entities = [ProfileItem::class, ProfileStatsItem::class, ProfileTestItem::class],
    version = 1
)
@TypeConverters(UuidConverter::class, EConfigTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .enableMultiInstanceInvalidation()
                    .build().also { INSTANCE = it }
            }
        }

        fun getDatabase(): AppDatabase {
            return INSTANCE
                ?: error("AppDatabase is not initialized. Please call getDatabase(context) first.")
        }
    }
}