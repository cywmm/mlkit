package com.google.mlkit.vision.demo

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.TextureView
import java.util.*
import kotlin.collections.ArrayList


/**
 * date: 2022/8/15 9:35
 * author: wangming
 * dec:
 */
class SkeletonTextureView : TextureView, TextureView.SurfaceTextureListener {
    private var mCanvas: Canvas? = null
    private var zMin = java.lang.Float.MAX_VALUE
    private var zMax = java.lang.Float.MIN_VALUE
    private val classificationTextPaint: Paint = Paint()
    private val leftPaint: Paint
    private val rightPaint: Paint
    private val whitePaint: Paint
    private lateinit var overlay: GraphicOverlay

    init {
        classificationTextPaint.color = Color.WHITE
        classificationTextPaint.textSize = POSE_CLASSIFICATION_TEXT_SIZE
        classificationTextPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK)

        whitePaint = Paint()
        whitePaint.strokeWidth = STROKE_WIDTH
        whitePaint.color = Color.WHITE
        whitePaint.textSize = IN_FRAME_LIKELIHOOD_TEXT_SIZE
        leftPaint = Paint()
        leftPaint.strokeWidth = STROKE_WIDTH
        leftPaint.color = Color.GREEN
        rightPaint = Paint()
        rightPaint.strokeWidth = STROKE_WIDTH
        rightPaint.color = Color.YELLOW
        surfaceTextureListener = this
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
        isOpaque = false
    }

    fun drawSkeleton(pose: ArrayList<PointF>?, overlay: GraphicOverlay) {
        this.overlay = overlay
        try {
            mCanvas = lockCanvas()
            mCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)// 绘制背景色
            if (pose.isNullOrEmpty()) {
                return
            }

            val nose = pose[0]
            val neck = pose[1]
            val leftShoulder = pose[5]
            val rightShoulder = pose[2]
            val leftElbow = pose[6]
            val rightElbow = pose[3]
            val leftWrist = pose[7]
            val rightWrist = pose[4]
            val leftHip = pose[11]
            val rightHip = pose[8]
            val leftKnee = pose[12]
            val rightKnee = pose[9]
            val leftAnkle = pose[13]
            val rightAnkle = pose[10]

            mCanvas?.apply {
                for (landmark in pose) {
                    drawPoint(this, landmark, whitePaint)
                }

                drawLine(this, nose, neck, whitePaint)
                drawLine(this, leftShoulder, neck, whitePaint)
                drawLine(this, neck, rightShoulder, whitePaint)
                drawLine(this, leftHip, rightHip, whitePaint)

                // Left body
                drawLine(this, leftShoulder, leftElbow, leftPaint)
                drawLine(this, leftElbow, leftWrist, leftPaint)
                drawLine(this, leftShoulder, leftHip, leftPaint)
                drawLine(this, leftHip, leftKnee, leftPaint)
                drawLine(this, leftKnee, leftAnkle, leftPaint)

                // Right body
                drawLine(this, rightShoulder, rightElbow, rightPaint)
                drawLine(this, rightElbow, rightWrist, rightPaint)
                drawLine(this, rightShoulder, rightHip, rightPaint)
                drawLine(this, rightHip, rightKnee, rightPaint)
                drawLine(this, rightKnee, rightAnkle, rightPaint)

                // Draw inFrameLikelihood for all points
                for (landmark in pose) {
                    this.drawText(
                        String.format(Locale.US, "%.2f", 0),
                        translateX(landmark.x, overlay),
                        translateY(landmark.y, overlay),
                        whitePaint
                    )
                }
//                unlockCanvasAndPost(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mCanvas?.let {
                unlockCanvasAndPost(it)
            }
        }
    }

    private fun drawPoint(canvas: Canvas, posePoint: PointF, paint: Paint) {
        canvas.drawCircle(
            translateX(posePoint.x, overlay),
            translateY(posePoint.y, overlay),
            DOT_RADIUS,
            paint
        )
    }

    private fun drawLine(
        canvas: Canvas,
        start: PointF?,
        end: PointF?,
        paint: Paint
    ) {
        if (start?.x == 0f && start.y == 0f) return
        if (end?.x == 0f && end.y == 0f) return

        // Gets average z for the current body line
        canvas.drawLine(
            translateX(start!!.x, overlay),
            translateY(start.y, overlay),
            translateX(end!!.x, overlay),
            translateY(end.y, overlay),
            paint
        )
    }

    /**
     * Adjusts the supplied value from the image scale to the view scale.
     */
    private fun scale(imagePixel: Float, overlay: GraphicOverlay): Float {
        return imagePixel * overlay.scaleFactor
    }

    /**
     * Adjusts the x coordinate from the image's coordinate system to the view coordinate system.
     */
    private fun translateX(x: Float, overlay: GraphicOverlay): Float {
        return if (false) {
            overlay.width - (scale(x, overlay) - overlay.postScaleWidthOffset)
        } else {
            scale(x, overlay) - overlay.postScaleWidthOffset
        }
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    private fun translateY(y: Float, overlay: GraphicOverlay): Float {
        return scale(y, overlay) - overlay.postScaleHeightOffset
    }


    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

    }

    companion object {
        private val DOT_RADIUS = 10.0f
        private val IN_FRAME_LIKELIHOOD_TEXT_SIZE = 30.0f
        private val STROKE_WIDTH = 14.0f
        private val POSE_CLASSIFICATION_TEXT_SIZE = 60.0f
    }
}