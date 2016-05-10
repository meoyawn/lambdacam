package adeln.telegram.camera

import android.app.Application
import android.os.StrictMode
import com.crashlytics.android.core.CrashlyticsCore
import com.squareup.leakcanary.LeakCanary
import common.android.threadPolicy
import common.android.vmPolicy
import io.fabric.sdk.android.Fabric
import timber.log.Timber

class App : Application() {
  override fun onCreate(): Unit {
    super.onCreate()
    if (BuildConfig.DEBUG) {
      StrictMode.setVmPolicy(vmPolicy())
      StrictMode.setThreadPolicy(threadPolicy())
      Timber.plant(Timber.DebugTree())
      LeakCanary.install(this)
    }
    BACKGROUND_THREAD.execute {
      Fabric.with(this, CrashlyticsCore())
    }
  }
}
