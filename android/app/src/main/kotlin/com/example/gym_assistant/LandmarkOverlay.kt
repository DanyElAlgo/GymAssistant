package com.example.gym_assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import org.json.JSONArray

class LandmarkOverlay(context: Context) : View(context) {
    companion object {
        private const val TAG = "GymPose"
    }

    private val points = mutableListOf<Pair<Float, Float>>()
    private var frameWidth = 1
    private var frameHeight = 1
    private var rotation = 0
    private var mirrorX = false
    private var updateCount = 0
    private var drawCount = 0
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        5 to 11, 6 to 12, 11 to 12,
        11 to 13, 13 to 15, 12 to 14, 14 to 16,
        11 to 23, 12 to 24, 23 to 24,
        23 to 25, 25 to 27, 24 to 26, 26 to 28
    )

    fun update(json: String, width: Int, height: Int, rotation: Int, mirror: Boolean) {
        val next = mutableListOf<Pair<Float, Float>>()
        val landmarks = JSONArray(json)
        for (index in 0 until landmarks.length()) {
            val landmark = landmarks.getJSONObject(index)
            val x = landmark.optDouble("x", Double.NaN).toFloat()
            val y = landmark.optDouble("y", Double.NaN).toFloat()
            if (x.isFinite() && y.isFinite()) {
                next += x.coerceIn(0f, 1f) to y.coerceIn(0f, 1f)
            }
        }
        updateCount++
        if (next.isNotEmpty() || updateCount == 1 || updateCount % 30 == 0) {
            Log.d(
                TAG,
                "Overlay update #$updateCount: input=${landmarks.length()}, valid=${next.size}, " +
                    "frame=${width}x$height, currentView=${this.width}x${this.height}, " +
                    "rotation=$rotation, mirror=$mirror"
            )
        }
        post {
            frameWidth = width
            frameHeight = height
            this.rotation = rotation
            mirrorX = mirror
            points.clear()
            points.addAll(next)
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = minOf(width.toFloat() / frameWidth, height.toFloat() / frameHeight)
        val offsetX = (width - frameWidth * scale) / 2f
        val offsetY = (height - frameHeight * scale) / 2f
        fun map(point: Pair<Float, Float>): Pair<Float, Float> {
            val rotated = when (rotation) {
                90 -> 1f - point.second to point.first
                180 -> 1f - point.first to 1f - point.second
                270 -> point.second to 1f - point.first
                else -> point
            }
            val x = if (mirrorX) 1f - rotated.first else rotated.first
            return offsetX + x * frameWidth * scale to
                offsetY + rotated.second * frameHeight * scale
        }

        val mapped = points.map(::map)
        drawCount++
        if (drawCount == 1 || drawCount % 30 == 0) {
            Log.d(
                TAG,
                "Overlay draw #$drawCount: view=${width}x$height, frame=${frameWidth}x$frameHeight, " +
                    "points=${mapped.size}, scale=$scale, offset=($offsetX,$offsetY)"
            )
        }
        connections.forEach { (start, end) ->
            if (start < mapped.size && end < mapped.size) {
                canvas.drawLine(
                    mapped[start].first,
                    mapped[start].second,
                    mapped[end].first,
                    mapped[end].second,
                    linePaint
                )
            }
        }
        mapped.forEach { (x, y) ->
            canvas.drawCircle(x, y, 7f, pointPaint)
        }
    }
}
