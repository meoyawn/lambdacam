package adeln.telegram.camera

import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.TextureView
import android.view.View
import common.context.color
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.find
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.textureView
import org.jetbrains.anko.view

fun CameraActivity.layout(panelSize: Int): _FrameLayout =
    frameLayout {
      id = R.id.content

      textureView {
        id = R.id.camera_texture
        lparams(width = matchParent, height = matchParent)
        surfaceTextureListener = listener(this@frameLayout, this@textureView)
      }

      view {
        id = R.id.panel
        backgroundDrawable = ColorDrawable(color(R.color.dark_panel))
        lparams(width = matchParent, height = panelSize, gravity = Gravity.BOTTOM)
      }
    } as _FrameLayout

fun CameraActivity.content(): _FrameLayout = find(R.id.content)
fun _FrameLayout.panel(): View = find(R.id.panel)
fun View.cameraTexture(): TextureView = find(R.id.camera_texture)
