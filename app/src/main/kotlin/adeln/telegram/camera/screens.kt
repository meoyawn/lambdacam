package adeln.telegram.camera

import adeln.telegram.camera.media.stopRecorder
import android.graphics.Bitmap
import android.graphics.RectF
import android.hardware.Camera
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File

interface Screen

object CamScreen : Screen

class TakenScreen(
    val bytes: ByteArray,
    val rect: RectF?,
    val angle: Float
) : Screen

class CropScreen(
    val bitmap: Bitmap,
    val rect: RectF,
    val angle: Float
) : Screen

class VideoRecording(
    val recorder: MediaRecorder,
    val camera: Camera,
    val file: File,
    val updater: Runnable
) : Screen

fun VideoRecording.stopRecorder() {
  MAIN_THREAD.removeCallbacks(updater)
  recorder.stopRecorder()
  camera.reconnect()
}

class StopRecording(
    val rec: VideoRecording
) : Screen

class PlayerScreen(
    val file: File,
    val player: MediaPlayer
) : Screen

fun PlayerScreen.release() {
  player.release()
}

fun PlayerScreen.delete() {
  release()
  file.delete()
}
