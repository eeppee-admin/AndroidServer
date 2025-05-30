package com.safframework.androidserver

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safframework.androidserver.databinding.ActivityMainBinding
import com.safframework.androidserver.log.LogProxyImpl
import com.safframework.androidserver.server.startHttpServer
import com.safframework.kotlin.coroutines.runInBackground
import com.safframework.server.converter.gson.GsonConverter
import com.safframework.server.core.AndroidServer
import com.safframework.utils.localIPAddress

class MainActivity : AppCompatActivity() {

    private lateinit var androidServer: AndroidServer
    private var port: Int? = 9999 // 默认端口号
    private val CHANNEL_ID = "server_channel"
    private val NOTIFICATION_ID = 1
    private val REQUEST_NOTIFICATION_PERMISSION = 100
    var inputPort: Int? = null

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
                        androidServer.close()
                        binding?.content?.text = ""
                        cancelNotification()
                    }
                }
            }
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
        val inputDialog = AlertDialog.Builder(this)
        inputDialog.setTitle("请输入端口号")
        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        inputDialog.setView(input)
        inputDialog.setPositiveButton("确定") { dialog, which ->
            inputPort = input.text.toString().toIntOrNull()
            if (inputPort != null && isValidPort(inputPort!!)) {
                port = inputPort
                println("User entered port: $port") // Debug print
                initData()
            } else {
                Toast.makeText(this, "无效的端口号，请选择其他端口", Toast.LENGTH_SHORT).show()
                binding?.openServerSwitch?.isChecked = false
            }
        }
        inputDialog.setNegativeButton("取消") { dialog, which ->
            binding?.openServerSwitch?.isChecked = false
        }
        inputDialog.show()
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

            startHttpServer(this@MainActivity, androidServer)
            sendNotification()
        }
    }

    // 启动本程序不要在常见的port
    private fun isValidPort(port: Int): Boolean {
        val commonPorts = listOf(80, 3306, 443, 22, 21, 25, 110, 143, 465, 993, 995)
        return port !in commonPorts && port in 1..65535
    }


    override fun onDestroy() {
        super.onDestroy()
        androidServer.close()
    }
}