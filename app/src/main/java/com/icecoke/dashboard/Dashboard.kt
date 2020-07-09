package com.icecoke.dashboard

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

import java.lang.IllegalArgumentException
import kotlin.math.*

class Dashboard(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    View(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null)

    private var viewWidth = 0
    private var viewHeight = 0

    private var outStripeWidth = Utils.dp2px(context, 8f)
    private var outStripeColor = Color.parseColor("#33008FFF")
    private var midStripeWidth = Utils.dp2px(context, 6f)
    private var midStripeColorLight = Color.parseColor("#008FFF")
    private var midStripeColor = Color.parseColor("#80008FFF")
    private var innerStripeColorLight = Color.parseColor("#aa008FFF")
    private var innerStripeColor = Color.parseColor("#40008FFF")
    private var innerStripeColorStart = Color.BLACK

    private var radius = 0
    private var startAngle = 0
    private var sweepAngle = 0
    private var bigSliceCount = 0
    private var sliceCountPerBigSlice = 0
    private var innerRadius = 0f
    private var innerSliceCount = 0
    private var innerSliceRadius = 0
    private var innerSliceCountPerSmallSlice = 0
    private var innerSliceAngle = 0f
    private var arcColor = 0
    private var sliceTextSize = 0
    private var textColor = 0
    private var unitText = ""
    private var valueTextSize = 0

    var minValue = 0
    var maxValue = 0
    var currentValue = 0
        set(value) {
            field = value
            currentAngle = getAngleFromValue(value)
            invalidate()
        }
    private var bigSliceRadius = 0
    private var smallSliceRadius = 0
    private var sliceTextRadius = 0
    private var smallSliceCount = 0
    private var bigSliceAngle = 0f
    private var smallSliceAngle = 0f

    private var centerX = 0f
    private var centerY = 0f
    private var currentAngle = 0f
    private var rectInner = RectF()
    private var rectStripe = RectF()

    private lateinit var sliceTextValue: Array<String>

    private val paintArc = Paint()
    private val paintText = Paint()
    private val paintPointer = Paint()
    private val paintStripe = Paint()
    private val paintValue = Paint()

    init {
        attrs?.let {
            val typedArray =
                context.obtainStyledAttributes(it, R.styleable.Dashboard, defStyleAttr, 0)
            radius = typedArray.getDimensionPixelSize(
                R.styleable.Dashboard_radius,
                Utils.dp2px(context, 80f).toInt()
            )
            innerRadius = radius.toFloat() / 12 * 5
            startAngle = typedArray.getInteger(R.styleable.Dashboard_startAngle, 180)
            sweepAngle = typedArray.getInteger(R.styleable.Dashboard_sweepAngle, 180)
            bigSliceCount = typedArray.getInteger(R.styleable.Dashboard_bigSliceCount, 10)
            sliceCountPerBigSlice =
                typedArray.getInteger(R.styleable.Dashboard_sliceCountPerBigSlice, 5)
            innerSliceCountPerSmallSlice =
                typedArray.getInteger(R.styleable.Dashboard_innerSliceCountPerSmallSlice, 5)
            arcColor = typedArray.getColor(R.styleable.Dashboard_arcColor, Color.WHITE)

            sliceTextSize = typedArray.getDimensionPixelSize(
                R.styleable.Dashboard_sliceTextSize,
                Utils.sp2px(context, 12f).toInt()
            )

            textColor = typedArray.getColor(R.styleable.Dashboard_textColor, arcColor)
            unitText = typedArray.getString(R.styleable.Dashboard_unitText) ?: ""
            valueTextSize = typedArray.getDimensionPixelSize(
                R.styleable.Dashboard_valueTextSize,
                Utils.sp2px(context, 14f).toInt()
            )

            minValue = typedArray.getInteger(R.styleable.Dashboard_minValue, 0)
            maxValue = typedArray.getInteger(R.styleable.Dashboard_maxValue, 100)
            currentValue = typedArray.getInteger(R.styleable.Dashboard_currentValue, 0)

            typedArray.recycle()
        }

        initPaint()
        initSize()
    }

    private fun initPaint() {
        paintArc.apply {
            isAntiAlias = true
            color = arcColor
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        paintText.apply {
            isAntiAlias = true
            color = textColor
            style = Paint.Style.FILL
        }

        paintPointer.isAntiAlias = true
        paintPointer.strokeCap = Paint.Cap.ROUND

        paintStripe.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        paintValue.apply {
            isAntiAlias = true
            color = textColor
            style = Paint.Style.STROKE
            strokeWidth = Utils.dp2px(context, 2f)
            textAlign = Paint.Align.CENTER
            textSize = valueTextSize.toFloat()
        }
    }

    private fun initSize() {
        if (sweepAngle > 360) {
            throw IllegalArgumentException("sweepAngle must be less than 360 degree")
        } else if (sweepAngle == 0) {
            throw IllegalArgumentException("sweepAngle must be more than 0 degree")
        }

        if (startAngle >= 360) {
            throw IllegalArgumentException("startAngle must be less than 360 degree")
        }

        smallSliceRadius = radius - Utils.dp2px(context, 8f).toInt()
        bigSliceRadius = smallSliceRadius - Utils.dp2px(context, 4f).toInt()
        sliceTextRadius = bigSliceRadius - Utils.dp2px(context, 3f).toInt()

        smallSliceCount = bigSliceCount * sliceCountPerBigSlice
        bigSliceAngle = sweepAngle.toFloat() / bigSliceCount
        smallSliceAngle = bigSliceAngle / sliceCountPerBigSlice

        innerSliceRadius = (innerRadius - Utils.dp2px(context, 8f)).toInt()
        innerSliceAngle = smallSliceAngle / innerSliceCountPerSmallSlice
        innerSliceCount = (360 / innerSliceAngle).toInt()

        sliceTextValue = getSliceText()

        calculateCenterX()

        viewWidth = radius * 2 + paddingStart + paddingEnd
        viewHeight = radius * 2 + paddingTop + paddingBottom
        centerX = (viewWidth.toFloat() + paddingStart - paddingEnd) / 2
        centerY = (viewHeight.toFloat() + paddingTop - paddingBottom) / 2

        currentAngle = getAngleFromValue(currentValue)
    }

    private fun calculateCenterX() {
        centerX = 0f
        centerY = 0f
        val xList = ArrayList<Float>()
        if (startAngle <= 180 && startAngle + sweepAngle >= 180) {
            xList.add(-radius.toFloat())
        }

        if (startAngle >= 0 && startAngle + sweepAngle >= 360) {
            xList.add(radius.toFloat())
        }

        xList.add(-innerRadius)
        xList.add(innerRadius)
        val point1 = getCoordinatePoint(radius, startAngle.toFloat())
        val point2 = getCoordinatePoint(radius, (startAngle + sweepAngle).toFloat())
        xList.add(point1[0])
        xList.add(point2[0])
        xList.sort()
        Log.e("xiaolong", "xList: $xList")
    }

    private fun getSliceText(): Array<String> {

        return Array(bigSliceCount + 1) { i ->
            when (i) {
                0 -> {
                    minValue.toString()
                }
                bigSliceCount -> {
                    maxValue.toString()
                }
                else -> {
                    (((maxValue - minValue) / bigSliceCount) * i).toString()
                }
            }
        }
    }

    private fun getAngleFromValue(value: Int): Float {
        if (value >= maxValue) {
            return maxValue.toFloat()
        }
        return sweepAngle * (value.toFloat() - minValue) / (maxValue - minValue) + startAngle
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode == MeasureSpec.EXACTLY) {
            viewWidth = widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            viewWidth = min(viewWidth, widthSize)
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            viewHeight = heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            viewHeight = min(viewHeight, heightSize)
        }

        centerX = (viewWidth.toFloat() + paddingStart - paddingEnd) / 2
        centerY = (viewHeight.toFloat() + paddingTop - paddingBottom) / 2

        setMeasuredDimension(viewWidth, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawStripe(canvas)
        drawSlice(canvas)
        drawInnerArc(canvas)
        drawInnerSlice(canvas)
        drawHeader(canvas)
        drawPointer(canvas)
    }

    private fun getCoordinatePoint(radius: Int, cirAngle: Float): FloatArray {
        val point = FloatArray(2)
        val arcAngle = Math.toRadians(cirAngle.toDouble())
        point[0] = (centerX + cos(arcAngle) * radius).toFloat()
        point[1] = (centerY + sin(arcAngle) * radius).toFloat()
        return point
    }

    private fun drawStripe(canvas: Canvas) {
        drawOutStripe(canvas)
        drawMidStripe(canvas)
        drawInnerStripe(canvas)
    }

    private fun drawOutStripe(canvas: Canvas) {
        paintStripe.strokeWidth = outStripeWidth
        val r = radius - outStripeWidth / 2
        rectStripe = RectF(centerX - r, centerY - r, centerX + r, centerY + r)
        paintStripe.color = outStripeColor
        canvas.drawArc(
            rectStripe,
            startAngle.toFloat(),
            sweepAngle.toFloat(),
            false,
            paintStripe
        )
    }

    private fun drawMidStripe(canvas: Canvas) {
        paintStripe.strokeWidth = midStripeWidth
        val r = radius - outStripeWidth - Utils.dp2px(context, 1f) - midStripeWidth / 2
        rectStripe = RectF(centerX - r, centerY - r, centerX + r, centerY + r)
        paintStripe.color = midStripeColorLight
        canvas.drawArc(
            rectStripe,
            startAngle.toFloat(),
            currentAngle - startAngle,
            false,
            paintStripe
        )
        paintStripe.color = midStripeColor
        canvas.drawArc(
            rectStripe,
            currentAngle,
            sweepAngle - currentAngle + startAngle,
            false,
            paintStripe
        )
    }

    private fun drawInnerStripe(canvas: Canvas) {
        val r = radius - outStripeWidth - Utils.dp2px(context, 1f) - midStripeWidth
        rectStripe = RectF(centerX - r, centerY - r, centerX + r, centerY + r)
        val currentColors =
            listOf(innerStripeColorStart, innerStripeColorStart, innerStripeColorLight)
        val paintShader = Paint()
        val currentShader = RadialGradient(
            centerX,
            centerY,
            r,
            currentColors.toIntArray(),
            null,
            Shader.TileMode.CLAMP
        )
        paintShader.shader = currentShader
        canvas.drawArc(
            rectStripe,
            startAngle.toFloat(),
            currentAngle - startAngle,
            true,
            paintShader
        )

        val colors = listOf(innerStripeColorStart, innerStripeColorStart, innerStripeColor)
        val shader = RadialGradient(
            centerX,
            centerY,
            r,
            colors.toIntArray(),
            null,
            Shader.TileMode.CLAMP
        )
        paintShader.shader = shader
        canvas.drawArc(
            rectStripe,
            currentAngle,
            sweepAngle - currentAngle + startAngle,
            true,
            paintShader
        )
    }

    private fun drawSlice(canvas: Canvas) {
        drawBigSlice(canvas)
        drawSliceText(canvas)
        drawSmallSlice(canvas)
    }

    private fun drawBigSlice(canvas: Canvas) {
        paintArc.strokeWidth = Utils.dp2px(context, 2f)
        paintArc.color = arcColor
        for (i in 0..bigSliceCount) {
            val angle = i * bigSliceAngle + startAngle
            val point1 = getCoordinatePoint(radius, angle)
            val point2 = getCoordinatePoint(bigSliceRadius, angle)
            canvas.drawLine(point1[0], point1[1], point2[0], point2[1], paintArc)
        }
    }

    private fun drawSliceText(canvas: Canvas) {
        paintArc.strokeWidth = Utils.dp2px(context, 2f)
        paintArc.color = arcColor
        paintText.textSize = sliceTextSize.toFloat()
        for (i in 0..bigSliceCount) {
            val angle = i * bigSliceAngle + startAngle
            val sliceText = sliceTextValue[i]
            if (angle % 360 > 90 && angle % 360 < 270) {
                paintText.textAlign = Paint.Align.LEFT
            } else if ((angle % 360 >= 0 && angle % 360 < 90) || (angle % 360 > 270 && angle % 360 <= 360)) {
                paintText.textAlign = Paint.Align.RIGHT
            } else {
                paintText.textAlign = Paint.Align.CENTER
            }

            val sliceTextPoint = getCoordinatePoint(sliceTextRadius, angle)

            val fontMetrics = paintText.fontMetrics
            val baseLine = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom

            if (angle % 360 > 180 && angle % 360 < 270 || angle % 360 > 270 && angle % 360 < 360) {
                canvas.drawText(
                    sliceText,
                    sliceTextPoint[0],
                    sliceTextPoint[1] - fontMetrics.ascent,
                    paintText
                )
            } else if (angle % 360 > 90 && angle % 360 < 180 || angle % 360 > 0 && angle % 360 < 90) {
                canvas.drawText(sliceText, sliceTextPoint[0], sliceTextPoint[1], paintText)
            } else if (angle % 360 == 0f || angle % 360 == 180f) {
                canvas.drawText(
                    sliceText,
                    sliceTextPoint[0],
                    sliceTextPoint[1] + baseLine,
                    paintText
                )
            } else if (angle == 270f) {
                canvas.drawText(
                    sliceText,
                    sliceTextPoint[0],
                    sliceTextPoint[1] + fontMetrics.descent - fontMetrics.ascent,
                    paintText
                )
            } else if (angle == 90f) {
                canvas.drawText(
                    sliceText,
                    sliceTextPoint[0],
                    sliceTextPoint[1] - fontMetrics.bottom,
                    paintText
                )
            }
        }
    }

    private fun drawSmallSlice(canvas: Canvas) {
        paintArc.strokeWidth = Utils.dp2px(context, 1f)
        paintArc.color = arcColor
        for (i in 0 until smallSliceCount) {
            if (i % sliceCountPerBigSlice != 0) {
                val angle = i * smallSliceAngle + startAngle
                val point1 = getCoordinatePoint(radius, angle)
                val point2 = getCoordinatePoint(smallSliceRadius, angle)
                canvas.drawLine(point1[0], point1[1], point2[0], point2[1], paintArc)
            }
        }
    }

    private fun drawInnerArc(canvas: Canvas) {
        paintArc.strokeWidth = Utils.dp2px(context, 1f)
        paintArc.color = midStripeColorLight
        rectInner = RectF(
            centerX - innerRadius,
            centerY - innerRadius,
            centerX + innerRadius,
            centerY + innerRadius
        )
        canvas.drawArc(rectInner, startAngle.toFloat(), currentAngle - startAngle, false, paintArc)
        paintArc.color = Color.GRAY
        canvas.drawArc(rectInner, currentAngle, 360 - currentAngle + startAngle, false, paintArc)
    }

    private fun drawInnerSlice(canvas: Canvas) {
        paintArc.strokeWidth = Utils.dp2px(context, 1f)
        for (i in 0 until innerSliceCount) {
            val angle = i * innerSliceAngle + startAngle
            val point1 = getCoordinatePoint((innerRadius - Utils.dp2px(context, 2f)).toInt(), angle)
            val point2 = getCoordinatePoint(innerSliceRadius, angle)
            if (angle >= startAngle && angle <= currentAngle) {
                paintArc.color = midStripeColorLight
            } else {
                paintArc.color = Color.GRAY
            }
            canvas.drawLine(point1[0], point1[1], point2[0], point2[1], paintArc)
        }
    }

    private fun drawHeader(canvas: Canvas) {

        val fontMetrics = paintValue.fontMetrics
        val distance = (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        val baseLine = centerY + distance

        canvas.drawText(
            currentValue.toString(),
            centerX,
            baseLine,
            paintValue
        )

        paintText.textSize = valueTextSize.toFloat()
        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = valueTextSize.toFloat() / 3
        val headerFontMetrics = paintText.fontMetrics
        val headerBaseLine =
            centerY + (fontMetrics.bottom - fontMetrics.top) / 2 + (headerFontMetrics.bottom)
        canvas.drawText(
            unitText,
            centerX,
            headerBaseLine,
            paintText
        )


    }

    private fun drawPointer(canvas: Canvas) {
        paintPointer.style = Paint.Style.STROKE
        paintPointer.strokeWidth = Utils.dp2px(context, 2f)
        val point1 = getCoordinatePoint(
            (radius - outStripeWidth - Utils.dp2px(context, 2f)).toInt(),
            currentAngle
        )
        val point2 =
            getCoordinatePoint((innerRadius + Utils.dp2px(context, 2f)).toInt(), currentAngle)
        val shader = LinearGradient(
            point1[0],
            point1[1],
            point2[0],
            point2[1],
            midStripeColorLight,
            innerStripeColorStart,
            Shader.TileMode.CLAMP
        )
        paintPointer.shader = shader
        canvas.drawLine(point1[0], point1[1], point2[0], point2[1], paintPointer)
    }
}