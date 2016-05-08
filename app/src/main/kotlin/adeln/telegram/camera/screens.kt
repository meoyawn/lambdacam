package adeln.telegram.camera

import adeln.telegram.camera.media.stopRecorder
import android.graphics.Bitmap
import android.hardware.Camera
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File

interface Screen

enum class FileAction {  RETAIN, DELETE }

class CamScreen(
    val action: FileAction
) : Screen

class TakenScreen(
    val bytes: ByteArray
) : Screen

class CropScreen(
    val bitmap: Bitmap
) : Screen

class VideoRecording(
    val recorder: MediaRecorder,
    val camera: Camera,
    val file: File,
    val updater: Runnable
) : Screen

class StopRecording(
    val rec: VideoRecording
) : Screen

class PlayerScreen(
    val file: File,
    val player: MediaPlayer
) : Screen


fun VideoRecording.stopRecorder() {
  MAIN_THREAD.removeCallbacks(updater)
  recorder.stopRecorder()
  camera.reconnect()
}

fun PlayerScreen.release() {
  player.release()
}
