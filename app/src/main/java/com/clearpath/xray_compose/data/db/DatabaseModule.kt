package com.clearpath.xray_compose.data.db

import android.content.Context
import com.clearpath.xray_compose.data.db.dao.ProfileDao

val Context.appDatabase: AppDatabase
    get() = AppDatabase.getDatabase(this)

val Context.profileDao: ProfileDao
    get() = appDatabase.profileDao()

val appDatabase: AppDatabase
    get() = AppDatabase.getDatabase()

val profileDao: ProfileDao
    get() = appDatabase.profileDao()
