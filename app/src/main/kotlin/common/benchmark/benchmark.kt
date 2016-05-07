package common.benchmark

import android.os.Build
import android.os.SystemClock
import timber.log.Timber

fun absoluteNow(): Long =
    System.currentTimeMillis()

fun absoluteNowSeconds(): Long =
    absoluteNow() / 1000

fun relativeNowNano(): Long =
    when {
      Build.VERSION.SDK_INT >= 17 -> SystemClock.elapsedRealtimeNanos()
      else                        -> SystemClock.elapsedRealtime() * 1000000
    }

fun relativeNow(): Long =
    SystemClock.elapsedRealtime()

fun relativeDiff(start: Long): Long =
    relativeNow() - start

fun relativeNanoDiff(start: Long): Double =
    (relativeNowNano() - start) / 1000000.0

inline fun <T> benchmark(what: String, f: () -> T): T =
    relativeNowNano().let { start ->
      f().apply {
        Timber.d("$what took ${relativeNanoDiff(start)} ms")
      }
    }
