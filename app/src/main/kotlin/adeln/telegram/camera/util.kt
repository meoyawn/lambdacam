package adeln.telegram.camera

import android.content.Context
import android.graphics.Paint
import android.view.Display
import org.jetbrains.anko.windowManager

fun sharpPaint(clr: Int): Paint =
    Paint().apply {
      isAntiAlias = true
      isDither = true
      color = clr
    }

private fun navBarSize(d: Display): Int =
    Math.abs(Displays.getSize(d).y - Displays.getRealSize(d).y)

fun Context.navBarSizeIfPresent(): Int =
    navBarSize(windowManager.defaultDisplay)

enum class LeftRight {
  LEFT,
  RIGHT
}
