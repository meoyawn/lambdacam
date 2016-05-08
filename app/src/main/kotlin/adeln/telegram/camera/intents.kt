package adeln.telegram.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import common.trycatch.tryTimber
import java.io.File

object MimeTypes {
  val JPEG = "image/jpeg"
  val MPEG4 = "video/mp4"
}

fun Context.notifyGallery(f: File): Unit =
    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f)))

fun Context.open(f: File, mime: String): Unit =
    tryTimber {
      val i = Intent(Intent.ACTION_VIEW)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          .setDataAndType(Uri.fromFile(f), mime)
      startActivity(i)
    } ?: Unit
