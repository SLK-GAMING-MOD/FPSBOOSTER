// đặt tại FpsMaster/app/src/main/java/com/fpsmaster/app/MainActivity.kt
package com.fpsmaster.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val OVERLAY_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler = findViewById<RecyclerView>(R.id.recyclerApps)
        val btnEnableBooster = findViewById<Button>(R.id.btnEnableBooster)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AppListAdapter(getInstalledGames()) { app ->
            // Bấm "Chơi" -> mở thẳng app đó
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Không mở được app này", Toast.LENGTH_SHORT).show()
            }
        }

        btnEnableBooster.setOnClickListener {
            requestOverlayPermissionThenStart()
        }
    }

    // Lấy danh sách các app có thể mở được (giống danh sách app trên máy)
    // Muốn CHỈ hiện game, bạn có thể lọc theo category = GAME (xem ghi chú bên dưới)
    private fun getInstalledGames(): List<AppInfo> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveList = pm.queryIntentActivities(mainIntent, 0)

        return resolveList
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun requestOverlayPermissionThenStart() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
        } else {
            startService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Đã bật nút FPS Booster nổi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "Đã bật nút FPS Booster nổi", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cần cấp quyền hiển thị đè lên app khác", Toast.LENGTH_LONG).show()
            }
        }
    }
}
