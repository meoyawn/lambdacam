package adeln.telegram.camera.media

import android.content.Context
import android.os.Environment
import java.io.File

fun Context.telegramDir(): File =
    File(cameraDir(), "Telegram").apply { mkdirs() }

fun Context.cameraDir(): File =
    if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
    else filesDir
