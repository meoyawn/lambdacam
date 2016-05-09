@file:Suppress("DEPRECATION")

package adeln.telegram.camera.media

import android.hardware.Camera
import android.hardware.Camera.Parameters.FOCUS_MODE_AUTO
import android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
import android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
import android.hardware.Camera.Parameters.FOCUS_MODE_EDOF
import android.hardware.Camera.Parameters.FOCUS_MODE_FIXED
import android.view.TextureView
import common.android.assertWorkerThread
import common.benchmark.benchmark
import org.jetbrains.anko.collections.forEachByIndex
import timber.log.Timber

// #enumsmatter

enum class Facing { BACK, FRONT }
enum class Mode { PICTURE, VIDEO }
enum class State { OPEN, CLOSING, CLOSED }
enum class Flash { AUTO, ON, OFF, TORCH }

fun Facing.id(): Int =
    when (this) {
      Facing.BACK  -> 0
      Facing.FRONT -> 1
    }

fun toString(flash: Flash): String =
    when (flash) {
      Flash.AUTO  -> Camera.Parameters.FLASH_MODE_AUTO
      Flash.OFF   -> Camera.Parameters.FLASH_MODE_OFF
      Flash.ON    -> Camera.Parameters.FLASH_MODE_ON
      Flash.TORCH -> Camera.Parameters.FLASH_MODE_TORCH
    }

fun fromString(it: String): Flash? =
    when (it) {
      Camera.Parameters.FLASH_MODE_AUTO  -> Flash.AUTO
      Camera.Parameters.FLASH_MODE_OFF   -> Flash.OFF
      Camera.Parameters.FLASH_MODE_ON    -> Flash.ON
      Camera.Parameters.FLASH_MODE_TORCH -> Flash.TORCH
      else                               -> null
    }

private val picFlashes = arrayOf(Flash.AUTO, Flash.ON, Flash.OFF)
private val videoFlashes = arrayOf(Flash.TORCH, Flash.OFF)

fun supportedFlashes(mode: Mode, flashes: List<String>?): List<Flash> =
    mutableListOf<Flash>().apply {
      val all = if (mode == Mode.PICTURE) picFlashes else videoFlashes
      flashes?.forEachByIndex { s ->
        fromString(s)?.let { f ->
          if (f in all)
            add(f)
        }
      }
    }

fun open(facing: Facing, mode: Mode, tv: TextureView): FacingCamera =
    benchmark("open") {
      val cam = Camera.open(facing.id())
      assertWorkerThread()
      Timber.d("opening with ${tv.width} x ${tv.height}")

      val height = tv.height
      val width = tv.width

      val ps = cam.parameters
      ps.config(mode, height, width)
      cam.parameters = ps

      val info = Camera.CameraInfo()
      Camera.getCameraInfo(facing.id(), info)
      val orient = when (facing) {
        Facing.FRONT -> Math.abs(360 - info.orientation) % 360
        Facing.BACK  -> info.orientation
      }
      cam.setDisplayOrientation(orient)

      cam.setPreviewTexture(tv.surfaceTexture)
      cam.startPreview()

      FacingCamera(cam, facing)
    }

fun closest(allSupportSizes: List<Camera.Size>, preferW: Int, preferH: Int): Camera.Size {
  val r = preferW / preferH.toFloat()

  var minRatioDiff = Float.MAX_VALUE
  var minSizeDiff = Int.MAX_VALUE

  var chosen: Camera.Size? = null
  allSupportSizes.forEachByIndex { size ->
    val w = size.width
    val h = size.height

    val ratioDiff = Math.abs(w / h.toFloat() - r)
    val sizeDiff = Math.abs(w - preferW) + Math.abs(h - preferH)

    if (ratioDiff <= minRatioDiff && sizeDiff <= minSizeDiff) {
      minSizeDiff = sizeDiff
      minRatioDiff = ratioDiff
      chosen = size
    }
  }

  Timber.d("choosing ${chosen?.width} x ${chosen?.height}")

  return chosen!!;
}

fun Camera.close() =
    benchmark("close") {
      assertWorkerThread()
      stopPreview()
      release()
    }

private val bestPictureFocuses: Array<String> =
    arrayOf(FOCUS_MODE_CONTINUOUS_PICTURE, FOCUS_MODE_AUTO, FOCUS_MODE_EDOF, FOCUS_MODE_FIXED)
private val bestVideoFocuses: Array<String> =
    arrayOf(FOCUS_MODE_CONTINUOUS_VIDEO, FOCUS_MODE_AUTO, FOCUS_MODE_EDOF, FOCUS_MODE_FIXED)

fun Mode.focus(focuses: List<String>): String =
    when (this) {
      Mode.PICTURE -> bestPictureFocuses.first { it in focuses }
      Mode.VIDEO   -> bestVideoFocuses.first { it in focuses }
    }

data class FacingCamera(
    val camera: Camera,
    val facing: Facing
)

fun Camera.Parameters.config(mode: Mode, preferW: Int, preferH: Int) {
  set("cam_mode", 1) // don't even ask
  focusMode = mode.focus(supportedFocusModes)

  val preview = closest(supportedPreviewSizes, preferW, preferH)
  setPreviewSize(preview.width, preview.height)

  val picture = closest(supportedPictureSizes, preferW, preferH)
  setPictureSize(picture.width, picture.height)
}

val SHUTTER = Camera.ShutterCallback {}
