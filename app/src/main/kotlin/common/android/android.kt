package common.android

import adeln.telegram.camera.BuildConfig
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.os.StrictMode

fun assertWorkerThread(): Unit =
    StrictMode.noteSlowCall("slow")

fun isMainThread(): Boolean =
    Looper.myLooper() == Looper.getMainLooper()

fun assertMainThread(): Unit =
    when {
      BuildConfig.DEBUG && !isMainThread() -> error("wrong thread, buddy")
      else                                 -> Unit
    }

fun threadPolicy(): StrictMode.ThreadPolicy =
    StrictMode.ThreadPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build()

fun vmPolicy(): StrictMode.VmPolicy =
    StrictMode.VmPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build()

inline fun Handler.execute(crossinline f: () -> Unit) {
  when (looper) {
    Looper.myLooper() -> f()
    else              -> post { f() }
  }
}

inline fun <reified T : Parcelable> creator(crossinline f: (Parcel) -> T) =
    object : Parcelable.Creator<T> {
      override fun createFromParcel(source: Parcel): T = f(source)
      override fun newArray(size: Int): Array<T?> = arrayOfNulls(size)
    }

inline fun <T> readOptional(dest: Parcel, f: (Parcel) -> T): T? =
    when (dest.readInt()) {
      1    -> f(dest)
      else -> null
    }

fun <T : Parcelable> T?.writeOptional(dest: Parcel, flags: Int): Unit =
    this?.let {
      dest.writeInt(1)
      it.writeToParcel(dest, flags)
    } ?: dest.writeInt(0)
