package adeln.telegram.camera.media

import adeln.telegram.camera.MAIN_THREAD
import adeln.telegram.camera.VideoRecording
import android.content.Context
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.SystemClock
import android.support.annotation.WorkerThread
import common.android.assertWorkerThread
import common.android.whenSdk
import common.trycatch.tryTimber
import java.io.File

private val preferredQualities: List<Int> =
    listOf(
        CamcorderProfile.QUALITY_HIGH
        , CamcorderProfile.QUALITY_480P
        , CamcorderProfile.QUALITY_720P
        , CamcorderProfile.QUALITY_1080P
    )

fun profile(id: Int): CamcorderProfile =
    CamcorderProfile.get(id, preferredQualities.last { CamcorderProfile.hasProfile(id, it) })

inline fun Camera.applyParams(f: Camera.Parameters.() -> Unit): Unit {
  parameters = parameters.apply(f)
}

@WorkerThread
inline fun Context.startRecorder(fc: FacingCamera,
                                 crossinline renderTime: (Long) -> Unit): VideoRecording {
  assertWorkerThread()

  val file = File(telegramDir(), "${System.currentTimeMillis()}.mp4")
  val (camera, facing) = fc

  whenSdk(23) {
    camera.stopPreview()
  }

  camera.applyParams {
    setRecordingHint(true)
    focusMode = Mode.VIDEO.focus(supportedFocusModes)
  }

  camera.unlock()

  val rec = MediaRecorder().apply {
    setCamera(camera)
    setVideoSource(MediaRecorder.VideoSource.CAMERA)
    setAudioSource(MediaRecorder.AudioSource.CAMCORDER)

    setProfile(profile(facing.id()))

    setOutputFile(file.absolutePath)

    val info = Camera.CameraInfo()
    Camera.getCameraInfo(facing.id(), info)
    setOrientationHint(info.orientation)

    prepare()
    start()
  }

  val start = SystemClock.elapsedRealtime()
  val updater = object : Runnable {
    override fun run() {
      renderTime(SystemClock.elapsedRealtime() - start)
      MAIN_THREAD.postDelayed(this, 16)
    }
  }

  return VideoRecording(rec, camera, file, updater)
}

enum class Result {
  SUCCESS,
  FAILURE
}

@WorkerThread
fun MediaRecorder.stopRecorder(): Result {
  assertWorkerThread()
  val ret = tryTimber { stop() }
  reset()
  release()
  return ret?.let { Result.SUCCESS } ?: Result.FAILURE
}

fun VideoRecording.stopRecorder(): Result {
  MAIN_THREAD.removeCallbacks(updater)
  return recorder.stopRecorder().apply {
    camera.reconnect()
    camera.parameters = camera.parameters.apply { setRecordingHint(false) }
    whenSdk(23) {
      camera.startPreview()
    }
  }
}
