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
import adeln.telegram.camera.media.Facing
import adeln.telegram.camera.media.id
import adeln.telegram.camera.navBarSizeIfPresent
import adeln.telegram.camera.push
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.RectF
import android.hardware.Camera
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import com.yalantis.ucrop.view.CropImageView
import common.animation.animationEnd
import common.trycatch.tryTimber
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

fun _FrameLayout.addPicTaken(panelSize: Int): Unit {
  val hugeButton = dip(52)
  val navBarSize = context.navBarSizeIfPresent()

  addCancelDone(panelSize)

  imageView(R.drawable.crop) {
    id = R.id.crop_button
    lparams {
      width = hugeButton
      height = hugeButton
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
      bottomMargin = (panelSize - navBarSize - hugeButton) / 2 + navBarSize
    }

    scaleType = ImageView.ScaleType.FIT_CENTER
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
    is CropScreen -> fromCropScreen(vg)
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

        config(to, vg, b)
      }
    }
  }

  bitmap?.let { config(to, vg, it) }

  vg.cancel().onClick {
    Flow.get(ctx).goBack()
  }
  vg.done().onClick {
    // TODO done
  }
}

fun config(to: TakenScreen, vg: _FrameLayout, bitmap: Bitmap) {
  val cropView = vg.cropView()
  val cs = cropView.currentScale
  cropView.postRotate(to.angle - cropView.currentAngle)

  val cvw = bitmap.width.toFloat()
  val cvh = bitmap.height.toFloat()
  cropView.setCropRect(RectF(0F, 0F, cvw, cvh))

  val rw = to.rect.width()
  val rh = to.rect.height()
  val r = if (rw < rh) cvw / rw else cvh / rh

  cropView.setMaxScaleMultiplier(1000F)
  cropView.zoomInImage(r / cs, to.rect.centerX(), to.rect.centerY())
  cropView.setImageToWrapCropBounds(true)

  vg.cropButton().onClick {
    Flow.get(vg.context).push(CropScreen(bitmap, to.rect, to.angle))
  }
}

private val TEMP_STORAGE = ByteArray(16 * 1024)
private val REUSE_POOL = mutableSetOf<Bitmap>()

fun decodeReuse(bytes: ByteArray): Bitmap {
  val opts = BitmapFactory.Options().apply {
    inSampleSize = 1
    inPreferredConfig = Bitmap.Config.RGB_565
    inMutable = true
    inTempStorage = TEMP_STORAGE
  }

  check(REUSE_POOL.size <= 2)

  return REUSE_POOL.firstOrNull {
    opts.inBitmap = it
    tryTimber { decode(bytes, opts) } != null
  } ?: decode(bytes, opts).apply { REUSE_POOL.add(this) }
}

fun decode(bytes: ByteArray, opts: BitmapFactory.Options): Bitmap =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

fun decodeRotateCut(facing: Facing, bytes: ByteArray): Bitmap {
  val info = Camera.CameraInfo()
  Camera.getCameraInfo(facing.id(), info)

  val orig = decodeReuse(bytes)
  val cutX = (orig.height * Constants.PIC_RATIO).toInt()

  val rotate = Matrix().apply {
    postRotate(info.orientation.toFloat())
    if (facing == Facing.FRONT) {
      postScale(-1F, 1F)
    }
  }

  val x = if (facing == Facing.FRONT) Math.abs(orig.width - cutX) else 0
  // TODO reuse bitmaps
  return Bitmap.createBitmap(orig, x, 0, cutX, orig.height, rotate, false)
}

fun CameraActivity.fromPicTaken(f: _FrameLayout, panelSize: Int) {
  f.cameraTexture().visibility = View.VISIBLE
  window.setBackgroundDrawable(null)

  f.removeView(f.cropView())

  cam?.camera?.startPreview()
  f.addCamButtons(panelSize, cam)

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

  f.flashView().alpha = 0F
  f.flashView().animate()
      .alpha(1F)
      .start()
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
