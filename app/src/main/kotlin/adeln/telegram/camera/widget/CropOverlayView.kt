package adeln.telegram.camera.widget

import adeln.telegram.camera.Constants
import adeln.telegram.camera.Dimens
import adeln.telegram.camera.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import common.animation.animateInt
import common.context.color
import common.context.dipF
import common.graphics.component1
import common.graphics.component2
import common.graphics.component3
import common.graphics.component4

class CropOverlayView(ctx: Context) : View(ctx) {
  private val borderThick = dipF(2)
  private val cornerThick = dipF(3)
  private val cornerLength = dipF(15)
  private val touchRadius = dipF(24)
  private val initialPadding = dipF(Dimens.CROP_OVERLAY_INITIAL())

  private val dimPaint = Paint().apply {
    color = Color.BLACK
    alpha = 180
  }
  private val guidelinePaint = Paint().apply {
    color = Color.WHITE
    style = Paint.Style.STROKE
    strokeWidth = dipF(1)
    alpha = 0
  }
  private val borderPaint = Paint().apply {
    color = ctx.color(R.color.inactive_circle)
    style = Paint.Style.STROKE
    strokeWidth = borderThick
  }
  private val cornerPaint = Paint().apply {
    color = Color.WHITE
    style = Paint.Style.STROKE
    strokeWidth = cornerThick
  }

  private var pressedCorner: HandleOffset? = null
  private var pressedInside: PointF? = null

  var cropListener: (RectF) -> Unit = {}
  val cropRect = RectF()

  fun reset() {
    cropRect.set(initialPadding, initialPadding, width - initialPadding, height - initialPadding)
    invalidate()
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int): Unit {
    cropRect.set(initialPadding, initialPadding, width - initialPadding, height - initialPadding)
    cropListener(cropRect)
  }

  override fun onDraw(canvas: Canvas): Unit {
    drawDim(canvas)
    drawGuidelines(canvas)
    canvas.drawRect(cropRect, borderPaint)
    drawCorners(canvas)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean =
      when (event.action) {
        MotionEvent.ACTION_DOWN                          -> onActionDown(event.x, event.y)
        MotionEvent.ACTION_MOVE                          -> onActionMove(event.x, event.y)
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onActionUp()
        else                                             -> false
      }

  private fun onActionDown(x: Float, y: Float): Boolean {
    val (left, top, right, bottom) = cropRect

    val leftDiff = x - left
    val rightDiff = x - right
    val topDiff = y - top
    val bottomDiff = y - bottom

    val nearLeft = Math.abs(leftDiff) < touchRadius
    val nearTop = Math.abs(topDiff) < touchRadius
    val nearRight = Math.abs(rightDiff) < touchRadius
    val nearBottom = Math.abs(bottomDiff) < touchRadius

    pressedCorner = when {
      nearTop && nearLeft     -> HandleOffset(Handle.TOP_LEFT, leftDiff, topDiff)
      nearTop && nearRight    -> HandleOffset(Handle.TOP_RIGHT, rightDiff, topDiff)
      nearBottom && nearRight -> HandleOffset(Handle.BOTTOM_RIGHT, rightDiff, bottomDiff)
      nearBottom && nearLeft  -> HandleOffset(Handle.BOTTOM_LEFT, leftDiff, bottomDiff)
      else                    -> null
    }

    if (pressedCorner == null && cropRect.contains(x, y)) {
      pressedInside = PointF(x - cropRect.left, y - cropRect.top)
    }

    if (pressedSomething()) {
      animateInt(0, Constants.MAX_GUIDELINE_ALPHA) {
        guidelinePaint.alpha = it
        invalidate()
      }
          .setDuration(200)
          .start()
    }

    return true
  }

  private fun onActionUp(): Boolean {
    if (pressedSomething()) {
      animateInt(Constants.MAX_GUIDELINE_ALPHA, 0) {
        guidelinePaint.alpha = it
        invalidate()
      }
          .setDuration(200)
          .start()

      pressedCorner = null
      pressedInside = null
      cropListener(cropRect)
    }
    return true
  }

  private fun onActionMove(x: Float, y: Float): Boolean {
    val w = width.toFloat()
    val h = height.toFloat()

    pressedCorner?.let { ph ->
      val endX = (x - ph.offsetX).coerceIn(0F, w)
      val endY = (y - ph.offsetY).coerceIn(0F, h)

      when (ph.handle) {
        Handle.TOP_LEFT     -> {
          cropRect.left = Math.min(endX, cropRect.right - cornerLength)
          cropRect.top = Math.min(endY, cropRect.bottom - cornerLength)
        }
        Handle.BOTTOM_LEFT  -> {
          cropRect.left = Math.min(endX, cropRect.right - cornerLength)
          cropRect.bottom = Math.max(endY, cropRect.top + cornerLength)
        }
        Handle.BOTTOM_RIGHT -> {
          cropRect.right = Math.max(endX, cropRect.left + cornerLength)
          cropRect.bottom = Math.max(endY, cropRect.top + cornerLength)
        }
        Handle.TOP_RIGHT    -> {
          cropRect.right = Math.max(endX, cropRect.left + cornerLength)
          cropRect.top = Math.min(endY, cropRect.bottom - cornerLength)
        }
      }

      invalidate()
    }

    pressedInside?.let { p ->
      cropRect.offsetTo(x - p.x, y - p.y)

      if (cropRect.left < 0) cropRect.offsetTo(0F, cropRect.top)
      if (cropRect.top < 0) cropRect.offsetTo(cropRect.left, 0F)
      if (cropRect.right > w) cropRect.offset(w - cropRect.right, 0F)
      if (cropRect.bottom > h) cropRect.offset(0F, h - cropRect.bottom)

      invalidate()
    }

    return true
  }

  private fun pressedSomething(): Boolean =
      pressedInside != null || pressedCorner != null

  private fun drawDim(canvas: Canvas) {
    val (left, top, right, bottom) = cropRect
    val w = width.toFloat()
    val h = height.toFloat()

    // Draw "top", "bottom", "left", then "right" quadrants according to diagram above.
    canvas.drawRect(0F, 0F, w, top, dimPaint)
    canvas.drawRect(0F, bottom, w, h, dimPaint)
    canvas.drawRect(0F, top, left, bottom, dimPaint)
    canvas.drawRect(right, top, w, bottom, dimPaint)
  }

  private fun drawGuidelines(canvas: Canvas) {
    val (left, top, right, bottom) = cropRect

    // Draw vertical guidelines.
    val oneThirdCropWidth = cropRect.width() / 3

    val x1 = left + oneThirdCropWidth
    canvas.drawLine(x1, top, x1, bottom, guidelinePaint)
    val x2 = right - oneThirdCropWidth
    canvas.drawLine(x2, top, x2, bottom, guidelinePaint)

    // Draw horizontal guidelines.
    val oneThirdCropHeight = cropRect.height() / 3F

    val y1 = top + oneThirdCropHeight
    canvas.drawLine(left, y1, right, y1, guidelinePaint)
    val y2 = bottom - oneThirdCropHeight
    canvas.drawLine(left, y2, right, y2, guidelinePaint)
  }

  private fun drawCorners(canvas: Canvas): Unit {
    val (left, top, right, bottom) = cropRect

    // Absolute value of the offset by which to draw the corner line such that its inner edge is flush with the border's inner edge.
    val lateralOffset = (cornerThick - borderThick) / 2f
    // Absolute value of the offset by which to start the corner line such that the line is drawn all the way to form a corner edge with the adjacent side.
    val startOffset = cornerThick - (borderThick / 2f)

    // Top-left corner: left side
    canvas.drawLine(left - lateralOffset, top - startOffset, left - lateralOffset, top + cornerLength, cornerPaint)
    // Top-left corner: top side
    canvas.drawLine(left - startOffset, top - lateralOffset, left + cornerLength, top - lateralOffset, cornerPaint)

    // Top-right corner: right side
    canvas.drawLine(right + lateralOffset, top - startOffset, right + lateralOffset, top + cornerLength, cornerPaint)
    // Top-right corner: top side
    canvas.drawLine(right + startOffset, top - lateralOffset, right - cornerLength, top - lateralOffset, cornerPaint)

    // Bottom-left corner: left side
    canvas.drawLine(left - lateralOffset, bottom + startOffset, left - lateralOffset, bottom - cornerLength, cornerPaint)
    // Bottom-left corner: bottom side
    canvas.drawLine(left - startOffset, bottom + lateralOffset, left + cornerLength, bottom + lateralOffset, cornerPaint)

    // Bottom-right corner: right side
    canvas.drawLine(right + lateralOffset, bottom + startOffset, right + lateralOffset, bottom - cornerLength, cornerPaint)
    // Bottom-right corner: bottom side
    canvas.drawLine(right + startOffset, bottom + lateralOffset, right - cornerLength, bottom + lateralOffset, cornerPaint)
  }

  private enum class Handle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
  }

  private data class HandleOffset(
      val handle: Handle,
      val offsetX: Float,
      val offsetY: Float
  )
}
