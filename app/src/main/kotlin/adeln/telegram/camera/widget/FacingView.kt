package adeln.telegram.camera.widget

import adeln.telegram.camera.Interpolators
import adeln.telegram.camera.sharpPaint
import adeln.telegram.camera.R
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View
import common.animation.chainInterpolator
import common.animation.chainUpdateListener
import org.jetbrains.anko.dip

class FacingView(ctx: Context) : View(ctx), ValueAnimator.AnimatorUpdateListener {
  private val arrows = BitmapFactory.decodeResource(resources, R.drawable.switch_arrow)
  private val arrowsMatrix = Matrix()

  //  private val arrowsPaint = Paint()
  private val circlePaint = sharpPaint(Color.WHITE)
  // TODO https://medium.com/@rey5137/let-s-drill-a-hole-in-your-view-e7f53fa23376
  private val drillPaint = sharpPaint(Color.TRANSPARENT).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

  private val duration = 500L
  private val circleRadius = dip(6).toFloat()
  private val maxDrillRadius = dip(4)
  private var drillRadius = maxDrillRadius.toFloat()

  fun toFront(): Unit =
      ValueAnimator.ofFloat(0F, 180F)
          .chainUpdateListener(this)
          .setDuration(duration)
          .chainInterpolator(Interpolators.decelerate)
          .start()

  fun toBack(): Unit =
      ValueAnimator.ofFloat(180F, 360F)
          .chainUpdateListener(this)
          .setDuration(duration)
          .chainInterpolator(Interpolators.decelerate)
          .start()

  override fun onAnimationUpdate(animation: ValueAnimator) {
    val value = animation.animatedValue as Float
    val w = width
    val h = height

    arrowsMatrix.setTranslate((w - arrows.width) / 2F, (h - arrows.height) / 2F)
    arrowsMatrix.postRotate(value, w / 2F, h / 2F)
    drillRadius = Math.abs(value - 180F) / 180F * maxDrillRadius

    invalidate()
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    arrowsMatrix.setTranslate((w - arrows.width) / 2F, (h - arrows.height) / 2F)
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawBitmap(arrows, arrowsMatrix, null)
    val cx = width / 2F
    val cy = height / 2F
    canvas.drawCircle(cx, cy, circleRadius, circlePaint)
    canvas.drawCircle(cx, cy, drillRadius, drillPaint)
  }
}
