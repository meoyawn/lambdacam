package adeln.telegram.camera.screen

import adeln.telegram.camera.CamScreen
import adeln.telegram.camera.CameraActivity
import adeln.telegram.camera.Constants
import adeln.telegram.camera.CropScreen
import adeln.telegram.camera.DEGREE_FORMAT
import adeln.telegram.camera.FileAction
import adeln.telegram.camera.Interpolators
import adeln.telegram.camera.MAIN_THREAD
import adeln.telegram.camera.R
import adeln.telegram.camera.Screen
import adeln.telegram.camera.TakenScreen
import adeln.telegram.camera.cropOverlayView
import adeln.telegram.camera.media.MimeTypes
import adeln.telegram.camera.media.notifyGallery
import adeln.telegram.camera.media.open
import adeln.telegram.camera.media.telegramDir
import adeln.telegram.camera.navBarSizeIfPresent
import adeln.telegram.camera.panel
import adeln.telegram.camera.resetTo
import adeln.telegram.camera.stylePanelText
import adeln.telegram.camera.wheelView
import adeln.telegram.camera.widget.CropOverlayView
import adeln.telegram.camera.widget.WheelView
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.yalantis.ucrop.callback.BitmapCropCallback
import common.animation.animationEnd
import common.context.color
import flow.Flow
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.ctx
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.imageView
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.onClick
import org.jetbrains.anko.space
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textView
import org.jetbrains.anko.wrapContent
import timber.log.Timber
import java.io.File

fun _FrameLayout.addCropScreen(w: Int, h: Int, y: Int) {
  val civ = cropView()

  cropOverlayView {
    id = R.id.crop_overlay
    lparams(width = w, height = h)
    cropListener = { civ.setCropRect(it) }
  }

  val ymax = y - context.navBarSizeIfPresent()
  val frac = (ymax - h) / 3

  Timber.d("frac size $frac")

  val degs = textView {
    id = R.id.degrees_text

    lparams {
      width = wrapContent
      height = frac
      gravity = Gravity.CENTER_HORIZONTAL
      topMargin = h
    }

    textSize = 16F
    setTypeface(null, Typeface.BOLD)
    gravity = Gravity.CENTER_VERTICAL
    textColor = Color.WHITE
  }

  wheelView {
    id = R.id.wheel

    lparams {
      width = dip(240)
      height = frac
      gravity = Gravity.CENTER_HORIZONTAL
      topMargin = h + frac
      horizontalMargin = dip(68)
    }

    scrollingListener = object : WheelView.ScrollingListener {
      override fun onAngle(delta: Float, angle: Float) {
        civ.postRotate(delta)
        civ.zoomOutImage(1F)
        civ.setImageToWrapCropBounds(false)
        degs.text = formatAngle(angle)
      }

      override fun onScrollEnd() = Unit
      override fun onScrollStart() = Unit
    }
  }

  imageView(R.drawable.rotate) {
    id = R.id.rotate_button
    lparams {
      width = dip(48)
      height = frac
      gravity = Gravity.END
      topMargin = h + frac
      horizontalMargin = dip(10)
    }
    scaleType = ImageView.ScaleType.CENTER_INSIDE
    backgroundResource = R.drawable.clickable_bg

    onClick {
      civ.postRotate(-90F)
      civ.setImageToWrapCropBounds()
      degs.text = formatAngle(civ.currentAngle)
    }
  }

  linearLayout {
    id = R.id.crop_button_panel
    orientation = LinearLayout.HORIZONTAL

    textView(R.string.cancel) {
      id = R.id.cancel_text
      stylePanelText(this)
    }

    textView(R.string.reset) {
      id = R.id.reset_button
      stylePanelText(this)
    }

    space {
      lparams(weight = 1F, width = 0, height = 0)
    }

    textView(R.string.done) {
      id = R.id.done_button
      stylePanelText(this)
      textColor = color(R.color.inner_blue)
    }
  }.lparams {
    width = matchParent
    height = frac
    topMargin = h + frac + frac
  }
}

fun _FrameLayout.cropOverlay(): CropOverlayView = find(R.id.crop_overlay)
fun _FrameLayout.wheel(): WheelView = find(R.id.wheel)
fun View.degrees(): TextView = find(R.id.degrees_text)
fun View.rotate90(): View = find(R.id.rotate_button)
fun View.cancelText(): View = find(R.id.cancel_text)
fun View.resetText(): View = find(R.id.reset_button)
fun View.doneText(): View = find(R.id.done_button)
fun View.cropButtonPanel(): View = find(R.id.crop_button_panel)

fun _FrameLayout.removeCrop() {
  removeView(cropOverlay())
  removeView(wheel())
  removeView(degrees())
  removeView(rotate90())
  removeView(cropButtonPanel())
}

fun CameraActivity.toCropScreen(size: Point, to: CropScreen, vg: _FrameLayout, from: Screen) {
  val w = size.x
  val h = (w * Constants.PIC_RATIO).toInt()

  val ymax = size.y - navBarSizeIfPresent()
  val where = (ymax - h) / 3F * 2F

  val i = Interpolators.decelerate

  when (from) {
    is TakenScreen -> {
      cancelDoneJump.setAtRest()
      cancelDoneJump.removeAllListeners()

      vg.cancel().animate()
          .translationY(where)
          .alpha(0F)
          .setInterpolator(i)
          .start()
      vg.cropButton().animate()
          .translationY(where)
          .alpha(0F)
          .setInterpolator(i)
          .start()
      vg.done().animate()
          .translationY(where)
          .alpha(0F)
          .setInterpolator(i)
          .animationEnd {
            vg.removePicTaken()
          }
          .start()
    }
    is CropScreen  -> vg.addCropImage(to.bitmap, h, w)
  }

  vg.addCropScreen(w, h, size.y)

  vg.cropButtonPanel().translationY = -where
  vg.cropButtonPanel().animate()
      .translationY(0F)
      .alpha(1F)
      .setInterpolator(i)
      .start()
  vg.panel().animate()
      .translationY(where)
      .setInterpolator(i)
      .start()

  val cropper = vg.cropView()
  cropper.zoomOutImage(1F)

  val overlay = vg.cropOverlay()
  overlay.alpha = 0F
  overlay.animate()
      .alpha(1F)
      .start()

  vg.cancelText().onClick {
    Flow.get(ctx).goBack()
  }

  vg.resetText().onClick {
    cropper.postRotate(-vg.wheel().angle())
    cropper.setCropRect(RectF(0F, 0F, w.toFloat(), h.toFloat()))
    cropper.zoomOutImage(1F)

    vg.wheel().setAngle(0F)
    vg.degrees().text = formatAngle(0F)
    overlay.reset()
    MAIN_THREAD.postDelayed({ cropper.setCropRect(overlay.cropRect) }, 500)
  }

  var cropping = false
  vg.doneText().onClick {
    if (cropping) return@onClick

    cropping = true
    val f = File(telegramDir(), "${System.currentTimeMillis()}.jpg")
    cropper.cropAndSaveImage(
        Constants.COMPRESSION_FORMAT,
        Constants.COMPRESSION_QUALITY,
        Uri.fromFile(f),
        object : BitmapCropCallback {
          override fun onBitmapCropped() {
            notifyGallery(f)
            open(f, MimeTypes.JPEG)
            Flow.get(ctx).resetTo(CamScreen(FileAction.DELETE))
          }

          override fun onCropFailure(bitmapCropException: Exception): Unit =
              Timber.e(bitmapCropException, "")
        }
    )
  }
}

fun formatAngle(angle: Float): String =
    "${DEGREE_FORMAT.format(angle % 360F)}\u00B0"

fun fromCropScreen(vg: _FrameLayout, b: Bitmap) {
  val i = Interpolators.decelerate

  val panel = vg.panel()
  val where = panel.height / 2F
  vg.cropButtonPanel().animate()
      .translationY(-where)
      .alpha(0F)
      .setInterpolator(i)
      .start()

  vg.removeView(panel)
  vg.addView(panel, 5)

  panel.animate()
      .translationY(0F)
      .setInterpolator(i)
      .start()

  vg.cropButton().alpha = 0F
  vg.cropButton().translationY = where
  vg.cropButton().animate()
      .translationY(0F)
      .alpha(1F)
      .setInterpolator(i)
      .start()

  vg.cancel().alpha = 0F
  vg.cancel().translationY = where
  vg.cancel().animate()
      .translationY(0F)
      .alpha(1F)
      .setInterpolator(i)
      .start()

  vg.done().alpha = 0F
  vg.done().translationY = where
  vg.done().animate()
      .translationY(0F)
      .alpha(1F)
      .setInterpolator(i)
      .start()

  vg.cropOverlay().animate()
      .alpha(0F)
      .animationEnd {
        vg.removeCrop()
      }
      .start()

  vg.cropView().postRotate(-vg.cropView().currentAngle)
  vg.cropView().setCropRect(RectF(0F, 0F, b.width.toFloat(), b.height.toFloat()))
  vg.cropView().zoomOutImage(1F)
  vg.cropView().setImageToWrapCropBounds(true)
}
