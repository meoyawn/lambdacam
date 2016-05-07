package common.graphics

import android.graphics.Rect
import android.graphics.RectF

operator fun Rect.component1() = left
operator fun Rect.component2() = top
operator fun Rect.component3() = right
operator fun Rect.component4() = bottom

operator fun RectF.component1() = left
operator fun RectF.component2() = top
operator fun RectF.component3() = right
operator fun RectF.component4() = bottom

fun RectF.circle(cx: Float, cy: Float, radius: Float): Unit =
    set(cx - radius, cy - radius, cx + radius, cy + radius)
