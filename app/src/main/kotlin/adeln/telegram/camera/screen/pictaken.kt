package adeln.telegram.camera.screen

import adeln.telegram.camera.BACKGROUND_THREAD
import adeln.telegram.camera.CamScreen
import adeln.telegram.camera.CameraActivity
import adeln.telegram.camera.Constants
import adeln.telegram.camera.CropScreen
import adeln.telegram.camera.MAIN_THREAD
import adeln.telegram.camera.R
import adeln.telegram.camera.Screen
import adeln.telegram.camera.TakenScreen
import adeln.telegram.camera.cameraTexture
import adeln.telegram.camera.media.MimeTypes
import adeln.telegram.camera.media.decodeRotateCut
import adeln.telegram.camera.media.notifyGallery
import adeln.telegram.camera.media.open
import adeln.telegram.camera.media.telegramDir
import adeln.telegram.camera.navBarSizeIfPresent
import adeln.telegram.camera.push
import android.graphics.Bitmap
import android.graphics.Point
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import com.yalantis.ucrop.view.CropImageView
import common.android.execute
import common.animation.animationEnd
import common.benchmark.benchmark
import flow.Flow
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.ctx
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.imageView
import org.jetbrains.anko.onClick
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

fun _FrameLayout.addPicTaken(panelSize: Int): Unit {
  val circleSize = dip(52)
  val navBarSize = context.navBarSizeIfPresent()

  addCancelDone(panelSize)

  imageView(R.drawable.crop) {
    id = R.id.crop_button
    lparams {
      width = circleSize
      height = circleSize
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
      bottomMargin = (panelSize - navBarSize - circleSize) / 2 + navBarSize
    }

    scaleType = ImageView.ScaleType.CENTER_INSIDE
    backgroundResource = R.drawable.clickable_bg
  }
}

fun _FrameLayout.addCancelDone(panelSize: Int) {
  val circleSize = dip(52)
  val radius = circleSize / 2F
  val navBarSize = context.navBarSizeIfPresent()
  val bMargin = (panelSize - navBarSize - circleSize) / 2 + navBarSize

  imageView(R.drawable.clickable_cancel) {
    id = R.id.cancel_circle
    lparams {
      width = circleSize
      height = circleSize
      gravity = Gravity.BOTTOM
      bottomMargin = bMargin
      horizontalMargin = dip(16)
    }

    scaleType = ImageView.ScaleType.CENTER_INSIDE
    pivotX = radius
    pivotY = radius
  }

  imageView(R.drawable.clickable_done) {
    id = R.id.done_circle
    lparams {
      width = circleSize
      height = circleSize
      gravity = Gravity.BOTTOM or Gravity.RIGHT
      horizontalMargin = dip(16)
      bottomMargin = bMargin
    }

    scaleType = ImageView.ScaleType.CENTER_INSIDE
    pivotX = radius
    pivotY = radius
  }
}

fun _FrameLayout.addCropImage(b: Bitmap, h: Int, w: Int) =
    CropImageView(context).apply {
      id = R.id.crop_view
      Timber.d("crop image size $w x $h")
      lparams(width = w, height = h)
      setImageBitmap(b)

      addView(this, 1)
    }

fun _FrameLayout.cropView(): CropImageView = find(R.id.crop_view)
fun _FrameLayout.cancel(): View = find(R.id.cancel_circle)
fun _FrameLayout.cropButton(): View = find(R.id.crop_button)
fun _FrameLayout.done(): View = find(R.id.done_circle)

fun _FrameLayout.removePicTaken() {
  removeView(cropButton())
  removeCancelDone()
}

fun _FrameLayout.removeCancelDone() {
  removeView(cancel())
  removeView(done())
}

fun CameraActivity.toPicTaken(panelSize: Int, to: TakenScreen, vg: _FrameLayout, from: Screen, size: Point) {
  vg.addPicTaken(panelSize)

  when (from) {
    is CamScreen  -> fromCamScreen(vg)
    is CropScreen -> fromCropScreen(vg, from)
  }

  var bitmap: Bitmap? = if (from is CropScreen) from.bitmap else null
  if (bitmap == null) {
    BACKGROUND_THREAD.execute {
      val b = decodeRotateCut(facing, to.bytes)
      bitmap = b
      MAIN_THREAD.post {
        window.setBackgroundDrawableResource(android.R.color.black)
        vg.cameraTexture().setOnTouchListener(null)
        vg.cameraTexture().visibility = View.GONE

        vg.addCropImage(b, (size.x * Constants.PIC_RATIO).toInt(), size.x)
      }
    }
  }

  vg.cancel().onClick {
    Flow.get(ctx).goBack()
  }

  vg.cropButton().onClick {
    bitmap?.let {
      Flow.get(vg.context).push(CropScreen(it))
    }
  }

  var writing = false
  vg.done().onClick {
    if (writing) return@onClick
    val b = bitmap ?: return@onClick

    window.setBackgroundDrawableResource(android.R.color.black)
    writing = true

    BACKGROUND_THREAD.execute {
      benchmark("write") {
        val f = File(telegramDir(), "${System.currentTimeMillis()}.jpg")

        BufferedOutputStream(FileOutputStream(f)).use {
          b.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }

        notifyGallery(f)
        open(f, MimeTypes.JPEG)
      }

      MAIN_THREAD.execute {
        Flow.get(ctx).goBack()
      }
    }
  }
}

fun CameraActivity.fromPicTaken(f: _FrameLayout, panelSize: Int) {
  f.cameraTexture().visibility = View.VISIBLE
  window.setBackgroundDrawable(null)

  f.removeView(f.cropView())

  cam?.camera?.startPreview()
  f.addCamButtons(panelSize)

  cancelDoneJump.setAtRest()
  cancelDoneJump.removeAllListeners()
  f.cancel().animate()
      .scaleX(0F)
      .scaleY(0F)
      .start()
  f.done().animate()
      .scaleX(0F)
      .scaleY(0F)
      .start()
  f.cropButton().animate()
      .alpha(0F)
      .start()

  f.flashView()?.alpha = 0F
  f.flashView()?.animate()
      ?.alpha(1F)
      ?.start()
  f.twoCircles().alpha = 0F
  f.twoCircles().animate()
      .alpha(1F)
      .start()
  f.shootView().alpha = 0F
  f.shootView().animate()
      .alpha(1F)
      .start()
  f.facingView().alpha = 0F
  f.facingView().animate()
      .alpha(1F)
      .animationEnd {
        f.removePicTaken()
      }
      .start()
}
