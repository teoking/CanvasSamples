package com.teoking.canvasdemos.ui.spectrogram

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.sin


class SpectrogramView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mBytes: ByteArray? = null
    private var mFFTBytes: ByteArray? = null
    private val mRect: Rect = Rect()

    private val mFlashPaint: Paint = Paint()
    private val mFadePaint: Paint = Paint()

    private val mRenderer: Renderer
    private val mFftRenderer: Renderer

    private lateinit var mCanvasBitmap: Bitmap
    private lateinit var mCanvas: Canvas

    init {
        mFlashPaint.color = Color.argb(122, 255, 255, 255)
        mFadePaint.color = Color.argb(238, 255, 255, 255)
        mFadePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)

        val paint = Paint()
        paint.strokeWidth = 3f
        paint.isAntiAlias = true
        paint.color = Color.argb(255, 222, 92, 143)
        mRenderer = CircleRender(paint, true)

        val paint2 = Paint()
        paint2.strokeWidth = 8f
        paint2.isAntiAlias = true
        paint2.xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
        paint2.color = Color.argb(255, 222, 92, 143)
        mFftRenderer = CircleBarFftRenderer(paint2, 32, true)
    }

    fun update(bytes: ByteArray) {
        mBytes = bytes
        invalidate()
    }

    fun updateFFT(fftBytes: ByteArray) {
        mFFTBytes = fftBytes
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            mRect.set(0, 0, w, h)
            mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            mCanvas = Canvas(mCanvasBitmap)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mBytes?.let {
            mRenderer.render(mCanvas, it, mRect)
        }

        mFFTBytes?.let {
            mFftRenderer.renderFFT(mCanvas, it, mRect)
        }

        // Fade out old contents
        mCanvas.drawPaint(mFadePaint)

        canvas.drawBitmap(mCanvasBitmap, matrix, null)
    }
}

abstract class Renderer {

    // Have these as members, so we don't have to re-create them each time
    var mPoints: FloatArray? = null
    var mFFTPoints: FloatArray? = null

    fun render(canvas: Canvas, bytes: ByteArray, rect: Rect) {
        if (mPoints == null || mPoints?.size ?: 0 < bytes.size * 4) {
            mPoints = FloatArray(bytes.size * 4)
        }
        onRender(canvas, bytes, rect)
    }

    fun renderFFT(canvas: Canvas, fftBytes: ByteArray, rect: Rect) {
        if (mFFTPoints == null || mFFTPoints?.size ?: 0 < fftBytes.size * 4) {
            mFFTPoints = FloatArray(fftBytes.size * 4)
        }
        onRenderFFT(canvas, fftBytes, rect)
    }

    // As the display of raw/FFT audio will usually look different, subclasses
    // will typically only implement one of the below methods
    /**
     * Implement this method to render the audio data onto the canvas
     * @param canvas - Canvas to draw on
     * @param data - Data to render
     * @param rect - Rect to render into
     */
    abstract fun onRender(canvas: Canvas, data: ByteArray, rect: Rect)

    /**
     * Implement this method to render the FFT audio data onto the canvas
     * @param canvas - Canvas to draw on
     * @param data - Data to render
     * @param rect - Rect to render into
     */
    abstract fun onRenderFFT(canvas: Canvas, data: ByteArray, rect: Rect)
}

class CircleRender(private val mPaint: Paint, private val mCycleColor: Boolean) : Renderer() {

    private var modulation = 0f
    private var aggressive = 0.33f

    private val points = FloatArray(8)

    override fun onRender(canvas: Canvas, data: ByteArray, rect: Rect) {
        if (mCycleColor) {
            cycleColor()
        }

        val rwh = rect.width() / 2
        val rhh = rect.height() / 2

        mPoints?.let {
            for (i in 0 until data.size - 1) {
                points[0] = i.toFloat() / (data.size - 1)
                points[1] = (rhh + (data[i] + 128).toByte() * (rhh) / 128).toFloat()
                toPolar(points, rwh, rhh, 0)
                // x1
                it[i * 4] = points[2]
                // y1
                it[i * 4 + 1] = points[3]
                points[4] = (i + 1).toFloat() / (data.size - 1)
                points[5] = (rhh + (data[i + 1] + 128).toByte() * (rhh) / 128).toFloat()
                toPolar(points, rwh, rhh, 4)
                // x2
                it[i * 4 + 2] = points[6]
                // y2
                it[i * 4 + 3] = points[7]
            }
            canvas.drawLines(it, mPaint)

            // Controls the pulsing rate
            modulation += 0.04f
        }
    }

    override fun onRenderFFT(canvas: Canvas, data: ByteArray, rect: Rect) {
        // Do nothing, only render audio data
    }

    private fun toPolar(cartesian: FloatArray, rwh: Int, rhh: Int, start: Int): FloatArray {
        val cX = rwh.toDouble()
        val cY = rhh.toDouble()
        val angle = cartesian[start] * 2 * Math.PI
        val radius =
            (rwh * (1 - aggressive) + aggressive * cartesian[start + 1] / 2) * (1.2 + sin(
                modulation.toDouble()
            )) / 2.2
        cartesian[start + 2] = (cX + radius * sin(angle)).toFloat()
        cartesian[start + 3] = (cY + radius * cos(angle)).toFloat()
        return cartesian
    }

    private var colorCounter = 0f
    private fun cycleColor() {
        val r = floor(128 * (sin(colorCounter.toDouble()) + 1)).toInt()
        val g =
            floor(128 * (sin((colorCounter + 2).toDouble()) + 1)).toInt()
        val b =
            floor(128 * (sin((colorCounter + 4).toDouble()) + 1)).toInt()
        mPaint.color = Color.argb(128, r, g, b)
        colorCounter += 0.03f
    }
}

class CircleBarFftRenderer(
    private val mPaint: Paint, private val mDivisions: Int,
    private val mCycleColor: Boolean
) : Renderer() {

    private val points = FloatArray(8)

    override fun onRender(canvas: Canvas, data: ByteArray, rect: Rect) {
        // Do nothing, only render fft data
    }

    override fun onRenderFFT(canvas: Canvas, data: ByteArray, rect: Rect) {
        if (mCycleColor) {
            cycleColor()
        }

        val rwh = rect.width() / 2
        val rhh = rect.height() / 2

        mFFTPoints?.let {
            for (i in 0 until data.size / mDivisions) {
                // Calculate dbValue
                val rfk: Byte = data[mDivisions * i]
                val ifk: Byte = data[mDivisions * i + 1]
                val magnitude = (rfk * rfk + ifk * ifk).toFloat()
                val dbValue = 75 * log10(magnitude.toDouble()).toFloat()
                points[0] = (i * mDivisions).toFloat() / (data.size - 1)
                points[1] = rhh - dbValue / 4
                toPolar(points, rwh, rhh, 0)
                // x1
                it[i * 4] = points[2]
                // y1
                it[i * 4 + 1] = points[3]
                points[4] = (i * mDivisions).toFloat() / (data.size - 1)
                points[5] = rhh + dbValue
                toPolar(points, rwh, rhh, 4)
                // x2
                it[i * 4 + 2] = points[6]
                // y2
                it[i * 4 + 3] = points[7]
            }

            canvas.drawLines(it, mPaint)

            // Controls the pulsing rate
            modulation += 0.13f
            angleModulation += 0.28f
        }

    }

    private var modulation = 0f
    private var modulationStrength = 0.4f // 0-1

    private var angleModulation = 0f
    private var aggressive = 0.4f

    private fun toPolar(cartesian: FloatArray, rwh: Int, rhh: Int, start: Int): FloatArray {
        val cX = rwh.toDouble()
        val cY = rhh.toDouble()
        val angle = cartesian[start] * 2 * Math.PI
        val radius =
            (rwh * (1 - aggressive) + aggressive * cartesian[start + 1] / 2) * (1 - modulationStrength + modulationStrength * (1 + sin(
                modulation.toDouble()
            )) / 2)
        cartesian[start + 2] = (cX + radius * sin(angle + angleModulation)).toFloat()
        cartesian[start + 3] = (cY + radius * cos(angle + angleModulation)).toFloat()
        return cartesian
    }

    private var colorCounter = 0f
    private fun cycleColor() {
        val r = floor(128 * (sin(colorCounter.toDouble()) + 1)).toInt()
        val g =
            floor(128 * (sin((colorCounter + 2).toDouble()) + 1)).toInt()
        val b =
            floor(128 * (sin((colorCounter + 4).toDouble()) + 1)).toInt()
        mPaint.color = Color.argb(128, r, g, b)
        colorCounter += 0.03f
    }
}