package com.safframework.androidserver

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager


/**
 * TODO:Remove Latter
 */
object Util {
    /**
     * context是Activity Context
     */
    fun switchToAlias1(context: Context) {
        val packageManager =context.packageManager
        val defaultComponent = ComponentName(context, ".MainActivity")
        val aliasComponent = ComponentName(context, ".MainActivityAlias1")

        packageManager.setComponentEnabledSetting(defaultComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        packageManager.setComponentEnabledSetting(aliasComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }
}