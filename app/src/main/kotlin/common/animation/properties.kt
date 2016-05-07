package common.animation

import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.View
import org.jetbrains.anko.backgroundDrawable

fun ColorDrawable.animateAlpha(from: Int, to: Int): ValueAnimator =
    animateInt(from, to) {
      alpha = it
    }

fun View.animateBackgroundColor(from: Int, to: Int): ValueAnimator =
    (backgroundDrawable as ColorDrawable).animateColor(from, to)

fun ColorDrawable.animateColor(from: Int, to: Int): ValueAnimator =
    animateColor(from, to) { color = it }

fun View.animateTranslationX(from: Float, to: Float): ValueAnimator =
    animateFloat(from, to) {
      translationX = it
    }
