package adeln.telegram.camera.widget

import adeln.telegram.camera.Interpolators
import adeln.telegram.camera.R
import adeln.telegram.camera.sharpPaint
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Path
import android.view.View
import common.animation.chainInterpolator
import common.animation.chainUpdateListener
import common.context.dipF
import org.jetbrains.anko.dip

class FacingView(ctx: Context) : View(ctx), ValueAnimator.AnimatorUpdateListener {
  private val arrows = BitmapFactory.decodeResource(resources, R.drawable.switch_arrow)
  private val arrowsMatrix = Matrix()

  private val circlePath = Path().apply { fillType = Path.FillType.WINDING }
  private val circlePaint = sharpPaint(Color.WHITE)

  private val duration = 500L
  private val circleRadius = dip(6).toFloat()
  private val maxDrillRadius = dipF(4)

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

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    arrowsMatrix.setTranslate((w - arrows.width) / 2F, (h - arrows.height) / 2F)

    val cx = w / 2F
    val cy = h / 2F

    circlePath.reset()
    circlePath.addCircle(cx, cy, circleRadius, Path.Direction.CW)
    circlePath.addCircle(cx, cy, maxDrillRadius, Path.Direction.CCW)
  }

  override fun onAnimationUpdate(animation: ValueAnimator) {
    val deg = animation.animatedValue as Float
    val w = width
    val h = height

    val cx = w / 2F
    val cy = h / 2F

    arrowsMatrix.setTranslate((w - arrows.width) / 2F, (h - arrows.height) / 2F)
    arrowsMatrix.postRotate(deg, w / 2F, h / 2F)

    circlePath.reset()
    circlePath.addCircle(cx, cy, circleRadius, Path.Direction.CW)
    val drillRatio = Math.abs(deg - 180F) / 180F
    circlePath.addCircle(cx, cy, drillRatio * maxDrillRadius, Path.Direction.CCW)

    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    canvas.drawBitmap(arrows, arrowsMatrix, null)
    canvas.drawPath(circlePath, circlePaint)
  }
}
