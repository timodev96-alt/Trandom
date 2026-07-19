package com.example.trandom

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.random.Random

class SpinWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var options: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private var rotationDegrees = 0f
    private var spinAnimator: ValueAnimator? = null

    private val palette = intArrayOf(
        Color.parseColor("#FF5252"),
        Color.parseColor("#448AFF"),
        Color.parseColor("#69F0AE"),
        Color.parseColor("#FFD740"),
        Color.parseColor("#E040FB"),
        Color.parseColor("#40C4FF"),
        Color.parseColor("#FF6E40"),
        Color.parseColor("#B2FF59"),
        Color.parseColor("#FF80AB"),
        Color.parseColor("#EA80FC")
    )

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 4f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E1E1E")
        style = Paint.Style.FILL
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        textSize = 44f
        textAlign = Paint.Align.CENTER
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a < 0f) a += 360f
        return a
    }


    fun spin(onResult: (String) -> Unit) {
        if (options.size < 2 || spinAnimator?.isRunning == true) return

        val winnerIndex = Random.nextInt(options.size)
        val anglePerSegment = 360f / options.size

        val desiredRestAngle = normalizeAngle(-90f - winnerIndex * anglePerSegment - anglePerSegment / 2f)
        val currentMod = normalizeAngle(rotationDegrees)
        val delta = normalizeAngle(desiredRestAngle - currentMod)

        val extraSpins = 6
        val targetRotation = rotationDegrees + extraSpins * 360f + delta

        spinAnimator?.cancel()
        spinAnimator = ValueAnimator.ofFloat(rotationDegrees, targetRotation).apply {
            duration = 4200
            interpolator = DecelerateInterpolator(2.2f)
            addUpdateListener {
                rotationDegrees = it.animatedValue as Float
                invalidate()
            }
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    vibrate()
                    onResult(options[winnerIndex])
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Vibrator::class.java) as? Vibrator ?: return
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.parseColor("#121212"))

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - 40f

        if (options.size < 2) {
            canvas.drawText("Add at least 2 options", cx, cy, hintPaint)
            return
        }

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val anglePerSegment = 360f / options.size

        options.forEachIndexed { i, label ->
            val startAngle = normalizeAngle(rotationDegrees + i * anglePerSegment)
            segmentPaint.color = palette[i % palette.size]
            canvas.drawArc(rect, startAngle, anglePerSegment, true, segmentPaint)
            canvas.drawArc(rect, startAngle, anglePerSegment, true, strokePaint)

            canvas.save()
            val midAngle = startAngle + anglePerSegment / 2f
            canvas.translate(cx, cy)
            canvas.rotate(midAngle)
            canvas.translate(radius * 0.62f, 0f)
            val normalizedMid = normalizeAngle(midAngle)
            if (normalizedMid in 90f..270f) {
                canvas.rotate(180f)
            }
            val displayLabel = if (label.length > 14) label.take(13) + "…" else label
            canvas.drawText(displayLabel, 0f, textPaint.textSize / 3f, textPaint)
            canvas.restore()
        }

        canvas.drawCircle(cx, cy, radius * 0.12f, hubPaint)
        strokePaint.strokeWidth = 6f
        canvas.drawCircle(cx, cy, radius * 0.12f, strokePaint)
        strokePaint.strokeWidth = 4f

        val pointerHalfWidth = 40f
        val path = Path().apply {
            moveTo(cx - pointerHalfWidth, cy - radius - 10f)
            lineTo(cx + pointerHalfWidth, cy - radius - 10f)
            lineTo(cx, cy - radius + pointerHalfWidth)
            close()
        }
        canvas.drawPath(path, pointerPaint)
    }
}