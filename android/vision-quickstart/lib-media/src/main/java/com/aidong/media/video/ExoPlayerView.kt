package com.aidong.media.video

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import com.aidong.media.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.video.VideoDecoderGLSurfaceView
import com.google.android.exoplayer2.video.spherical.SphericalGLSurfaceView

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/4/10 3:03 PM
 */
class ExoPlayerView : FrameLayout, PlayerControllerImpl {
    private val TAG = "ExoPlayerView"

    private val layoutId = R.layout.exo_view
    private var contentFrame: AspectRatioFrameLayout? = null
    private var surfaceView: View? = null
    private var player: ExoPlayer? = null
    private var controllerView: PlayerControlView? = null
    private var componentListener: ComponentListener? = ComponentListener(this)

    private var resizeMode = ResizeMode.FIT
    private var surfaceType = SurfaceType.SURFACE_TYPE_SURFACE_VIEW
    private var controllerShowTimeoutMs = PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS
    private var autoPlay = false
    private var useController = false
    private var controllerHideOnTouch = true
    private var isTouching = false

    private var volume = 1.0f
    private var speed = 1.0f

    constructor(mContext: Context) : this(mContext, null)

    constructor(mContext: Context, attrs: AttributeSet?) : this(mContext, attrs, 0)

    constructor (mContext: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        mContext,
        attrs,
        defStyleAttr
    ) {
        initAttr(attrs)
        initView()
        initSurfaceView()
        initControllerView(attrs)
        initPlayer()
    }

    private fun initAttr(mAttributeSet: AttributeSet?) {
        mAttributeSet?.let {
            val a =
                context.theme.obtainStyledAttributes(mAttributeSet, R.styleable.ExoPlayerView, 0, 0)
            try {
                resizeMode = a.getInt(R.styleable.ExoPlayerView_resize_mode, resizeMode)
                autoPlay = a.getBoolean(R.styleable.ExoPlayerView_auto_play, autoPlay)
                useController =
                    a.getBoolean(R.styleable.ExoPlayerView_use_controller, useController)
                surfaceType = a.getInt(R.styleable.ExoPlayerView_surface_type, surfaceType)
            } finally {
                a.recycle()
            }
        }
    }

    fun setListener(listener: ExoPlayerListener) {
        componentListener?.setListener(listener)
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(layoutId, this)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        contentFrame = findViewById(R.id.exo_content)
    }

    private fun initSurfaceView() {
        if (contentFrame != null && surfaceType != SurfaceType.SURFACE_TYPE_NONE) {
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            surfaceView = when (surfaceType) {
                SurfaceType.SURFACE_TYPE_TEXTURE_VIEW -> {
                    TextureView(context)
                }
                SurfaceType.SURFACE_TYPE_VIDEO_DECODER_GL_SURFACE_VIEW -> {
                    VideoDecoderGLSurfaceView(context)
                }
                else -> {
                    SurfaceView(context)
                }
            }

            surfaceView?.layoutParams = params
            contentFrame?.addView(surfaceView, 0)
        } else {
            surfaceView = null
        }

        componentListener?.setSurfaceView(surfaceView)
    }

    private fun initControllerView(attrs: AttributeSet?) {
        if (!useController) return
        val controllerPlaceholder = findViewById<View>(R.id.exo_placeholder)
        if (controllerPlaceholder != null) {
            this.controllerView = PlayerControlView(context, null, 0, attrs)
            controllerView?.id = R.id.exo_controller
            controllerView?.layoutParams = controllerPlaceholder.layoutParams
            val parent = controllerPlaceholder.parent as ViewGroup
            val controllerIndex = parent.indexOfChild(controllerPlaceholder)
            parent.removeView(controllerPlaceholder)
            parent.addView(controllerView, controllerIndex)
        } else {
            controllerView = null
        }

        this.controllerShowTimeoutMs = if (controllerView != null) controllerShowTimeoutMs else 0
        this.useController = useController && controllerView != null
        updateContentDescription()

        controllerView?.addVisibilityListener(componentListener)
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(context).build()

        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper())
        Assertions.checkArgument(
            player == null || player?.applicationLooper == Looper.getMainLooper()
        )
        player?.let { player ->
            player.playWhenReady = autoPlay
            when (surfaceView) {
                is TextureView -> {
                    player.setVideoTextureView(surfaceView as TextureView)
                }
                is SurfaceView -> {
                    player.setVideoSurfaceView(surfaceView as SurfaceView)
                }
            }
            componentListener?.let { player.addListener(it) }
            controllerView?.player = player
        }
    }

    fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    fun pause() {
        player?.pause()
    }

    fun play() {
        player?.playWhenReady = true
    }

    fun release() {
        player?.clearMediaItems()
        componentListener?.let { player?.removeListener(it) }
        player?.release()
        player = null
        componentListener = null
        controllerView?.hide()
        controllerView = null
    }

    fun useController(useController: Boolean) {
        this.useController = useController
        if (!useController) controllerView?.hide()
    }

    override fun setVolume(volume: Float) {
        player?.volume = volume
        this.volume = volume
    }

    override fun setSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        this.speed = speed
    }

    override fun setResizeMode(resizeMode: Int) {
        contentFrame?.setResizeMode(resizeMode)
    }

    override fun getPlayer(): ExoPlayer? {
        return player
    }

    fun addMediaUrl(url: String) {
        player?.addMediaItem(MediaItem.fromUri(url))
    }

    fun addMediaItem(mediaItem: MediaItem) {
        player?.addMediaItem(mediaItem)
    }

    fun addMediaItems(mediaItems: List<MediaItem>) {
        player?.addMediaItems(mediaItems)
    }

    fun addMediaSource(mediaSource: MediaSource) {
        player?.setMediaSource(mediaSource)
    }

    override fun prepare(isLoop: Boolean) {
        if (speed > 0f) {
            val parameters = PlaybackParameters(speed)
            player?.setPlaybackSpeed(speed)
        }
        if (isLoop) player?.repeatMode = Player.REPEAT_MODE_ALL
        player?.volume = volume
        player?.prepare()
    }


    fun onContentAspectRatioChanged(videoAspectRatio: Float, contentView: View?) {
        contentFrame?.setAspectRatio(if (contentView is SphericalGLSurfaceView) 0f else videoAspectRatio)
    }

    // Internal methods.
    private fun useController(): Boolean {
        if (useController) {
            Assertions.checkStateNotNull(controllerView)
            return true
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (!useController() || player == null) {
            false
        } else when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                true
            }
            MotionEvent.ACTION_UP -> {
                if (isTouching) {
                    isTouching = false
                    performClick()
                    return true
                }
                false
            }
            else -> false
        }
    }

    fun updateContentDescription() {
        contentDescription = if (controllerView == null || !useController) {
            null
        } else if (controllerView?.visibility == View.VISIBLE) {
            if (controllerHideOnTouch) "Hide player controls" else null
        } else {
            "Show player controls"
        }
    }

    fun onResume() {
        if (surfaceView is SphericalGLSurfaceView) {
            (surfaceView as SphericalGLSurfaceView).onResume()
        }
    }

    fun onPause() {
        if (surfaceView is SphericalGLSurfaceView) {
            (surfaceView as SphericalGLSurfaceView).onPause()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return toggleControllerVisibility()
    }

    fun toggleControllerVisibility(): Boolean {
        if (!useController || player == null) {
            return false
        }

        controllerView?.let { controller ->
            if (!controller.isVisible) {
                controller.show()
            } else if (controllerHideOnTouch) {
                controller.hide()
            }
        }
        return true
    }
}