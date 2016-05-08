package adeln.telegram.camera.widget

import adeln.telegram.camera.Dimens
import adeln.telegram.camera.R
import adeln.telegram.camera.SPRING_SYSTEM
import adeln.telegram.camera.sharpPaint
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.View
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringListener
import common.animation.animateFloat
import common.animation.chainDelay
import common.animation.onUpdate
import common.context.color
import common.graphics.circle
import org.jetbrains.anko.dip

class ShootButton(ctx: Context) : View(ctx), SpringListener {
  val redSpring = SPRING_SYSTEM.createSpring().apply {
    springConfig = SpringConfig(500.0, 12.0)
  }

  val border = RectF()
  val outerBlue = RectF()
  val stopSquare = RectF()

  val whitePaint = sharpPaint(Color.WHITE)
  val outerBluePaint = sharpPaint(ctx.color(R.color.outer_blue))
  val innerRedPaint = sharpPaint(ctx.color(R.color.red))
  val stopSquarePaint = sharpPaint(ctx.color(R.color.inner_blue))

  val maxOuterBlueRadius = dip(30).toFloat()
  val maxInnerBlueRadius = dip(18).toFloat()
  val maxInnerRedRadius = dip(10).toFloat()
  val roundCorners = dip(36).toFloat()
  val maxStopSquareRadius = dip(15).toFloat()

  var innerRedRadius = 0F
  var stopSquareRadius = roundCorners

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    redSpring.addListener(this@ShootButton)
  }

  override fun onDetachedFromWindow() {
    redSpring.removeAllListeners()
    super.onDetachedFromWindow()
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    val cx = w / 2f
    val cy = h / 2f
    border.circle(cx, cy, dip(Dimens.HUGE_BUTTON_HEIGHT()) / 2F)
    outerBlue.circle(cx, cy, maxOuterBlueRadius)
    stopSquare.circle(cx, cy, maxInnerBlueRadius)
  }

  override fun onDraw(canvas: Canvas) {
    val round = roundCorners
    val cx = width / 2F
    val cy = height / 2F

    canvas.drawRoundRect(border, round, round, whitePaint)
    canvas.drawRoundRect(outerBlue, round, round, outerBluePaint)
    canvas.drawCircle(cx, cy, innerRedRadius, innerRedPaint)
    canvas.drawRoundRect(stopSquare, stopSquareRadius, stopSquareRadius, stopSquarePaint)
  }

  override fun onSpringUpdate(spring: Spring) {
    innerRedRadius = spring.currentValue.toFloat()
    invalidate()
  }

  fun shoot() {
    val cx = width / 2F
    val cy = height / 2F
    ValueAnimator.ofFloat(maxOuterBlueRadius, maxInnerRedRadius, maxOuterBlueRadius)
        .onUpdate<Float> {
          outerBlue.circle(cx, cy, it)
          invalidate()
        }
        .start()
  }

  fun toVideo() {
    val off = (width - dip(Dimens.HUGE_BUTTON_HEIGHT())) / 2F
    animateBlue(0F, to = off)
    animateSmallRedRecord(from = 0f, to = maxInnerRedRadius, delay = 200L)
  }

  fun toPicture() {
    val off = (width - dip(Dimens.HUGE_BUTTON_HEIGHT())) / 2F
    animateBlue(from = off, to = 0F)
    animateSmallRedRecord(from = maxInnerRedRadius, to = 0f, delay = 0L)
  }

  private fun animateBlue(from: Float, to: Float) {
    val h = dip(Dimens.HUGE_BUTTON_HEIGHT())
    val top = (height - h) / 2F
    val w = width
    val bw = h / 2F - maxOuterBlueRadius
    val off = (w - h) / 2F

    stopSquareRadius = roundCorners
    stopSquarePaint.color = context.color(R.color.inner_blue)

    animateFloat(from, to) { incr ->
      border.set(off - incr, top, w - off + incr, top + h)
      outerBlue.set(bw + off - incr, bw + top, w - bw - off + incr, h - bw + top)

      val ratio = incr.toFloat() / off

      val a = (1F - ratio) * 255F
      outerBluePaint.alpha = a.toInt()

      val blueRadius = (1F - ratio) * maxInnerBlueRadius
      stopSquare.set(w / 2f - blueRadius, h / 2f - blueRadius + top, w / 2f + blueRadius, h / 2f + blueRadius + top)

      invalidate()
    }.start()
  }

  private fun animateSmallRedRecord(from: Float, to: Float, delay: Long): Unit {
    stopSquare.setEmpty()
    redSpring.setAtRest()

    animateFloat(from, to) {
      innerRedRadius = it
      invalidate()
    }.chainDelay(delay).start()
  }

  fun startRecording() {
    val h = dip(Dimens.HUGE_BUTTON_HEIGHT())
    animateBorderCollapse(from = 0F, to = width / 2F)
    animateBigRedRecord(from = maxInnerRedRadius, to = h / 2F)
    animateWhiteRecord(from = 0f, to = maxStopSquareRadius, dur = 200L)
  }

  fun stopRecording() {
    val h = dip(Dimens.HUGE_BUTTON_HEIGHT())
    animateBorderCollapse(from = width / 2F, to = 0F)
    animateBigRedRecord(from = h / 2F, to = maxInnerRedRadius)
    animateWhiteRecord(from = maxStopSquareRadius, to = 0F, dur = 0L)
  }

  private fun animateBigRedRecord(from: Float, to: Float) {
    if (from < to) {
      redSpring.currentValue = from.toDouble()
      redSpring.setAtRest()
      redSpring.endValue = to.toDouble()
    } else {
      redSpring.setAtRest()
      animateFloat(innerRedRadius, to) {
        innerRedRadius = it
        invalidate()
      }.start()
    }
  }

  private fun animateBorderCollapse(from: Float, to: Float) {
    val h = dip(Dimens.HUGE_BUTTON_HEIGHT())
    val top = (height - h) / 2F
    val w = width
    val qh = h / 5F
    animateFloat(from, to) {
      val frac = it / (w / 2F) * qh
      border.set(it, frac + top, w - it, h - frac + top)
      invalidate()
    }.start()
  }

  private fun animateWhiteRecord(from: Float, to: Float, dur: Long) {
    val h = dip(Dimens.HUGE_BUTTON_HEIGHT())
    val top = (height - h) / 2F
    val w = width
    val hw = w / 2F
    val hh = h / 2F

    stopSquarePaint.color = Color.WHITE
    animateFloat(from, to) {
      stopSquare.set(hw - it, hh - it + top, hw + it, hh + it + top)
      val f = it / maxStopSquareRadius
      stopSquareRadius = (1.1F - f) * roundCorners
      invalidate()
    }.setDuration(dur).start()
  }

  override fun onSpringActivate(spring: Spring?) = Unit
  override fun onSpringAtRest(spring: Spring?) = Unit
  override fun onSpringEndStateChange(spring: Spring?) = Unit
}
