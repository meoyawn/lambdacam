package adeln.telegram.camera.media

import android.media.AudioManager
import android.media.MediaPlayer
import common.android.assertWorkerThread
import java.io.File

fun preparePlayer(file: File): MediaPlayer =
    MediaPlayer().apply {
      assertWorkerThread()
      setDataSource(file.absolutePath)
      setAudioStreamType(AudioManager.STREAM_MUSIC)
      isLooping = true
      prepare()
    }
