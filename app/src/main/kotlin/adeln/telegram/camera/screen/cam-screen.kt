package adeln.telegram.camera.screen

import adeln.telegram.camera.CAMERA_THREAD
import adeln.telegram.camera.CamScreen
import adeln.telegram.camera.CameraActivity
import adeln.telegram.camera.CropScreen
import adeln.telegram.camera.Dimens
import adeln.telegram.camera.Gesture
import adeln.telegram.camera.Interpolators
import adeln.telegram.camera.LeftRight
import adeln.telegram.camera.MAIN_THREAD
import adeln.telegram.camera.PlayerScreen
import adeln.telegram.camera.R
import adeln.telegram.camera.Screen
import adeln.telegram.camera.StartRecording
import adeln.telegram.camera.StopRecording
import adeln.telegram.camera.TakenScreen
import adeln.telegram.camera.VideoRecording
import adeln.telegram.camera.cameraTexture
import adeln.telegram.camera.circlesView
import adeln.telegram.camera.flashView
import adeln.telegram.camera.gestures
import adeln.telegram.camera.media.Facing
import adeln.telegram.camera.media.Mode
import adeln.telegram.camera.media.SHUTTER
import adeln.telegram.camera.media.State
import adeln.telegram.camera.media.applyParams
import adeln.telegram.camera.media.close
import adeln.telegram.camera.media.focus
import adeln.telegram.camera.media.open
import adeln.telegram.camera.media.supportedFlashes
import adeln.telegram.camera.navBarSizeIfPresent
import adeln.telegram.camera.panel
import adeln.telegram.camera.push
import adeln.telegram.camera.replace
import adeln.telegram.camera.shootButton
import adeln.telegram.camera.switchView
import adeln.telegram.camera.widget.FacingView
import adeln.telegram.camera.widget.FlashView
import adeln.telegram.camera.widget.ShootButton
import adeln.telegram.camera.widget.TwoCirclesView
import adeln.telegram.camera.widget.calc
import android.hardware.Camera
import android.view.Gravity
import android.view.View
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import common.android.execute
import common.animation.animateBackgroundColor
import common.animation.animationEnd
import common.context.color
import common.trycatch.tryTimber
import flow.Flow
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.ctx
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.findOptional
import org.jetbrains.anko.horizontalMargin
import org.jetbrains.anko.onClick

fun _FrameLayout.addCamButtons(panelSize: Int) {
  val touchableSize = dip(52)
  val navBarSize = context.navBarSizeIfPresent()

  flashView {
    id = R.id.flash
    lparams {
      width = touchableSize
      height = dip(32 * (1.5F * 2 + 1))
      gravity = Gravity.END

      horizontalMargin = dip(8)
    }
  }

  circlesView {
    id = R.id.circles
    lparams {
      width = dip(30)
      height = dip(8)
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
      bottomMargin = panelSize + dip(12)
    }
  }

  switchView {
    id = R.id.facing
    lparams {
      width = touchableSize
      height = touchableSize
      gravity = Gravity.BOTTOM
      bottomMargin = (panelSize - navBarSize - touchableSize) / 2 + navBarSize
      horizontalMargin = dip(16)
    }

    backgroundResource = R.drawable.clickable_bg
  }

  shootButton {
    id = R.id.shoot
    val buttonHeight = dip(Dimens.HUGE_BUTTON_SIZE())
    lparams {
      width = dip(Dimens.HUGE_BUTTON_SIZE())
      height = buttonHeight
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
      bottomMargin = (panelSize - navBarSize - buttonHeight) / 2 + navBarSize
    }
  }
}

fun _FrameLayout.flashView(): FlashView? = findOptional(R.id.flash)
fun _FrameLayout.shootView(): ShootButton = find(R.id.shoot)
fun _FrameLayout.facingView(): FacingView = find(R.id.facing)
fun _FrameLayout.twoCircles(): TwoCirclesView = find(R.id.circles)

fun _FrameLayout.removeCamButtons() {
  removeView(flashView())
  removeView(twoCircles())
  removeView(facingView())
  removeView(shootView())
}

fun CameraActivity.toCamScreen(from: Screen, panelSize: Int, f: _FrameLayout, to: CamScreen) {
  when (from) {
    is TakenScreen    -> fromPicTaken(f, panelSize)
    is StartRecording -> deleteRecording(f, from)
    is VideoRecording -> deleteRecording(f, from)
    is StopRecording  -> {
      f.addCamButtons(panelSize)
      f.facingView().translationY = panelSize.toFloat()
      f.facingView().animate()
          .translationY(0F)
          .setInterpolator(Interpolators.decelerate)
          .start()
      f.shootView().translationY = panelSize.toFloat()
      f.shootView().animate()
          .translationY(0F)
          .setInterpolator(Interpolators.decelerate)
          .start()
      f.twoCircles().animate()
          .alpha(1F)
          .setInterpolator(Interpolators.decelerate)
          .start()
      f.panel().animate()
          .translationY(0F)
          .setInterpolator(Interpolators.decelerate)
          .start()
    }
    is PlayerScreen   -> fromPlayer(f, from, panelSize, to)
    is CropScreen     -> {
      f.cameraTexture().visibility = View.VISIBLE
      window.setBackgroundDrawable(null)
      f.removeCrop()
      f.removeView(f.cropView())
      f.addCamButtons(panelSize)
      f.panel().translationY = 0F
      cam?.camera?.startPreview()
    }
    else              -> f.addCamButtons(panelSize)
  }

  val transparent = ctx.color(R.color.transparent_panel)
  val dark = ctx.color(R.color.dark_panel)

  val tv = f.cameraTexture()
  gestures(tv, { it.y <= tv.height - panelSize }) {
    when {
      it == Gesture.SWIPE_RIGHT && mode == Mode.VIDEO  -> {
        f.twoCircles().moveLeft()
        f.panel().animateBackgroundColor(transparent, dark).start()
        f.shootView().toPicture()
        setCameraParams(f, Mode.PICTURE)
      }
      it == Gesture.SWIPE_LEFT && mode == Mode.PICTURE -> {
        f.twoCircles().moveRight()
        f.panel().animateBackgroundColor(dark, transparent).start()
        f.shootView().toVideo()
        setCameraParams(f, Mode.VIDEO)
      }
      it == Gesture.LONG_TAP                           -> {
        if (mode == Mode.PICTURE) {
          f.twoCircles().moveRight()
        }
        setCameraParams(f, Mode.VIDEO)
        Flow.get(ctx).push(StartRecording(null))
      }
    }
  }

  if (mode == Mode.VIDEO) {
    f.twoCircles().leftRight = LeftRight.RIGHT
    f.panel().backgroundColor = transparent
  } else {
    f.twoCircles().leftRight = LeftRight.LEFT
    f.panel().backgroundColor = dark
  }
  f.shootView().mode = mode

  val fv = f.flashView()

  if (Camera.getNumberOfCameras() > 1) {
    f.facingView().facing = facing

    var switching = false
    f.facingView().onClick {
      if (switching) return@onClick
      switching = true

      when (facing) {
        Facing.BACK  -> {
          f.facingView().toFront()
          facing = Facing.FRONT
        }
        Facing.FRONT -> {
          f.facingView().toBack()
          facing = Facing.BACK
        }
      }

      window.setBackgroundDrawableResource(android.R.color.black)
      tv.animate()
          .alpha(0F)
          .setDuration(500)
          .start()
      camState = State.CLOSING

      CAMERA_THREAD.execute {
        cam?.camera?.close()
        camState = State.CLOSED
        val c = open(facing, mode, tv)
        cam = c

        val sf = supportedFlashes(mode, cam?.camera?.parameters?.supportedFlashModes)
        val cur = calc(sf, flash)
        flash = cur?.let { sf[it] }

        MAIN_THREAD.execute {
          fv?.setFlash(sf, cur)
          switching = false
        }
      }
    }
  } else {
    f.facingView().visibility = View.GONE
  }

  var taking = false
  f.shootView().onClick {
    when (mode) {
      Mode.VIDEO   -> {
        val flow = Flow.get(ctx)
        val top = flow.history.top<Screen>()
        when (top) {
          is CamScreen      -> flow.push(StartRecording(null))
          is VideoRecording -> flow.replace(StopRecording(top))
        }
      }
      Mode.PICTURE -> {
        if (taking) return@onClick
        taking = true

        val raw: Camera.PictureCallback? = null
        val jpeg = Camera.PictureCallback { bytes, c ->
          taking = false
          f.shootView().setOnClickListener(null)
          Flow.get(ctx).push(TakenScreen(bytes))
        }

        CAMERA_THREAD.execute {
          val took = tryTimber {
            cam?.camera?.takePicture(SHUTTER, raw, jpeg)
          }
          if (took == null) {
            MAIN_THREAD.execute {
              taking = false
            }
          }
        }

        f.shootView().shoot()
        tv.setOnTouchListener(null)
      }
    }
  }

  val sf = supportedFlashes(mode, cam?.camera?.parameters?.supportedFlashModes)
  val cur = calc(sf, flash)
  flash = cur?.let { sf[it] }

  fv?.run {
    setFlash(sf, cur)
    onClick {
      setNext()?.let { flash = it }
    }
  }
}

fun CameraActivity.setCameraParams(f: _FrameLayout, mode: Mode) {
  this.mode = mode

  val params = tryTimber { cam?.camera?.parameters }
  val sf = supportedFlashes(mode, params?.supportedFlashModes)
  val cur = calc(sf, flash)
  f.flashView()?.setFlash(sf, cur)

  CAMERA_THREAD.execute {
    cam?.camera?.applyParams {
      this.focusMode = mode.focus(supportedFocusModes)
    }
    flash = cur?.let { sf[it] }
  }
}

fun CameraActivity.fromCamScreen(vg: _FrameLayout) {
  vg.flashView()?.animate()
      ?.alpha(0F)
      ?.start()
  vg.twoCircles().animate()
      .alpha(0F)
      .start()
  vg.shootView().animate()
      .alpha(0F)
      .start()
  vg.facingView().animate()
      .alpha(0F)
      .start()

  cancelDoneJump.addListener(object : SimpleSpringListener() {
    override fun onSpringUpdate(spring: Spring) {
      val scale = spring.currentValue.toFloat()

      vg.cancel().scaleX = scale
      vg.cancel().scaleY = scale

      vg.done().scaleX = scale
      vg.done().scaleY = scale
    }
  })
  cancelDoneJump.currentValue = 0.0
  cancelDoneJump.setAtRest()
  cancelDoneJump.endValue = 1.0

  vg.cropButton().animate()
      .alpha(1F)
      .animationEnd {
        vg.removeCamButtons()
      }
      .start()
}
