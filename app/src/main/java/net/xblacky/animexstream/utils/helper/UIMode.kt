package net.xblacky.animexstream.utils.helper

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration

fun Activity.isRunningOnAndroidTV(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
}