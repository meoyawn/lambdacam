package adeln.telegram.camera

import adeln.telegram.camera.widget.CropOverlayView
import adeln.telegram.camera.widget.FacingView
import adeln.telegram.camera.widget.FlashView
import adeln.telegram.camera.widget.ShootButton
import adeln.telegram.camera.widget.TwoCirclesView
import adeln.telegram.camera.widget.WheelView
import android.view.ViewManager
import com.yalantis.ucrop.view.CropImageView
import org.jetbrains.anko.custom.ankoView

inline fun ViewManager.switchView(init: FacingView.() -> Unit) =
    ankoView({ FacingView(it) }, init)

inline fun ViewManager.circlesView(init: TwoCirclesView.() -> Unit) =
    ankoView({ TwoCirclesView(it) }, init)

inline fun ViewManager.shootButton(init: ShootButton.() -> Unit) =
    ankoView({ ShootButton(it) }, init)

inline fun ViewManager.flashView(init: FlashView.() -> Unit) =
    ankoView({ FlashView(it) }, init)

inline fun ViewManager.cropImageView(init: CropImageView.() -> Unit) =
    ankoView({ CropImageView(it) }, init)

inline fun ViewManager.cropOverlayView(init: CropOverlayView.() -> Unit) =
    ankoView({ CropOverlayView(it) }, init)

inline fun ViewManager.wheelView(init: WheelView.() -> Unit) =
    ankoView({ WheelView(it) }, init)
