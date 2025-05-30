package com.safframework.androidserver

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kongzue.dialogx.dialogs.InputDialog
import com.kongzue.dialogx.interfaces.OnInputDialogButtonClickListener
import com.safframework.androidserver.databinding.ActivityMainBinding
import com.safframework.androidserver.log.LogProxyImpl
import com.safframework.androidserver.server.startHttpServer
import com.safframework.kotlin.coroutines.runInBackground
import com.safframework.server.converter.gson.GsonConverter
import com.safframework.server.core.AndroidServer
import com.safframework.utils.localIPAddress

class MainActivity : AppCompatActivity() {
    var changeTo: String? = null
    private var androidServer: AndroidServer? = null
    private var port: Int? = 9999 // 默认端口号
    private val CHANNEL_ID = "server_channel"
    private val NOTIFICATION_ID = 1
    private val REQUEST_NOTIFICATION_PERMISSION = 100
    var inputPort: Int? = null // 输入的端口好

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // 开关控制打开或关闭服务器
        binding?.openServerSwitch?.setOnCheckedChangeListener(
            object : CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(
                    p0: CompoundButton?,
                    ischeck: Boolean
                ) {
                    if (ischeck) {
                        requestNotificationPermission()
                        showPortInputDialog()
                    } else {
                        androidServer?.close()
                        binding?.content?.text = ""
                        cancelNotification()
                    }
                }
            }
        )

        // 切换应用图标
        binding?.switchAppIconBtn?.setOnClickListener {
            Toast.makeText(this, javaClass.name + "1", Toast.LENGTH_SHORT).show()
            changeTo = javaClass.name + "1"
            changeLauncherIcon(changeTo!!)
        }
    }

    //https://juejin.cn/post/7457897807921758234
    fun changeLauncherIcon(name: String) {
        val pm = packageManager
        //隐藏之前显示的桌面组件,todo:为了调试卸载，不要隐藏原来的app图标，否则要去应用卸载
//        pm.setComponentEnabledSetting(
//            componentName,
//            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
//        )
        //显示新的桌面组件
        pm.setComponentEnabledSetting(
            ComponentName(this@MainActivity, name),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                createNotificationChannel()
            }
        } else {
            createNotificationChannel()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createNotificationChannel()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Server Channel"
            val descriptionText = "Channel for server notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Server Status")
            .setContentText("Server is running on port $port")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun cancelNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
    }

    private fun showPortInputDialog() {
        // dialogx 的ios风格dialog
        val dia = InputDialog("请输入端口号", "不能输入常见端口号，如3306", "确定", "取消", "")
            .setCancelable(true)
            .setOkButton(object : OnInputDialogButtonClickListener<InputDialog> {
                override fun onClick(
                    dialog: InputDialog?,
                    v: View?,
                    inputStr: String?
                ): Boolean {
                    val check = isValidPort(inputStr?.toIntOrNull()!!)
                    if (check) {
                        inputPort = inputStr?.toIntOrNull()
                        port = inputPort
                        initData()
                        Toast.makeText(
                            this@MainActivity,
                            inputPort.toString(),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        dialog?.dismiss()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "无效的端口号，请选择其他端口",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        binding?.openServerSwitch?.isChecked = false
                    }
                    return true
                }
            })
        dia.show()

// 下面是安卓内置的dialog
//        val inputDialog = AlertDialog.Builder(this)
//        inputDialog.setTitle("请输入端口号,不能输入常见端口号,如3306")
//        val input = EditText(this)
//        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
//        inputDialog.setView(input)
//        inputDialog.setPositiveButton("确定") { dialog, which ->
//            inputPort = input.text.toString().toIntOrNull()
//            if (inputPort != null && isValidPort(inputPort!!)) {
//                port = inputPort
//                println("User entered port: $port") // Debug print
//                initData()
//            } else {
//                Toast.makeText(this, "无效的端口号，请选择其他端口", Toast.LENGTH_SHORT).show()
//                binding?.openServerSwitch?.isChecked = false
//            }
//        }
//        inputDialog.setNegativeButton("取消") { dialog, which ->
//            binding?.openServerSwitch?.isChecked = false
//        }
//        inputDialog.show()
    }

    private fun initData() {
        println("Starting server on port: $port") // Debug print
        binding?.content?.text = "内网IP：$localIPAddress \nAndroidServer库在${port}端口提供服务"

        runInBackground { //  通过协程启动 AndroidServer
            androidServer = AndroidServer.Builder {
                converter {
                    GsonConverter()
                }
                logProxy {
                    LogProxyImpl
                }
                port {
                    inputPort!!
                }
            }.build()

            startHttpServer(this@MainActivity, androidServer!!)
            sendNotification()
        }
    }

    // 启动本程序不要在常见的port
    private fun isValidPort(port: Int): Boolean {
        val commonPorts = listOf(80, 3306, 443, 22, 21, 25, 110, 143, 465, 993, 995)
        return port !in commonPorts && port in 1..65535
    }


    override fun onDestroy() {
        if (changeTo != null) {
            changeLauncherIcon(changeTo!!)
        }

        androidServer?.close()
        super.onDestroy()
    }
}