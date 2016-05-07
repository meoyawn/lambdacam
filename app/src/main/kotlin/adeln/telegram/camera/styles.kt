package adeln.telegram.camera

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import common.context.attr
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.dip
import org.jetbrains.anko.horizontalPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.textColor
import org.jetbrains.anko.wrapContent

fun _FrameLayout.stylePanelText(tv: TextView) {
  tv.lparams(height = matchParent, width = wrapContent)
  tv.gravity = Gravity.CENTER_VERTICAL
  tv.textColor = Color.WHITE
  tv.textSize = Dimens.CROP_TEXT_SIZE()
  tv.backgroundResource = attr(android.R.attr.selectableItemBackground)
  tv.horizontalPadding = dip(16)
  tv.setTypeface(null, Typeface.BOLD)
  tv.setAllCaps(true)
}
