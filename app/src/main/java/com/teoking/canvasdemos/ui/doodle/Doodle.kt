package com.teoking.canvasdemos.ui.doodle

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.abs

class DoodleItem {

    // Doodle Line Color
    var color = -0xff0100

    // Doodle Line Width
    var width = 12.0f

    // Doodle points list
    val pointList = ArrayList<PointF>()

    // Path created from points
    private val path = Path()

    private lateinit var lastPoint: PointF

    // Add doodle point
    fun addPoint(point: PointF) {
        if (pointList.size == 0) {
            lastPoint = point
            // First point. Move the the start.
            path.moveTo(lastPoint.x, lastPoint.y)
            pointList.add(point)
        } else {
            val dx: Float = abs(point.x - lastPoint.x)
            val dy: Float = abs(point.y - lastPoint.y)
            if (dx > 0 || dy > 0) {
                // Add a quadratic bezier:
                // lastPoint as is curve control point
                //
                path.quadTo(
                    lastPoint.x,
                    lastPoint.y,
                    (point.x + lastPoint.x) / 2.0f,
                    (point.y + lastPoint.y) / 2.0f
                )
                pointList.add(point)
                lastPoint = point
            }
        }
    }

    fun draw(canvas: Canvas, paint: Paint) {
        if (pointList.size > 1) {
            val firstPoint: PointF = pointList[0]
            paint.style = Paint.Style.FILL
            paint.color = color
            canvas.drawCircle(firstPoint.x, firstPoint.y, width / 2.0f, paint)
            val lastPointA: PointF = pointList[pointList.size - 1]
            val lastPointB: PointF = pointList[pointList.size - 2]
            canvas.drawCircle(
                (lastPointA.x + lastPointB.x) / 2.0f,
                (lastPointA.y + lastPointB.y) / 2.0f,
                width / 2.0f,
                paint
            )
            paint.strokeWidth = width
            paint.style = Paint.Style.STROKE
            canvas.drawPath(path, paint)
        } else if (pointList.size == 1) { // Draw only one point
            val firstPoint: PointF = pointList[0]
            paint.style = Paint.Style.FILL
            paint.color = color
            canvas.drawCircle(firstPoint.x, firstPoint.y, width / 2.0f, paint)
            paint.strokeWidth = width
            paint.style = Paint.Style.STROKE
            canvas.drawPath(path, paint)
        }
        paint.xfermode = null
    }
}

class DoodleView(context: Context, attrs: AttributeSet): View(context, attrs) {

    private var currentDoodle: DoodleItem? = null
    private var doodleList = ArrayList<DoodleItem>()
    private val paint = Paint()
    private val lineWidth = 12.0f
    private val lineColor = Color.RED

    override fun onDraw(canvas: Canvas) {
        for (i in doodleList.indices) {
            doodleList[i].draw(canvas, paint)
        }
        currentDoodle?.draw(canvas, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // New doodle here.
                currentDoodle = DoodleItem().also {
                    it.width = lineWidth
                    it.color = lineColor
                    it.addPoint(PointF(x, y))
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentDoodle?.addPoint(PointF(x, y))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentDoodle?.let {
                    if (it.pointList.size > 0) {
                        if (it.pointList.size == 1) {
                            super.performClick()
                        }
                        it.addPoint(PointF(x, y))
                        // Ends the doodle, save it to doodleList.
                        doodleList.add(it)
                        currentDoodle = null
                        invalidate()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}