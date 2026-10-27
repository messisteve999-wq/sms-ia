package com.steve.smsia

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Context
import kotlin.math.abs

class FloatingAssistantService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private val mint = Color.rgb(0, 220, 165)
    private val dark = Color.rgb(12, 25, 31)
    private val card = Color.rgb(27, 45, 52)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this, "Autorise l'affichage par-dessus les applications",
                Toast.LENGTH_LONG
            ).show()
            stopSelf()
            return
        }
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private fun params(width: Int, height: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width, height, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18
            y = 240
        }

    private fun rounded(color: Int, radius: Float = 22f) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }

    private fun showBubble() {
        if (bubble != null) return

        val view = TextView(this).apply {
            text = "S"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(dark)
            background = rounded(mint, 100f)
            elevation = 12f
            contentDescription = "Ouvrir SMS-IA"
        }
        val p = params(58, 58)
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x
                    startY = p.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = startX + (event.rawX - touchX).toInt()
                    p.y = startY + (event.rawY - touchY).toInt()
                    try { wm.updateViewLayout(v, p) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (abs(event.rawX - touchX) < 10 &&
                        abs(event.rawY - touchY) < 10) showPanel()
                    true
                }
                else -> true
            }
        }

        bubble = view
        wm.addView(view, p)
    }

    private fun showPanel() {
        if (panel != null) {
            closePanel()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            background = rounded(dark, 28f)
            elevation = 16f
        }

        root.addView(TextView(this).apply {
            text = "✦ SMS-IA"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(mint)
        })

        root.addView(TextView(this).apply {
            text = "Écris ou colle le message reçu"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 12)
        })

        val message = EditText(this).apply {
            hint = "Message à traiter…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 15f
            minLines = 3
            gravity = Gravity.TOP
            setPadding(12, 12, 12, 12)
            background = rounded(card, 16f)
        }
        root.addView(message, LinearLayout.LayoutParams(-1, -2))

        val open = Button(this).apply {
            text = "Générer avec Gemini"
            isAllCaps = false
            setTextColor(dark)
            setBackgroundColor(mint)
            setOnClickListener {
                val text = message.text.toString().trim()
                if (text.isBlank()) {
                    Toast.makeText(
                        this@FloatingAssistantService,
                        "Saisis d'abord un message",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val intent = Intent(
                        this@FloatingAssistantService,
                        MainActivity::class.java
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("floating_message", text)
                        putExtra("floating_generate", true)
                    }
                    closePanel()
                    startActivity(intent)
                }
            }
        }
        root.addView(open, LinearLayout.LayoutParams(-1, -2))

        val close = Button(this).apply {
            text = "Fermer"
            isAllCaps = false
            setOnClickListener { closePanel() }
        }
        root.addView(close, LinearLayout.LayoutParams(-1, -2))

        val p = params(
            (resources.displayMetrics.widthPixels - 32).coerceAtLeast(260),
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            x = 16
            y = 150
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        panel = root
        panelParams = p
        try {
            wm.addView(root, p)
        } catch (e: Exception) {
            panel = null
            panelParams = null
            Toast.makeText(this, "Impossible d'ouvrir la fenêtre", Toast.LENGTH_LONG).show()
        }
    }

    private fun closePanel() {
        panel?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        panel = null
        panelParams = null
    }

    override fun onDestroy() {
        closePanel()
        bubble?.let {
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        bubble = null
        super.onDestroy()
    }
}
