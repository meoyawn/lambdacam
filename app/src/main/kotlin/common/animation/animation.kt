package common.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.ViewPropertyAnimator

inline fun ViewPropertyAnimator.animationEnd(crossinline f: (Animator) -> Unit): ViewPropertyAnimator =
    setListener(object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        setListener(null)
        f(animation)
      }
    })

inline fun <reified T> animationUpdate(crossinline f: (T) -> Unit): ValueAnimator.AnimatorUpdateListener =
    ValueAnimator.AnimatorUpdateListener {
      f(it.animatedValue as T)
    }

inline fun <T> T.animateInt(from: Int, to: Int, crossinline f: T.(Int) -> Unit): ValueAnimator =
    ValueAnimator.ofInt(from, to)
        .apply {
          addUpdateListener {
            f(it.animatedValue as Int)
          }
        }

object ColorEvaluator : TypeEvaluator<Int> {
  override fun evaluate(fraction: Float, startValue: Int, endValue: Int): Int {
    val startA = Color.alpha(startValue);
    val startR = Color.red(startValue);
    val startG = Color.green(startValue);
    val startB = Color.blue(startValue);
    return Color.argb((startA + (fraction * (Color.alpha(endValue) - startA))).toInt(),
                      (startR + (fraction * (Color.red(endValue) - startR))).toInt(),
                      (startG + (fraction * (Color.green(endValue) - startG))).toInt(),
                      (startB + (fraction * (Color.blue(endValue) - startB))).toInt());
  }
}

inline fun <T> T.animateColor(from: Int, to: Int, crossinline f: T.(Int) -> Unit): ValueAnimator =
    ValueAnimator.ofObject(ColorEvaluator, from, to)
        .apply {
          addUpdateListener {
            f(it.animatedValue as Int)
          }
        }

inline fun <T> T.animateFloat(from: Float, to: Float, crossinline f: T.(Float) -> Unit): ValueAnimator =
    ValueAnimator.ofFloat(from, to)
        .apply {
          addUpdateListener {
            f(it.animatedValue as Float)
          }
        }

fun playSequentially(vararg a: Animator) =
    AnimatorSet().apply { playSequentially(*a) }
