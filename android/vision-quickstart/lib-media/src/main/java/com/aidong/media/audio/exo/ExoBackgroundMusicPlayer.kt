package com.aidong.media.audio.exo

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/4/7 10:30 AM
 *  播放背景音乐
 */
enum class ExoBackgroundMusicPlayer : Player.Listener {
    INSTANCE;

    private val TAG = "ExoAudioPlayer"
    private lateinit var context: Context
    private var exoPlayer: ExoPlayer? = null

    fun initAudio(context: Context) {
        this.context = context
        createAudioPlayer()
    }

    fun getPlayer(): ExoPlayer? {
        return exoPlayer
    }

    private fun createAudioPlayer() {
        exoPlayer = ExoPlayer.Builder(context).build()
    }

    fun addMediaItems(mediaItems: List<MediaItem>, isAutoPlay: Boolean) {
        exoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
        clearAudio()
        exoPlayer?.addMediaItems(mediaItems)
//        urls.forEach {
//            if (!TextUtils.isEmpty(it)) {
//                val uri = Uri.parse(it)
//                exoPlayer?.addMediaItem(MediaItem.fromUri(uri))
//            }
//        }
        exoPlayer?.playWhenReady = isAutoPlay
        exoPlayer?.prepare()
    }

    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }

    fun getCurrentItemIndex(): Int? {
        return exoPlayer?.currentMediaItemIndex
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.playWhenReady = true
    }

    fun clearAudio() {
        exoPlayer?.clearMediaItems()
    }

    fun release() {
        clearAudio()
        exoPlayer?.release()
    }
}