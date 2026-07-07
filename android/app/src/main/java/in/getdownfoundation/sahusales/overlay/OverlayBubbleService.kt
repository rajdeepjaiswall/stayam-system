package `in`.getdownfoundation.sahusales.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import kotlin.math.abs

class OverlayBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleRoot: FrameLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        showBubble()
    }

    private fun showBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics

        val bubbleSizePx = (68 * dm.density).toInt()
        val closeSizePx  = (26 * dm.density).toInt()
        val totalSize    = bubbleSizePx + closeSizePx / 2

        val params = WindowManager.LayoutParams(
            totalSize, totalSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 220
        }

        val root = FrameLayout(this)

        // ── Main bubble ──────────────────────────────────────────────────────
        val bubble = FrameLayout(this).apply {
            val lp = FrameLayout.LayoutParams(bubbleSizePx, bubbleSizePx)
            lp.gravity = Gravity.BOTTOM or Gravity.START
            lp.setMargins(0, closeSizePx / 2, 0, 0)
            layoutParams = lp
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1565C0"))
            }
            elevation = 10f
        }

        val alarmIcon = ImageButton(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setImageResource(android.R.drawable.ic_lock_idle_alarm)
            setColorFilter(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = (14 * dm.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        bubble.addView(alarmIcon)

        // ── Close (×) button top-right ───────────────────────────────────────
        val closeBtn = ImageButton(this).apply {
            val lp = FrameLayout.LayoutParams(closeSizePx, closeSizePx)
            lp.gravity = Gravity.TOP or Gravity.END
            layoutParams = lp
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#D32F2F"))
            }
            setOnClickListener { stopSelf() }
        }

        root.addView(bubble)
        root.addView(closeBtn)

        // ── Drag support on the bubble ───────────────────────────────────────
        var initX = 0; var initY = 0
        var initTX = 0f; var initTY = 0f
        var moved = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    initTX = event.rawX; initTY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initTX).toInt()
                    val dy = (event.rawY - initTY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    if (moved) {
                        params.x = initX + dx
                        params.y = initY + dy
                        try { windowManager?.updateViewLayout(root, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // tap without drag → open app
                    if (!moved) {
                        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        launch?.let { startActivity(it) }
                    }
                    true
                }
                else -> false
            }
        }

        bubbleRoot = root
        windowManager?.addView(root, params)
    }

    override fun onDestroy() {
        bubbleRoot?.let { v ->
            try { windowManager?.removeView(v) } catch (_: Exception) {}
        }
        bubbleRoot = null
        super.onDestroy()
    }
}
