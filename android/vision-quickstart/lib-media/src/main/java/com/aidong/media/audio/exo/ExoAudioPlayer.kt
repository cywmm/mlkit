package com.aidong.media.audio.exo

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aidong.media.audio.AudioPlayImpl
import com.aidong.media.utils.Utils
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.EventLogger

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/4/7 10:30 AM
 */
enum class ExoAudioPlayer : AudioPlayImpl<ExoPlayer>, Player.Listener {
    INSTANCE;

    private val TAG = "ExoAudioPlayer"
    private lateinit var context: Context
    private var exoPlayer: ExoPlayer? = null

    override fun initAudio(context: Context) {
        if (exoPlayer != null) return
        this.context = context
        createAudioPlayer()
    }

    override fun getPlayer(): ExoPlayer? {
        return exoPlayer
    }

    override fun createAudioPlayer() {
//        val builder = OkHttpClient.Builder()
//        val client = builder.build()
//        val okHttpDataSourceFactory = OkHttpDataSource.Factory(client)
//        exoPlayer = ExoPlayer
//            .Builder(context)
//            .setMediaSourceFactory(DefaultMediaSourceFactory(okHttpDataSourceFactory))
//            .build()

        val renderersFactory: RenderersFactory =
            Utils.buildRenderersFactory( /* context= */context, true)
        val trackSelector = DefaultTrackSelector( /* context= */context)
        exoPlayer = ExoPlayer.Builder(context)
//            .setRenderersFactory(renderersFactory)
//            .setTrackSelector(trackSelector)
            .build()
        exoPlayer?.addAnalyticsListener(EventLogger(trackSelector))
        exoPlayer?.addListener(this)
    }

    override fun playRaw(rawResourceId: Int) {
        clearAudio()
        val uri: Uri = RawResourceDataSource.buildRawResourceUri(rawResourceId)
        play(uri, false, 0f)
    }

    override fun play(url: String) {
        play(Uri.parse(url), false, 0f)
    }

    override fun play(uri: Uri) {
        play(uri, false, 0f)
    }

    override fun play(uri: Uri, isLoop: Boolean, speed: Float) {
        clearAudio()
        exoPlayer?.let {
            if (it.isPlaying) it.stop()
        }
        exoPlayer?.setMediaItem(MediaItem.fromUri(uri))

        if (isLoop) exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
        if (speed > 1.0f) exoPlayer?.playbackParameters = PlaybackParameters(speed)

        exoPlayer?.playWhenReady = true
        exoPlayer?.prepare()
        exoPlayer?.play()
    }


    override fun onPlayerError(error: PlaybackException) {
        Log.d(TAG, "onPlayerError: ${error.message}")
    }

    override fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun resume() {
        exoPlayer?.playWhenReady = true
    }

    fun clearAudio() {
        exoPlayer?.clearMediaItems()
    }

    override fun setSpeed(speed: Float) {
        val parameters = PlaybackParameters(speed)
        exoPlayer?.playbackParameters = parameters
    }

    override fun release() {
        exoPlayer?.removeListener(this)
        exoPlayer?.release()
        exoPlayer = null
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }
}