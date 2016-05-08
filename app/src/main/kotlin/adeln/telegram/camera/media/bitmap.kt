package adeln.telegram.camera.media

import adeln.telegram.camera.Constants
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import common.trycatch.tryTimber

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

  return REUSE_POOL.firstOrNull { tryTimber { decode(bytes, opts.apply { inBitmap = it }) } != null }
      ?: decode(bytes, opts.apply { inBitmap = null }).apply { REUSE_POOL.add(this) }
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
