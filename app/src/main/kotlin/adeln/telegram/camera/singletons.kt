package adeln.telegram.camera

import android.os.Handler
import android.os.Looper
import android.os.Process
import com.facebook.rebound.SpringSystem
import java.text.DecimalFormat
import java.util.concurrent.Executors

val MAIN_THREAD = Handler(Looper.getMainLooper())
val CAMERA_THREAD = Executors.newSingleThreadExecutor()
val BACKGROUND_THREAD = Executors.newSingleThreadExecutor { r ->
  object : Thread(r) {
    override fun run() {
      Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE)
      super.run()
    }
  }
}

val SPRING_SYSTEM = SpringSystem.create()
val DEGREE_FORMAT = DecimalFormat("0.0")
