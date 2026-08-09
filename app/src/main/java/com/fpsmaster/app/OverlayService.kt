// đặt tại FpsMaster/app/src/main/java/com/fpsmaster/app/OverlayService.kt
package com.fpsmaster.app

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)

        val overlayType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 300

        windowManager.addView(overlayView, params)

        val bubble = overlayView.findViewById<ImageView>(R.id.bubble)
        val panel = overlayView.findViewById<LinearLayout>(R.id.panel)
        val btnClean = overlayView.findViewById<Button>(R.id.btnClean)
        val btnStop = overlayView.findViewById<Button>(R.id.btnStop)

        // Kéo thả nút nổi + bấm để mở/đóng panel
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Bấm (không kéo) -> mở/đóng panel booster
                        panel.visibility =
                            if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        btnClean.setOnClickListener {
            cleanBackgroundApps()
        }

        btnStop.setOnClickListener {
            stopSelf()
        }
    }

    // Dọn bớt app chạy nền để rảnh RAM hơn khi chơi game
    // Lưu ý: Android giới hạn quyền này, chỉ dọn được phần nào, không "ép" tăng FPS thật sự
    private fun cleanBackgroundApps() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: emptyList()
        for (proc in runningApps) {
            if (proc.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                for (pkg in proc.pkgList) {
                    if (pkg != packageName) {
                        try {
                            am.killBackgroundProcesses(pkg)
                        } catch (e: Exception) {
                            // bỏ qua app không cho phép tắt
                        }
                    }
                }
            }
        }
        Toast.makeText(this, "Đã dọn app chạy nền", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
