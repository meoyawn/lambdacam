package adeln.telegram.camera

import adeln.telegram.camera.media.Facing
import adeln.telegram.camera.media.FacingCamera
import adeln.telegram.camera.media.Flash
import adeln.telegram.camera.media.Mode
import adeln.telegram.camera.media.State
import adeln.telegram.camera.media.toString
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.facebook.rebound.SpringConfig
import flow.FlowDelegate
import flow.History

class CameraActivity : Activity() {
  val cancelDoneJump = SPRING_SYSTEM.createSpring().apply { springConfig = SpringConfig(500.0, 12.0) }

  var facing = Facing.BACK
  var flash: Flash? = null
    set(value) {
      field = value
      value?.let {
        cam?.camera?.parameters = cam?.camera?.parameters?.apply {
          flashMode = toString(it)
        }
      }
    }

  @Volatile var mode = Mode.PICTURE
  @Volatile var camState = State.CLOSED

  var cam: FacingCamera? = null

  var flowDelegate: FlowDelegate? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val size = Displays.getRealSize(windowManager.defaultDisplay)
    val panelSize = size.y - size.x * Constants.PIC_RATIO

    val frame = layout(panelSize.toInt())
    flowDelegate = mkFlow(ParcelableParceler, History.single(CamScreen(FileAction.DELETE)), savedInstanceState) { t, c ->
      dispatch(frame, t, c, size)
    }

    setContentView(frame)

    if (Build.VERSION.SDK_INT >= 19) {
      window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
  }

  override fun onBackPressed(): Unit =
      if (flowDelegate?.onBackPressed() ?: false) Unit else super.onBackPressed()

  override fun getSystemService(name: String?): Any? =
      flowDelegate?.getSystemService(name) ?: super.getSystemService(name)
}
