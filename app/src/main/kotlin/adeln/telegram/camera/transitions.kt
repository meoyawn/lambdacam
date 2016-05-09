package adeln.telegram.camera

import adeln.telegram.camera.media.State
import adeln.telegram.camera.media.close
import adeln.telegram.camera.media.open
import adeln.telegram.camera.media.supportedFlashes
import adeln.telegram.camera.screen.flashView
import adeln.telegram.camera.screen.startRecording
import adeln.telegram.camera.screen.stopRecording
import adeln.telegram.camera.screen.toCamScreen
import adeln.telegram.camera.screen.toCropScreen
import adeln.telegram.camera.screen.toPicTaken
import adeln.telegram.camera.screen.toPlayer
import adeln.telegram.camera.screen.toRecording
import adeln.telegram.camera.widget.calc
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.view.TextureView
import common.android.execute
import common.animation.animationEnd
import flow.Flow
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.ctx

fun CameraActivity.dispatch(vg: _FrameLayout, t: Flow.Traversal, tcb: Flow.TraversalCallback, size: Point) {
  val panelSize = (size.y - size.x * Constants.PIC_RATIO).toInt()
  val from = t.origin.top<Screen>()
  val to = t.destination.top<Screen>()
  when (to) {
    is CamScreen      -> toCamScreen(from, panelSize, vg, to)
    is TakenScreen    -> toPicTaken(panelSize, to, vg, from, size)
    is CropScreen     -> toCropScreen(size, to, vg, from)
    is StartRecording -> startRecording(vg, to)
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
          val c = open(facing, mode, tv)
          cam = c

          val sf = supportedFlashes(mode, c.camera.parameters.supportedFlashModes)
          val cur = calc(sf, flash)
          flash = cur?.let { sf[it] }

          MAIN_THREAD.execute {
            vg.flashView()?.run {
              alpha = 0F
              animate()
                  .alpha(1F)
                  .start()
              setFlash(sf, cur)
            }
          }
        }
      }

      override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        val flow = Flow.get(ctx)
        val top = flow.history.top<Screen>()
        when (top) {
          is StartRecording -> flow.goBack()
          is VideoRecording -> flow.replace(StopRecording(top))
        }

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
