package adeln.telegram.camera

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import org.jetbrains.anko.dip

val SWIPE_THRESHOLD = 50
val SWIPE_VELOCITY_THRESHOLD = 100

enum class Gesture {
  SWIPE_LEFT,
  SWIPE_RIGHT,
  LONG_TAP
}

@SuppressLint("ClickableViewAccessibility")
inline fun gestures(view: View,
                    crossinline filter: (MotionEvent) -> Boolean,
                    crossinline onGesture: (Gesture) -> Unit) {
  val ogl = object : GestureDetector.SimpleOnGestureListener() {
    override fun onDown(e: MotionEvent): Boolean = filter(e)
    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
      val diffX = e2.x - e1.x
      if (Math.abs(diffX) > view.dip(SWIPE_THRESHOLD) && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
        onGesture(if (diffX > 0) Gesture.SWIPE_RIGHT else Gesture.SWIPE_LEFT)
      }
      return true
    }

    override fun onLongPress(e: MotionEvent): Unit =
        if (filter(e)) {
          view.performLongClick()
          onGesture(Gesture.LONG_TAP)
        } else Unit
  }
  val gd = GestureDetector(view.context, ogl, MAIN_THREAD)
  view.setOnTouchListener { view, ev -> gd.onTouchEvent(ev) }
}
