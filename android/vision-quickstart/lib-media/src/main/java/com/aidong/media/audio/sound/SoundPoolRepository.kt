package com.aidong.media.audio.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 *  author: wangming
 *  email:  cy_wangming@163.com
 *  date:   2020/3/26 5:23 PM
 */
class SoundPoolRepository : SoundPoolImpl {
    private var soundPool: SoundPool? = null
    private var context: Context? = null
    private var soundIds: MutableMap<Int, Int?> = HashMap()

    private object Holder {
        val instance = SoundPoolRepository()
    }

    companion object {
        fun getInstance() = Holder.instance
    }

    override fun initSound(context: Context) {
        this.context = context
        val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        soundPool = SoundPool.Builder()
                .setMaxStreams(16)
                .setAudioAttributes(audioAttributes)
                .build()
    }

    override fun loadSoundIds(ids: Array<Int>) {
        context?.let { context ->
            for (id in ids) {
                val load = soundPool?.load(context, id, 1)
                soundIds[id] = load
            }
        }
    }

    override fun play(id: Int, isLoop: Boolean, volume: Float, rate: Float): Boolean {
        val loop = if (isLoop) -1 else 0
        return play(id, loop, volume, rate)
    }

    override fun play(id: Int, playCount: Int, volume: Float, rate: Float): Boolean {
        val soundID = soundIds[id]
        return if (soundID != null) {
            soundPool?.play(soundID, volume, volume, 1, playCount, rate) != 0
        } else {
            false
        }
    }

    override fun pause(id: Int) {
        val soundID = soundIds[id]
        soundID?.let {
            soundPool?.pause(it)
        }
    }

    override fun resume(id: Int) {
        val soundID = soundIds[id]
        soundID?.let {
            soundPool?.resume(it)
        }
    }

    override fun pauseAll() {
        soundPool?.autoPause()
    }

    override fun resumeAll() {
        soundPool?.autoResume()
    }

    override fun release() {
        soundPool?.release()
    }

    override fun unload(id: Int) {
        val soundID = soundIds[id]
        soundID?.let {
            soundPool?.unload(it)
        }
    }
}