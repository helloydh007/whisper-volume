package com.whispervolume.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.whispervolume.app.R
import com.whispervolume.app.util.formatAttenuation

class VolumeOverlay(
    private val service: Service,
    private val onAdjust: (Float) -> Unit,
    private val onClose: () -> Unit,
) {
    private val windowManager: WindowManager =
        service.getSystemService(WindowManager::class.java)

    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var dbLabel: TextView? = null

    fun show(): Boolean {
        if (root != null) return true

        val content = LayoutInflater.from(service)
            .inflate(R.layout.overlay_volume, null, false)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(14)
            y = dp(96)
        }

        dbLabel = content.findViewById(R.id.overlay_db)
        content.findViewById<View>(R.id.overlay_minus).setOnClickListener {
            onAdjust(-1f)
        }
        content.findViewById<View>(R.id.overlay_plus).setOnClickListener {
            onAdjust(1f)
        }
        content.findViewById<View>(R.id.overlay_close).setOnClickListener {
            onClose()
        }
        installDrag(content.findViewById(R.id.overlay_drag_handle), content, layoutParams)

        return try {
            windowManager.addView(content, layoutParams)
            root = content
            params = layoutParams
            true
        } catch (_: Exception) {
            root = null
            params = null
            dbLabel = null
            false
        }
    }

    fun update(db: Float) {
        dbLabel?.text = formatAttenuation(db)
    }

    fun hide() {
        val view = root ?: return
        runCatching { windowManager.removeView(view) }
        root = null
        params = null
        dbLabel = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun installDrag(handle: View, content: View, layoutParams: WindowManager.LayoutParams) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0

        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = layoutParams.x
                    startY = layoutParams.y
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    val screenWidth = screenWidth()
                    val screenHeight = screenHeight()
                    layoutParams.x = (startX - dx).coerceIn(
                        dp(4),
                        (screenWidth - content.width - dp(4)).coerceAtLeast(dp(4)),
                    )
                    layoutParams.y = (startY + dy).coerceIn(
                        dp(24),
                        (screenHeight - content.height - dp(24)).coerceAtLeast(dp(24)),
                    )
                    runCatching { windowManager.updateViewLayout(content, layoutParams) }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun screenWidth(): Int {
        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            null
        }
        return metrics?.width() ?: service.resources.displayMetrics.widthPixels
    }

    private fun screenHeight(): Int {
        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            null
        }
        return metrics?.height() ?: service.resources.displayMetrics.heightPixels
    }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()
}
