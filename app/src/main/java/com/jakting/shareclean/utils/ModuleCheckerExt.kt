package com.jakting.shareclean.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.jakting.shareclean.utils.application.Companion.appContext
import com.topjohnwu.superuser.Shell

fun moduleApplyAvailable(): Boolean {
    // 模块生效后会拦截本应用对 text/tigerinthewall 的查询并返回空列表
    return try {
        val resolveInfoList = appContext.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_PROCESS_TEXT).setType("text/tigerinthewall"),
            PackageManager.MATCH_ALL
        )
        resolveInfoList.none { it.activityInfo.packageName == appContext.packageName }
    } catch (e: Exception) {
        false
    }
}

fun Context?.moduleInfo(): Array<String> {
    val zygiskShell = runShell("cat /data/adb/modules/zygisk_ifw_enhance_tiw/module.prop")
    if(zygiskShell.isSuccess){
        val version = moduleVersion(zygiskShell)
        return arrayOf("Zygisk", version[0] ?: "", version[1] ?: "")
    }
    return arrayOf("", "", "")
}

fun Context?.moduleVersion(sr: Shell.Result): Array<String?> {
    val shellResult = sr.getPureCat()
    logd(shellResult)
    if (shellResult.isEmpty()) return arrayOf("", "")
    val version = Regex(".*?version=(.*?),.*?versionCode=(.*?),").find(shellResult)?.groupValues
    return arrayOf(version?.get(1), version?.get(2))
}

