package com.aidong.media.video

import android.graphics.Matrix
import android.graphics.RectF
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import com.aidong.media.video.spherical.SingleTapListener
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.video.VideoSize

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/4/13 2:56 PM
 */
class ComponentListener(private var exoPlayerView: ExoPlayerView) :
    Player.Listener,
    View.OnLayoutChangeListener,
    SingleTapListener,
    PlayerControlView.VisibilityListener {
    private var textureViewRotation = 0
    private var surfaceView: View? = null
    private var exoPlayerListener: ExoPlayerListener? = null

    fun setSurfaceView(surfaceView: View?) {
        this.surfaceView = surfaceView
    }

    fun setListener(exoPlayerListener: ExoPlayerListener?) {
        this.exoPlayerListener = exoPlayerListener
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        exoPlayerView.getPlayer()?.let {
            val vz = it.videoSize
            val width = vz.width
            val height = vz.height
            val unappliedRotationDegrees = vz.unappliedRotationDegrees
            var videoAspectRatio: Float =
                if (height == 0 || width == 0) 0f else width * vz.pixelWidthHeightRatio / height

            if (surfaceView is TextureView) {
                // Try to apply rotation transformation when our surface is a TextureView.
                if (videoAspectRatio > 0
                    && (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270)
                ) {
                    // We will apply a rotation 90/270 degree to the output texture of the TextureView.
                    // In this case, the output video's width and height will be swapped.
                    videoAspectRatio = 1 / videoAspectRatio
                }
                if (textureViewRotation != 0) {
                    surfaceView?.removeOnLayoutChangeListener(this)
                }
                textureViewRotation = unappliedRotationDegrees
                if (textureViewRotation != 0) {
                    // The texture view's dimensions might be changed after layout step.
                    // So add an OnLayoutChangeListener to apply rotation after layout step.
                    surfaceView?.addOnLayoutChangeListener(this)
                }
                applyTextureViewRotation((surfaceView as TextureView?)!!, textureViewRotation)
            }
            exoPlayerView.onContentAspectRatioChanged(videoAspectRatio, surfaceView)
        }
    }

    override fun onLayoutChange(
        view: View?, left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
    ) {
        view?.let {
            applyTextureViewRotation((it as TextureView), textureViewRotation)
        }
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return exoPlayerView.toggleControllerVisibility()
    }

    override fun onVisibilityChange(visibility: Int) {
        exoPlayerView.updateContentDescription()
    }

    private fun applyTextureViewRotation(textureView: TextureView, textureViewRotation: Int) {
        val transformMatrix = Matrix()
        val textureViewWidth = textureView.width.toFloat()
        val textureViewHeight = textureView.height.toFloat()
        if (textureViewWidth != 0f && textureViewHeight != 0f && textureViewRotation != 0) {
            val pivotX = textureViewWidth / 2
            val pivotY = textureViewHeight / 2
            transformMatrix.postRotate(textureViewRotation.toFloat(), pivotX, pivotY)

            // After rotation, scale the rotated texture to fit the TextureView size.
            val originalTextureRect = RectF(0f, 0f, textureViewWidth, textureViewHeight)
            val rotatedTextureRect = RectF()
            transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
            transformMatrix.postScale(
                textureViewWidth / rotatedTextureRect.width(),
                textureViewHeight / rotatedTextureRect.height(),
                pivotX,
                pivotY
            )
        }
        textureView.setTransform(transformMatrix)
    }

    override fun onRenderedFirstFrame() {
        exoPlayerListener?.firstFrame()
    }
}