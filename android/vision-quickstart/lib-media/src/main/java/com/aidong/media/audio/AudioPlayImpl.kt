package com.aidong.media.audio

import android.content.Context
import android.net.Uri

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/4/8 11:39 AM
 */
interface AudioPlayImpl<T> {
    fun initAudio(context: Context)
    fun createAudioPlayer()
    fun getPlayer(): T?
    fun play()
    fun play(url: String)
    fun playRaw(rawResourceId: Int)
    fun play(uri: Uri)
    fun play(uri: Uri, isLoop: Boolean, speed: Float)
    fun setVolume(volume: Float)
    fun setSpeed(speed: Float)
    fun pause()
    fun resume()
    fun release()
}