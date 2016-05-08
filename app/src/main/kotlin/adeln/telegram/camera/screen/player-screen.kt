package adeln.telegram.camera.screen

import adeln.telegram.camera.BACKGROUND_THREAD
import adeln.telegram.camera.CAMERA_THREAD
import adeln.telegram.camera.CamScreen
import adeln.telegram.camera.CameraActivity
import adeln.telegram.camera.Dimens
import adeln.telegram.camera.FileAction
import adeln.telegram.camera.Interpolators
import adeln.telegram.camera.MAIN_THREAD
import adeln.telegram.camera.MimeTypes
import adeln.telegram.camera.PlayerScreen
import adeln.telegram.camera.R
import adeln.telegram.camera.StopRecording
import adeln.telegram.camera.media.preparePlayer
import adeln.telegram.camera.navBarSizeIfPresent
import adeln.telegram.camera.notifyGallery
import adeln.telegram.camera.open
import adeln.telegram.camera.panel
import adeln.telegram.camera.release
import adeln.telegram.camera.replace
import adeln.telegram.camera.resetTo
import adeln.telegram.camera.stopRecorder
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import common.android.execute
import common.animation.animationEnd
import common.benchmark.benchmark
import common.context.dipF
import common.context.drawable
import common.trycatch.tryOptional
import flow.Flow
import org.jetbrains.anko._FrameLayout
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.imageResource
import org.jetbrains.anko.imageView
import org.jetbrains.anko.include
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.onClick
import org.jetbrains.anko.padding
import org.jetbrains.anko.textureView
import org.jetbrains.anko.wrapContent

fun _FrameLayout.addPlayer(h: Int, panelSize: Int) {
  textureView {
    id = R.id.player_texture
    lparams(width = matchParent, height = h)
  }

  val p = panel()
  removeView(p)
  addView(p)

  include<SeekBar>(R.layout.max_height_seekbar) {
    id = R.id.player_progress
    lparams {
      width = matchParent
      height = wrapContent
      topMargin = h - panelSize - dip(8)
    }
    padding = 0
    thumb = context.drawable(R.drawable.player_thumb)
    progressDrawable = context.drawable(R.drawable.player_progress)
  }

  val navBar = context.navBarSizeIfPresent()
  val playSize = dip(52)

  imageView(R.drawable.video_play) {
    id = R.id.play_pause
    lparams {
      width = playSize
      height = playSize
      bottomMargin = (panelSize - navBar - playSize) / 2 + navBar
      gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
    }
    scaleType = ImageView.ScaleType.CENTER_INSIDE
    backgroundResource = R.drawable.clickable_bg
  }

  addCancelDone(panelSize)
}

fun View.playerTexture(): TextureView = find(R.id.player_texture)
fun View.playerProgress(): SeekBar = find(R.id.player_progress)
fun View.playPause(): ImageView = find(R.id.play_pause)

fun stopRecording(vg: _FrameLayout, sr: StopRecording) {
  val rec = sr.rec

  CAMERA_THREAD.execute {
    val p = benchmark("stop and prepare") {
      rec.stopRecorder()
      preparePlayer(rec.file)
    }
    MAIN_THREAD.execute {
      Flow.get(vg.context).replace(PlayerScreen(rec.file, p))
    }
  }

  vg.flashView().animate()
      .alpha(0F)
      .start()
  vg.recordDuration().animate()
      .translationY(vg.dipF(Dimens.RECORD_DURATION_HIDE()))
      .setInterpolator(Interpolators.accelerate)
      .start()
  vg.shootView().animate()
      .translationY(vg.dipF(Dimens.RECORD_BUTTON_HIDE()))
      .setInterpolator(Interpolators.accelerate)
      .animationEnd {
        vg.removeCamButtons()
        vg.removeRecording()
      }
      .start()
}

fun updater(mp: MediaPlayer, pb: ProgressBar): Runnable =
    object : Runnable {
      override fun run() {
        tryOptional {
          pb.progress = mp.currentPosition
          if (mp.isPlaying) {
            MAIN_THREAD.postDelayed(this, 16)
          }
        }
      }
    }

fun toPlayer(vg: _FrameLayout, ps: PlayerScreen) {
  val pH = vg.panel().height

  vg.addPlayer(vg.height, pH)
  val panelSizeF = pH.toFloat()

  vg.panel().backgroundResource = R.color.transparent_panel
  vg.panel().animate()
      .translationY(0F)
      .start()

  vg.playPause().translationY = panelSizeF
  vg.playPause().animate()
      .translationY(0F)
      .start()

  vg.cancel().translationY = panelSizeF
  vg.cancel().animate()
      .translationY(0F)
      .start()

  vg.done().translationY = panelSizeF
  vg.done().animate()
      .translationY(0F)
      .start()

  val player = ps.player
  showFirstFrame(player)
  val upd = updater(player, vg.playerProgress())
  val progress = vg.playerProgress()
  val playPause = vg.playPause()

  playPause.onClick {
    playPause.imageResource = if (player.isPlaying) {
      player.pause()
      MAIN_THREAD.removeCallbacks(upd)
      R.drawable.video_play
    } else {
      player.start()
      upd.run()
      R.drawable.video_pause
    }
  }

  progress.max = player.duration
  progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean): Unit =
        if (fromUser) player.seekTo(progress)
        else Unit

    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
  })

  vg.playerTexture().surfaceTextureListener = surfaceListener(player, upd, playPause)

  vg.cancel().onClick {
    Flow.get(vg.context).goBack()
  }

  vg.done().onClick {
    vg.context.notifyGallery(ps.file)
    vg.context.open(ps.file, MimeTypes.MPEG4)
    Flow.get(vg.context).resetTo(CamScreen(FileAction.RETAIN))
  }
}

fun surfaceListener(player: MediaPlayer, upd: Runnable, playPause: ImageView) =
    object : TextureView.SurfaceTextureListener {
      private var surface: Surface? = null

      override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        surface = Surface(texture)
        player.setSurface(surface)
      }

      override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        MAIN_THREAD.removeCallbacks(upd)

        surface?.release()
        surface = null

        tryOptional {
          if (player.isPlaying) {
            player.pause()
            playPause.imageResource = R.drawable.video_play
          }
        }

        return true
      }

      override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) = Unit
      override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) = Unit
    }

fun showFirstFrame(player: MediaPlayer) {
  player.start()
  player.pause()
  player.seekTo(0)
}

fun _FrameLayout.removePlayer() {
  removeView(playerTexture())
  removeView(playerProgress())
  removeView(playPause())
  removeCancelDone()
}

fun CameraActivity.fromPlayer(vg: _FrameLayout, ps: PlayerScreen, panelSize: Int, to: CamScreen) {
  BACKGROUND_THREAD.execute {
    ps.release()
    if (to.action == FileAction.DELETE) {
      ps.file.delete()
    }
  }

  vg.addCamButtons(panelSize, cam)

  vg.playerTexture().visibility = View.GONE
  vg.playerProgress().animate()
      .alpha(0F)
      .start()
  vg.playPause().animate()
      .alpha(0F)
      .start()
  vg.cancel().animate()
      .scaleX(0F)
      .scaleY(0F)
      .start()
  vg.done().animate()
      .scaleX(0F)
      .scaleY(0F)
      .animationEnd {
        vg.removePlayer()
      }
      .start()
}
