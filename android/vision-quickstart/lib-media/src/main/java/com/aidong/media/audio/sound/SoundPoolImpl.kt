package com.aidong.media.audio.sound

import android.content.Context

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/3/27 1:42 PM
 */
interface SoundPoolImpl {
    fun initSound(context: Context)

    fun loadSoundIds(ids: Array<Int>)

    fun play(id: Int, isLoop: Boolean, volume: Float, rate: Float): Boolean

    fun play(id: Int, playCount: Int, volume: Float, rate: Float): Boolean

    fun pause(id: Int)

    fun resume(id: Int)

    fun pauseAll()

    fun resumeAll()

    fun release()

    fun unload(id:Int)
}