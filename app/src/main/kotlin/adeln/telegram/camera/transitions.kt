package adeln.telegram.camera

import adeln.telegram.camera.media.State
import adeln.telegram.camera.media.close
import adeln.telegram.camera.media.open
import adeln.telegram.camera.screen.flashView
import adeln.telegram.camera.screen.stopRecording
import adeln.telegram.camera.screen.toCamScreen
import adeln.telegram.camera.screen.toCropScreen
import adeln.telegram.camera.screen.toPicTaken
import adeln.telegram.camera.screen.toPlayer
import adeln.telegram.camera.screen.toRecording
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.view.TextureView
import common.android.execute
import common.animation.animationEnd
import flow.Flow
import org.jetbrains.anko._FrameLayout

fun CameraActivity.dispatch(vg: _FrameLayout, t: Flow.Traversal, tcb: Flow.TraversalCallback, size: Point) {
  val panelSize = (size.y - size.x * Constants.PIC_RATIO).toInt()
  val from = t.origin.top<Screen>()
  val to = t.destination.top<Screen>()
  when (to) {
    is CamScreen      -> toCamScreen(from, panelSize, vg)
    is TakenScreen    -> toPicTaken(panelSize, to, vg, from, size)
    is CropScreen     -> toCropScreen(size, to, vg, from)
    is VideoRecording -> toRecording(vg, to)
    is StopRecording  -> stopRecording(vg, to)
    is PlayerScreen   -> toPlayer(vg, to)
    else              -> error("exhaust")
  }
  tcb.onTraversalCompleted()
}

fun CameraActivity.listener(vg: _FrameLayout, tv: TextureView) =
    object : TextureView.SurfaceTextureListener {
      override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        CAMERA_THREAD.execute {
          val c = open(facing, mode, flash, tv)
          cam = c
          MAIN_THREAD.execute {
            val fv = vg.flashView()
            fv.alpha = 0F
            fv.animate()
                .alpha(1F)
                .start()
            flash = fv.setFlash(c.flashes, flash)
          }
        }
      }

      override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        window.setBackgroundDrawableResource(android.R.color.black)
        camState = State.CLOSING
        CAMERA_THREAD.execute {
          cam?.camera?.close()
          camState = State.CLOSED
          cam = null
        }
        return false
      }

      override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) = Unit
      override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        if (camState == State.CLOSED) {
          tv.alpha = 0F
          tv.animate()
              .alpha(1F)
              .setDuration(200)
              .animationEnd { window.setBackgroundDrawable(null) }
              .start()
          camState = State.OPEN
        } else Unit
      }
    }
