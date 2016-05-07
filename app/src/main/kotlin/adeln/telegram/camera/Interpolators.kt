package adeln.telegram.camera

import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

object Interpolators {
  val accelerate = AccelerateInterpolator(2F)
  val decelerate = DecelerateInterpolator(1F)
  val accelerateDecelerate = AccelerateDecelerateInterpolator()
}
