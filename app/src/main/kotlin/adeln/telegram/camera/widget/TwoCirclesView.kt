package adeln.telegram.camera.widget

import adeln.telegram.camera.sharpPaint
import adeln.telegram.camera.R
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.View
import common.animation.animateFloat
import common.animation.playSequentially
import common.context.color
import org.jetbrains.anko.dip

// TODO less animation update allocation
class TwoCirclesView(ctx: Context) : View(ctx) {
  private val leftSplit = RectF()
  private val rightSplit = RectF()

  private val activePaint = sharpPaint(ctx.color(R.color.active_circle))
  private val passivePaint = sharpPaint(ctx.color(R.color.inactive_circle))

  private val circleRadius = dip(4).toFloat()
  private var activePos = circleRadius
    set(value) {
      field = value
      invalidate()
    }

  fun moveRight() {
    val w = width.toFloat()
    val d = circleRadius * 2
    rightSplit.set(w - d, 0f, w, d)
    val move = animateFloat(circleRadius, w - circleRadius) {
      activePos = it
      leftSplit.set(0F, 0F, it + circleRadius, d)
    }
    playSequentially(move, split()).start()
  }

  fun moveLeft() {
    val d = circleRadius * 2
    leftSplit.set(0f, 0f, d, d)
    val move = animateFloat(width - circleRadius, circleRadius) {
      activePos = it
      rightSplit.set(it - circleRadius, 0F, width.toFloat(), circleRadius * 2)
    }
    playSequentially(move, split()).start()
  }

  private fun split(): ValueAnimator {
    val w = width.toFloat()
    val d = circleRadius * 2F
    return animateFloat(w / 2, w - d) {
      rightSplit.set(it, 0F, w, d)
      leftSplit.set(0F, 0F, w - it, d)
      invalidate()
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    val d = circleRadius * 2F
    rightSplit.set(w - d, 0F, w.toFloat(), d)
  }

  override fun onDraw(canvas: Canvas) {
    val radius = circleRadius
    val pp = passivePaint
    canvas.drawRoundRect(leftSplit, radius, radius, pp)
    canvas.drawRoundRect(rightSplit, radius, radius, pp)
    canvas.drawCircle(activePos, radius, radius, activePaint)
  }
}
