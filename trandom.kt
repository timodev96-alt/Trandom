package com.example.trandom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper

class FingerRandomizerView : View {

    private lateinit var paint: Paint
    private val localHandler = Handler(Looper.getMainLooper())
    private val fingers = HashMap<Int, Pair<Float, Float>>()
    private var winnerId: Int? = null

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    private fun initView() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }

    private val selectWinner = Runnable {
        if (fingers.isNotEmpty()) {
            winnerId = fingers.keys.toList().random()
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (winnerId != null) {
            winnerId = null
            fingers.clear()
            invalidate()
            return true
        }

        val action = event.actionMasked
        val id = event.getPointerId(event.actionIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                fingers[id] = Pair(event.getX(event.actionIndex), event.getY(event.actionIndex))
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
            currentHandler.postDelayed(selectWinner, 1500)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#121212"))

        if (fingers.isEmpty()) {
            paint.color = Color.GRAY
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Place 2+ fingers to start", width / 2f, height / 2f, paint)
            return
        }

        fingers.forEach { (id, pos) ->
            val (x, y) = pos
            paint.color = Color.HSVToColor(floatArrayOf((id * 75 % 360).toFloat(), 0.8f, 0.9f))
            paint.style = Paint.Style.FILL

            if (winnerId != null) {
                if (id == winnerId) {
                    canvas.drawCircle(x, y, 180f, paint)
                } else {
                    paint.color = Color.parseColor("#22FFFFFF")
                    canvas.drawCircle(x, y, 80f, paint)
                }
            } else {
                canvas.drawCircle(x, y, 110f, paint)
            }
        }
    }
}