package com.aidong.media.video

import com.google.android.exoplayer2.Player

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/4/10 5:03 PM
 */
interface PlayerControllerImpl {
    fun setVolume(volume: Float)

    fun setSpeed(speed: Float)

    fun setResizeMode(resizeMode: Int)

    fun getPlayer(): Player?

    fun prepare(isLoop: Boolean)
}