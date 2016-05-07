package adeln.telegram.camera.widget

import adeln.telegram.camera.Interpolators
import adeln.telegram.camera.R
import adeln.telegram.camera.media.Flash
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import common.android.assertMainThread
import common.animation.chainInterpolator
import common.animation.chainUpdateListener
import common.context.dipF

class FlashView(ctx: Context) : View(ctx), ValueAnimator.AnimatorUpdateListener {
  val bitmaps = run {
    val res = resources
    val on = BitmapFactory.decodeResource(res, R.drawable.flash_on)
    mapOf(
        Flash.AUTO to BitmapFactory.decodeResource(res, R.drawable.flash_auto),
        Flash.ON to on,
        Flash.OFF to BitmapFactory.decodeResource(res, R.drawable.flash_off),
        Flash.TORCH to on
    )
  }

  val imgSize = dipF(32)
  val activePaint = Paint()
  val inactivePaint = Paint().apply { alpha = 0 }

  private var supported: List<Flash> = emptyList()
  var current: Int? = null
  var translate = imgSize

  override fun onDraw(canvas: Canvas) {
    current?.let {
      canvas.drawBitmap(bitmaps[supported[it]], 0F, translate, activePaint)
    }
    prev()?.let {
      canvas.drawBitmap(bitmaps[supported[it]], 0F, translate + imgSize, inactivePaint)
    }
  }

  fun setFlash(fs: List<Flash>, f: Flash?): Flash? {
    assertMainThread()

    val before = current?.let { supported[it] }
    supported = fs
    current = when {
      f in fs         -> fs.indexOf(f)
      fs.isNotEmpty() -> 0
      else            -> null
    }
    val after = current?.let { supported[it] }

    if (after != before) animateChange()

    return after
  }

  fun setNext(): Flash? =
      next()?.let { i ->
        current = i
        animateChange()
        supported[i]
      }

  private fun animateChange() =
      ValueAnimator.ofFloat(0F, imgSize)
          .chainUpdateListener(this)
          .chainInterpolator(Interpolators.decelerate)
          .start()

  override fun onAnimationUpdate(animation: ValueAnimator) {
    val it = animation.animatedValue as Float
    val r = 1 - it / imgSize
    translate = it
    inactivePaint.alpha = (r * 255).toInt()
    invalidate()
  }

  private fun prev() =
      current?.let { if (it == 0) supported.size - 1 else it.dec() }

  private fun next() =
      current?.let { it.inc() % supported.size }
}
