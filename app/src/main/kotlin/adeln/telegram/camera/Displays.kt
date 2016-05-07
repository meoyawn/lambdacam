package adeln.telegram.camera

import android.graphics.Point
import android.os.Build
import android.view.Display

object Displays {
  private val size = Point()
  private val realSize = Point()

  fun getSize(d: Display): Point =
      size.apply { d.getSize(this) }

  fun getRealSize(d: Display): Point =
      when {
        Build.VERSION.SDK_INT >= 19 -> realSize.apply { d.getRealSize(this) }
        else                        -> getSize(d)
      }
}
