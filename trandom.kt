package com.example.trandom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator

class FingerRandomizerView : View {

    private lateinit var paint: Paint
    private val localHandler = Handler(Looper.getMainLooper())
    private val fingers = HashMap<Int, Pair<Float, Float>>()
    private var winnerId: Int? = null

    constructor(context: Context) : super(context) { initView() }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { initView() }

    private fun initView() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }

    // ---- idle pulse (while waiting / not enough fingers yet) ----
    private var pulseRadius = 110f
    private var pulseGrowing = true

    private var pulseRunnable = object : Runnable {
        override fun run() {
            if (fingers.isNotEmpty() && winnerId == null) {
                if (pulseGrowing) {
                    pulseRadius += 1.5f
                    if (pulseRadius >= 130f) pulseGrowing = false
                } else {
                    pulseRadius -= 1.5f
                    if (pulseRadius <= 110f) pulseGrowing = true
                }
                invalidate()
                localHandler.postDelayed(this, 16)
            }
        }
    }

    // ---- countdown ring ----
    private val countdownDurationMs = 1500L
    private var countdownStart = 0L
    private var countdownActive = false

    // ---- winner reveal animation ----
    private var winnerScale = 0f
    private var winnerAnimator: ValueAnimator? = null

    // ---- shockwave rings that burst out when a winner is picked ----
    private data class Shockwave(var radius: Float, var alpha: Int)
    private val shockwaves = mutableListOf<Shockwave>()
    private var shockwaveRunnable: Runnable? = null

    private val selectWinner = Runnable {
        if (fingers.isNotEmpty()) {
            winnerId = fingers.keys.toList().random()
            countdownActive = false
            triggerVibration()
            startWinnerAnimation()
        }
    }

    private fun startWinnerAnimation() {
        shockwaves.add(Shockwave(radius = 100f, alpha = 255))
        startShockwaveLoop()

        winnerScale = 0f
        winnerAnimator?.cancel()
        winnerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = OvershootInterpolator(3f)
            addUpdateListener {
                winnerScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startShockwaveLoop() {
        if (shockwaveRunnable != null) return
        shockwaveRunnable = object : Runnable {
            override fun run() {
                val iterator = shockwaves.iterator()
                while (iterator.hasNext()) {
                    val wave = iterator.next()
                    wave.radius += 8f
                    wave.alpha -= 10
                    if (wave.alpha <= 0) iterator.remove()
                }
                invalidate()
                if (shockwaves.isNotEmpty()) {
                    localHandler.postDelayed(this, 16)
                } else {
                    shockwaveRunnable = null
                }
            }
        }
        localHandler.post(shockwaveRunnable!!)
    }

    private fun triggerVibration() {
        val vibrator = context.getSystemService(Vibrator::class.java) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (winnerId != null) {
            winnerId = null
            fingers.clear()
            shockwaves.clear()
            invalidate()
            return true
        }

        val action = event.actionMasked
        val id = event.getPointerId(event.actionIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                fingers[id] = Pair(event.getX(event.actionIndex), event.getY(event.actionIndex))
                if (fingers.size == 1) {
                    localHandler.post(pulseRunnable)
                }
                resetTimer()
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    if (fingers.containsKey(pId)) {
                        fingers[pId] = Pair(event.getX(i), event.getY(i))
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                fingers.remove(id)
                resetTimer()
            }

            MotionEvent.ACTION_CANCEL -> {
                val currentHandler = handler ?: localHandler
                currentHandler.removeCallbacks(selectWinner)
                countdownActive = false
                fingers.clear()
            }
        }
        invalidate()
        return true
    }

    private fun resetTimer() {
        val currentHandler = handler ?: localHandler
        currentHandler.removeCallbacks(selectWinner)
        if (fingers.size >= 2) {
            countdownStart = System.currentTimeMillis()
            countdownActive = true
            currentHandler.postDelayed(selectWinner, countdownDurationMs)
        } else {
            countdownActive = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#121212"))

        if (fingers.isEmpty() && shockwaves.isEmpty()) {
            paint.style = Paint.Style.FILL
            paint.color = Color.GRAY
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Place 2+ fingers to start", width / 2f, height / 2f, paint)
            return
        }

        // progress toward a pick, 0..1
        val progress = if (countdownActive) {
            ((System.currentTimeMillis() - countdownStart).toFloat() / countdownDurationMs).coerceIn(0f, 1f)
        } else 0f

        fingers.forEach { (id, pos) ->
            val (x, y) = pos
            val hue = (id * 75 % 360).toFloat()
            val fingerColor = Color.HSVToColor(floatArrayOf(hue, 0.8f, 0.9f))

            if (winnerId != null) {
                if (id == winnerId) {
                    val radius = 110f * winnerScale
                    paint.style = Paint.Style.FILL
                    paint.alpha = 255
                    paint.color = fingerColor
                    canvas.drawCircle(x, y, radius, paint)

                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 15f
                    paint.color = Color.WHITE
                    canvas.drawCircle(x, y, radius, paint)
                } else {
                    paint.style = Paint.Style.FILL
                    paint.color = Color.parseColor("#22FFFFFF")
                    canvas.drawCircle(x, y, 80f, paint)
                }
            } else {
                // base finger circle
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = fingerColor
                paint.alpha = 100
                canvas.drawCircle(x, y, pulseRadius + 30f, paint)

                paint.style = Paint.Style.FILL
                paint.alpha = 255
                canvas.drawCircle(x, y, pulseRadius, paint)

                // countdown ring: fills clockwise as the pick approaches
                if (countdownActive) {
                    val ringRadius = pulseRadius + 55f
                    val rect = RectF(x - ringRadius, y - ringRadius, x + ringRadius, y + ringRadius)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 10f
                    paint.alpha = 255
                    paint.color = fingerColor
                    canvas.drawArc(rect, -90f, 360f * progress, false, paint)
                }
            }
        }

        if (shockwaves.isNotEmpty() && winnerId != null) {
            val (wx, wy) = fingers[winnerId] ?: return
            shockwaves.forEach { wave ->
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = Color.WHITE
                paint.alpha = wave.alpha
                canvas.drawCircle(wx, wy, wave.radius, paint)
            }
        }
    }
}