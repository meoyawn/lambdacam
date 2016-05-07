package adeln.telegram.camera.widget

import adeln.telegram.camera.Dimens
import adeln.telegram.camera.R
import adeln.telegram.camera.sharpPaint
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import common.context.color
import common.context.dipF
import org.jetbrains.anko.dip

class WheelView(ctx: Context) : View(ctx) {
  private val lineWidth = dipF(1)
  private val lineHeight = dipF(12)

  private val middleWidth = dipF(3)
  private val middleHeight = dipF(22)

  private val highlightedWidth = dipF(2)
  private val highlightedHeight = dipF(16)

  private val middleRect = RectF()

  private val linePaint = sharpPaint(Color.WHITE).apply { strokeWidth = lineWidth }
  private val bluePaint = sharpPaint(ctx.color(R.color.inner_blue)).apply { strokeWidth = lineWidth }
  private val highlightedPaint = sharpPaint(ctx.color(R.color.inner_blue)).apply { strokeWidth = highlightedWidth }
  private val middlePaint = sharpPaint(ctx.color(R.color.inner_blue))

  private val stepInterpolation = 0.75F
  private val stepInterpolator = DecelerateInterpolator(stepInterpolation)
  private val alpha = DecelerateInterpolator(0.75F)

  private var mScrollStarted = false
  private var mLastTouchedPosition = -1F
  private var totalScroll = 0F

  var scrollingListener: ScrollingListener? = null

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN                          -> mLastTouchedPosition = event.x
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        mScrollStarted = false
        scrollingListener?.onScrollEnd()
      }
      MotionEvent.ACTION_MOVE                          -> {
        val distance = event.x - mLastTouchedPosition
        if (distance != 0f) {
          if (!mScrollStarted) {
            mScrollStarted = true
            scrollingListener?.onScrollStart()
          }
          totalScroll -= distance
          invalidate()
          mLastTouchedPosition = event.x
          scrollingListener?.onAngle(-distance / dip(Dimens.WHEEL_TENSION()), angle())
        }
      }
    }
    return true
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    val cX = w / 2F
    val cY = h / 2F
    val hMW = middleWidth / 2F
    val hLH = middleHeight / 2F
    middleRect.set(cX - hMW, cY - hLH, cX + hMW, cY + hLH)
  }

  private fun degreeRespectingScroll(): Float =
      angle() * dip(Dimens.WHEEL_TENSION())

  fun angle(): Float {
    val balance = -totalScroll.compareTo(0F) * 180
    return ((totalScroll / dip(Dimens.WHEEL_TENSION()) - balance) % 360 + balance)
  }

  fun setAngle(a: Float) {
    totalScroll = a * dip(Dimens.WHEEL_TENSION())
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    val cX = width / 2F
    val cY = height / 2F
    val top = cY + lineHeight / 2F
    val bottom = cY - lineHeight / 2F

    val hTop = cY + highlightedHeight / 2F
    val hBottom = cY - highlightedHeight / 2F

    val times = 16
    val stepWidth = width / (times * 2F - 1)

    val scroll = degreeRespectingScroll()

    val deltaX = scroll % stepWidth

    repeat(times) { i ->
      val fraction = i / times.toFloat()

      val stepFraction = stepInterpolator.getInterpolation(fraction)
      val offset = cX * stepFraction

      val paintAlpha = ((1 - alpha.getInterpolation(fraction)) * 255).toInt()
      linePaint.alpha = paintAlpha
      bluePaint.alpha = paintAlpha
      highlightedPaint.alpha = paintAlpha

      val deltaFraction = if (i > 0) stepFraction / i * times else stepInterpolation * 2 // why 2? it works
      val relativeDelta = deltaFraction * deltaX

      val left = cX - offset - relativeDelta
      val right = cX + offset - relativeDelta

      val xxx = i * stepWidth
      val leftD = scroll - xxx - deltaX
      val rightD = scroll + xxx - deltaX

      val blueLeft = leftD > 0F
      when {
        Math.round(leftD) == 0 -> canvas.drawLine(left, hBottom, left, hTop, highlightedPaint)
        else                   -> canvas.drawLine(left, bottom, left, top, if (blueLeft) bluePaint else linePaint)
      }
      when {
        Math.round(rightD) == 0 -> canvas.drawLine(right, hBottom, right, hTop, highlightedPaint)
        i != 0 || !blueLeft     -> canvas.drawLine(right, bottom, right, top, if (rightD < 0F) bluePaint else linePaint)
      }
    }

    canvas.drawRoundRect(middleRect, dipF(6), dipF(6), middlePaint)
  }

  interface ScrollingListener {
    fun onScrollStart(): Unit
    fun onAngle(delta: Float, angle: Float): Unit
    fun onScrollEnd(): Unit
  }
}
