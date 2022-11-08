package com.aidong.media.audio.exo

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.upstream.RawResourceDataSource

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/4/7 10:30 AM
 */
enum class MediaAudioPlayer : MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener {
    INSTANCE;

    private val TAG = "MediaAudioPlayer"
    private lateinit var context: Context
    private var mediaPlayer: MediaPlayer? = null

    fun initAudio(context: Context) {
        if (mediaPlayer != null) return
        this.context = context
        createAudioPlayer()
    }

    private fun createAudioPlayer() {
        mediaPlayer = MediaPlayer()

        mediaPlayer?.setOnPreparedListener(this)
        mediaPlayer?.setOnCompletionListener(this)
        mediaPlayer?.setOnErrorListener(this)
    }

    fun play(url: String) {
        play(Uri.parse(url))
    }

    fun play(uri: Uri) {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
        }
        try {
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            mediaPlayer?.reset()
        }
    }

    fun playRaw(rawResourceId: Int) {
        val uri: Uri = RawResourceDataSource.buildRawResourceUri(rawResourceId)
        play(uri)
    }

    fun play() {
        mediaPlayer?.start()
    }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    fun reset() {
        mediaPlayer?.reset()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun release() {
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    override fun onPrepared(p0: MediaPlayer?) {
        Log.d(TAG, "onPrepared")
        p0?.start()
    }

    override fun onCompletion(p0: MediaPlayer?) {
        Log.d(TAG, "onCompletion:")
        p0?.reset()
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        Log.d(TAG, "onError:$p1")
        mediaPlayer?.reset()
        return true
    }
}