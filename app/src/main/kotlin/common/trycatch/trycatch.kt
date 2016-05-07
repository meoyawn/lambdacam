package common.trycatch

import timber.log.Timber

inline fun <T> tryExc(lazy: () -> T, recover: (Exception) -> T): T =
    try {
      lazy()
    } catch(e: Exception) {
      recover(e)
    }

inline fun <T> tryTimber(lazy: () -> T): T? =
    tryExc(lazy) {
      Timber.e(it, "")
      null
    }

inline fun <T> tryOptional(lazy: () -> T): T? =
    tryExc(lazy) { null }
