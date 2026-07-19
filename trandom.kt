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
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.hypot

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

    private val countdownDurationMs = 1500L
    private var countdownStart = 0L
    private var countdownActive = false

    private var winnerScale = 0f
    private var winnerAnimator: ValueAnimator? = null

    private var floodRadius = 0f
    private var floodColor = Color.WHITE
    private var floodOriginX = 0f
    private var floodOriginY = 0f
    private var floodAnimator: ValueAnimator? = null

    private data class Shockwave(var radius: Float, var alpha: Int)
    private val shockwaves = mutableListOf<Shockwave>()
    private var shockwaveRunnable: Runnable? = null

    private fun colorForFinger(id: Int): Int {
        val hue = (id * 75 % 360).toFloat()
        return Color.HSVToColor(floatArrayOf(hue, 0.8f, 0.9f))
    }

    private val selectWinner = Runnable {
        if (fingers.isNotEmpty()) {
            val id = fingers.keys.toList().random()
            winnerId = id
            countdownActive = false
            triggerVibration()
            val (wx, wy) = fingers[id]!!
            startWinnerAnimation(wx, wy, colorForFinger(id))
        }
    }

    private fun startWinnerAnimation(originX: Float, originY: Float, color: Int) {
        floodOriginX = originX
        floodOriginY = originY
        floodColor = color
        floodRadius = 0f

        val corners = listOf(
            0f to 0f,
            width.toFloat() to 0f,
            0f to height.toFloat(),
            width.toFloat() to height.toFloat()
        )
        val maxRadius = corners.maxOf { (cx, cy) ->
            hypot((cx - originX).toDouble(), (cy - originY).toDouble()).toFloat()
        }

        floodAnimator?.cancel()
        floodAnimator = ValueAnimator.ofFloat(0f, maxRadius).apply {
            duration = 700
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                floodRadius = it.animatedValue as Float
                invalidate()
            }
            start()
        }

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

        shockwaves.add(Shockwave(radius = 100f, alpha = 255))
        startShockwaveLoop()
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
        val action = event.actionMasked

        if (winnerId != null) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                winnerId = null
                fingers.clear()
                shockwaves.clear()
                floodAnimator?.cancel()
                floodRadius = 0f
                invalidate()
            }
            return true
        }

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

        if (winnerId != null && floodRadius > 0f) {
            paint.style = Paint.Style.FILL
            paint.color = floodColor
            paint.alpha = 255
            canvas.drawCircle(floodOriginX, floodOriginY, floodRadius, paint)
        }

        if (fingers.isEmpty() && shockwaves.isEmpty() && winnerId == null) {
            paint.style = Paint.Style.FILL
            paint.color = Color.GRAY
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Place 2+ fingers to start", width / 2f, height / 2f, paint)
            return
        }

        val progress = if (countdownActive) {
            ((System.currentTimeMillis() - countdownStart).toFloat() / countdownDurationMs).coerceIn(0f, 1f)
        } else 0f

        fingers.forEach { (id, pos) ->
            val (x, y) = pos
            val fingerColor = colorForFinger(id)

            if (winnerId != null) {
                if (id == winnerId) {
                    val radius = 130f * winnerScale
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 16f
                    paint.alpha = 255
                    paint.color = Color.WHITE
                    canvas.drawCircle(x, y, radius, paint)
                }
            } else {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = fingerColor
                paint.alpha = 100
                canvas.drawCircle(x, y, pulseRadius + 30f, paint)

                paint.style = Paint.Style.FILL
                paint.alpha = 255
                canvas.drawCircle(x, y, pulseRadius, paint)

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
            shockwaves.forEach { wave ->
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                paint.color = Color.WHITE
                paint.alpha = wave.alpha
                canvas.drawCircle(floodOriginX, floodOriginY, wave.radius, paint)
            }
        }
    }
}