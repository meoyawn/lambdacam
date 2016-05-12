package adeln.telegram.camera.screen

import adeln.telegram.camera.CAMERA_THREAD
import adeln.telegram.camera.CameraActivity
import adeln.telegram.camera.Dimens
import adeln.telegram.camera.Interpolators
import adeln.telegram.camera.MAIN_THREAD
import adeln.telegram.camera.R
import adeln.telegram.camera.Screen
import adeln.telegram.camera.StartRecording
import adeln.telegram.camera.VideoRecording
import adeln.telegram.camera.cameraTexture
import adeln.telegram.camera.media.startRecorder
import adeln.telegram.camera.media.stopRecorder
import adeln.telegram.camera.panel
import adeln.telegram.camera.replace
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.TextView
import common.android.assertMainThread
import common.android.execute
import common.animation.animationEnd
import common.benchmark.benchmark
import common.context.dipF
import flow.Flow
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.horizontalPadding
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalPadding

fun leadingZero(num: Long): String =
    when (num.toInt()) {
      in 0..9 -> "0$num"
      else    -> num.toString()
    }

fun minsSecs(time: Long): String =
    "${leadingZero(time / 60000)}:${leadingZero((time / 1000 % 60))}"

fun CameraActivity.startRecording(vg: _FrameLayout, to: StartRecording): Unit {
  val dur = vg.run {
    textView(minsSecs(0L)) {
      id = R.id.record_duration
      lparams {
        topMargin = dip(16)
        gravity = Gravity.CENTER_HORIZONTAL
      }

      textColor = Color.WHITE
      textSize = 16F
      horizontalPadding = dip(8)
      verticalPadding = dip(4)

      backgroundResource = R.drawable.duration_bg
    }
  }

  CAMERA_THREAD.execute {
    val rec = benchmark("start record") {
      cam?.let {
        startRecorder(it) {
          assertMainThread()
          dur.text = minsSecs(it)
        }
      }
    }

    to.recording = rec

    MAIN_THREAD.execute {
      val flow = Flow.get(vg)
      val top = flow.history.top<Screen>()
      if (top is StartRecording) {
        flow.replace(rec)
      }
    }
  }

  vg.cameraTexture().setOnTouchListener(null)

  dur.translationY = dipF(Dimens.RECORD_DURATION_HIDE())
  dur.animate()
      .translationY(0F)
      .setInterpolator(Interpolators.decelerate)
      .start()
  vg.shootView().startRecording()
  vg.facingView().animate()
      .translationY(vg.panel().height.toFloat())
      .setInterpolator(Interpolators.decelerate)
      .start()
  vg.panel().animate()
      .translationY(vg.panel().height.toFloat())
      .setInterpolator(Interpolators.decelerate)
      .start()
  vg.twoCircles().animate()
      .alpha(0F)
      .setInterpolator(Interpolators.decelerate)
      .start()
}

fun toRecording(vg: _FrameLayout, to: VideoRecording) {
  to.updater.run()
}

fun View.recordDuration(): TextView = find(R.id.record_duration)

fun _FrameLayout.removeRecording() {
  removeView(recordDuration())
}

fun deleteRecording(f: _FrameLayout, from: VideoRecording) {
  CAMERA_THREAD.execute {
    stopDelete(from)
  }

  recToCam(f)
}

fun deleteRecording(f: _FrameLayout, from: StartRecording) {
  CAMERA_THREAD.execute {
    from.recording?.let { stopDelete(it) }
  }

  recToCam(f)
}

fun recToCam(f: _FrameLayout) {
  f.recordDuration().animate()
      .translationY(f.dipF(Dimens.RECORD_DURATION_HIDE()))
      .setInterpolator(Interpolators.decelerate)
      .animationEnd {
        f.removeRecording()
      }
      .start()

  f.shootView().stopRecording()
  f.panel().animate()
      .translationY(0F)
      .setInterpolator(Interpolators.decelerate)
      .start()
  f.facingView().animate()
      .translationY(0F)
      .setInterpolator(Interpolators.decelerate)
      .start()
  f.twoCircles().animate()
      .alpha(1F)
      .setInterpolator(Interpolators.decelerate)
      .start()
}

fun stopDelete(rec: VideoRecording) {
  benchmark("stop recorder") {
    rec.stopRecorder()
    rec.file.delete()
  }
}
