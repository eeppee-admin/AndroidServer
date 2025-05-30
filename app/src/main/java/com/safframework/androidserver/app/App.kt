package com.safframework.androidserver.app

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.interfaces.DialogXStyle
import com.kongzue.dialogx.style.MaterialStyle
import kotlin.properties.Delegates
import com.kongzue.dialogx.style.IOSStyle;

/**
 *
 * @FileName:
 *          com.safframework.androidserver.app.App
 * @author: Tony Shen
 * @date: 2020-11-20 23:59
 * @version: V1.0 <描述当前版本功能>
 */
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        CONTEXT = applicationContext

        //初始化
        DialogX.init(this);
        DialogX.globalStyle = IOSStyle.style()
    }

    companion object {
        var CONTEXT: Context by Delegates.notNull()
    }
}