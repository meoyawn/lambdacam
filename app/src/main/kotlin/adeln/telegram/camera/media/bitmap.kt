package adeln.telegram.camera.media

import adeln.telegram.camera.Constants
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import common.trycatch.tryTimber
import java.util.ArrayList

private val TEMP_STORAGE = ByteArray(16 * 1024)
private val DECODE_POOL = ArrayList<Bitmap>()
private val TRANSFORM_POOL = ArrayList<Bitmap>()

fun decodeReuse(bytes: ByteArray): Bitmap {
  val opts = BitmapFactory.Options().apply {
    inSampleSize = 1
    inPreferredConfig = Bitmap.Config.RGB_565
    inMutable = true
    inTempStorage = TEMP_STORAGE
  }

  return DECODE_POOL.firstOrNull { tryTimber { decode(bytes, opts.apply { inBitmap = it }) } != null }
      ?: decode(bytes, opts.apply { inBitmap = null }).apply { DECODE_POOL.add(this) }
}

fun decode(bytes: ByteArray, opts: BitmapFactory.Options): Bitmap =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

fun decodeRotateCut(facing: Facing, bytes: ByteArray): Bitmap {
  val info = Camera.CameraInfo()
  Camera.getCameraInfo(facing.id(), info)

  val source = decodeReuse(bytes)

  val cutWidth = (source.height * Constants.PIC_RATIO).toInt()
  val startX = if (info.orientation == 270) Math.abs(source.width - cutWidth) else 0

  val bitmap = TRANSFORM_POOL.firstOrNull { it.width == source.height && it.height == cutWidth }
      ?: Bitmap.createBitmap(source.height, cutWidth, Bitmap.Config.RGB_565).apply { TRANSFORM_POOL.add(this) }

  val m = Matrix().apply {
    postRotate(info.orientation.toFloat())
    if (facing == Facing.FRONT) {
      postScale(-1F, 1F)
    }
  }

  val x = startX
  val y = 0
  val width = cutWidth
  val height = source.height

  val srcR = Rect(x, y, x + width, y + height)
  val dstR = RectF(0f, 0f, width.toFloat(), height.toFloat())
  val deviceR = RectF()
  m.mapRect(deviceR, dstR)

  val canvas = Canvas()
  canvas.translate(-deviceR.left, -deviceR.top)
  canvas.concat(m)
  canvas.setBitmap(bitmap)
  canvas.drawBitmap(source, srcR, dstR, null)
  canvas.setBitmap(null)

  return bitmap
}
