package com.clearpath.xray_compose.utils

import android.content.Context
import com.clearpath.xray_compose.GlobalConst
import java.util.concurrent.ConcurrentHashMap

object PackageUidResolver {
    private val packageUidCache = ConcurrentHashMap<String, Int>()

    fun packageUid(context: Context, packageName: String): Int? {
        return packageUidCache.getOrPut(packageName) {
            resolveUid(context, packageName)
        }
    }

    private fun resolveUid(context: Context, packageName: String): Int? {
        if (packageName == GlobalConst.unidentifiedPackageName) {
            return -1
        }
        return try {
            context.packageManager.getPackageUid(packageName, 0)
        } catch (e: Exception) {
            LogUtil.e("Failed to resolve UID for package: $packageName", e)
            null
        }
    }
}