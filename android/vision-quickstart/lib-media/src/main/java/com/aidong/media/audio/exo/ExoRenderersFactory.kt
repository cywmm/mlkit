/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aidong.media.audio.exo

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.IntDef
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.audio.*
import com.google.android.exoplayer2.audio.DefaultAudioSink.DefaultAudioProcessorChain
import com.google.android.exoplayer2.mediacodec.DefaultMediaCodecAdapterFactory
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.metadata.MetadataRenderer
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.text.TextRenderer
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer
import com.google.android.exoplayer2.video.VideoRendererEventListener
import com.google.android.exoplayer2.video.spherical.CameraMotionRenderer
import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.*

/** Default [RenderersFactory] implementation.  */
class ExoRenderersFactory : RenderersFactory {
    /**
     * Modes for using extension renderers. One of [.EXTENSION_RENDERER_MODE_OFF], [ ][.EXTENSION_RENDERER_MODE_ON] or [.EXTENSION_RENDERER_MODE_PREFER].
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(EXTENSION_RENDERER_MODE_OFF, EXTENSION_RENDERER_MODE_ON, EXTENSION_RENDERER_MODE_PREFER)
    annotation class ExtensionRendererMode

    private val context: Context
    private val codecAdapterFactory: DefaultMediaCodecAdapterFactory

    @ExtensionRendererMode
    private var extensionRendererMode: Int
    private var allowedVideoJoiningTimeMs: Long
    private var enableDecoderFallback = false
    private var mediaCodecSelector: MediaCodecSelector
    private var enableFloatOutput = false
    private var enableAudioTrackPlaybackParams = false
    private var enableOffload = false

    /** @param context A [Context].
     */
    constructor(context: Context) {
        this.context = context
        codecAdapterFactory = DefaultMediaCodecAdapterFactory()
        extensionRendererMode = EXTENSION_RENDERER_MODE_OFF
        allowedVideoJoiningTimeMs = DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS
        mediaCodecSelector = MediaCodecSelector.DEFAULT
    }

    @Deprecated(
        """Use {@link #ExoRenderersFactory(Context)} and {@link
   *     #setExtensionRendererMode(int)}."""
    )
    constructor(
        context: Context, @ExtensionRendererMode extensionRendererMode: Int
    ) : this(context, extensionRendererMode, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS) {
    }

    @Deprecated(
        """Use {@link #ExoRenderersFactory(Context)}, {@link
   *     #setExtensionRendererMode(int)} and {@link #setAllowedVideoJoiningTimeMs(long)}."""
    )
    constructor(
        context: Context,
        @ExtensionRendererMode extensionRendererMode: Int,
        allowedVideoJoiningTimeMs: Long
    ) {
        this.context = context
        this.extensionRendererMode = extensionRendererMode
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs
        mediaCodecSelector = MediaCodecSelector.DEFAULT
        codecAdapterFactory = DefaultMediaCodecAdapterFactory()
    }

    /**
     * Sets the extension renderer mode, which determines if and how available extension renderers are
     * used. Note that extensions must be included in the application build for them to be considered
     * available.
     *
     *
     * The default value is [.EXTENSION_RENDERER_MODE_OFF].
     *
     * @param extensionRendererMode The extension renderer mode.
     * @return This factory, for convenience.
     */
    fun setExtensionRendererMode(
        @ExtensionRendererMode extensionRendererMode: Int
    ): ExoRenderersFactory {
        this.extensionRendererMode = extensionRendererMode
        return this
    }

    /**
     * Enables [com.google.android.exoplayer2.mediacodec.MediaCodecRenderer] instances to
     * operate their [MediaCodec] in asynchronous mode and perform asynchronous queueing.
     *
     *
     * This feature can be enabled only on devices with API versions &gt;= 23. For devices with
     * older API versions, this method is a no-op.
     *
     * @return This factory, for convenience.
     */
    fun forceEnableMediaCodecAsynchronousQueueing(): ExoRenderersFactory {
        codecAdapterFactory.forceEnableAsynchronous()
        return this
    }

    /**
     * Disables [com.google.android.exoplayer2.mediacodec.MediaCodecRenderer] instances from
     * operating their [MediaCodec] in asynchronous mode and perform asynchronous queueing.
     * [MediaCodec] instances will be operated synchronous mode.
     *
     * @return This factory, for convenience.
     */
    fun forceDisableMediaCodecAsynchronousQueueing(): ExoRenderersFactory {
        codecAdapterFactory.forceDisableAsynchronous()
        return this
    }

    /**
     * Enable synchronizing codec interactions with asynchronous buffer queueing.
     *
     *
     * This method is experimental, and will be renamed or removed in a future release.
     *
     * @param enabled Whether codec interactions will be synchronized with asynchronous buffer
     * queueing.
     * @return This factory, for convenience.
     */
    fun experimentalSetSynchronizeCodecInteractionsWithQueueingEnabled(
        enabled: Boolean
    ): ExoRenderersFactory {
        codecAdapterFactory.experimentalSetSynchronizeCodecInteractionsWithQueueingEnabled(enabled)
        return this
    }

    /**
     * Sets a [MediaCodecSelector] for use by [MediaCodec] based renderers.
     *
     *
     * The default value is [MediaCodecSelector.DEFAULT].
     *
     * @param mediaCodecSelector The [MediaCodecSelector].
     * @return This factory, for convenience.
     */
    fun setMediaCodecSelector(mediaCodecSelector: MediaCodecSelector): ExoRenderersFactory {
        this.mediaCodecSelector = mediaCodecSelector
        return this
    }

    /**
     * Sets whether floating point audio should be output when possible.
     *
     *
     * Enabling floating point output disables audio processing, but may allow for higher quality
     * audio output.
     *
     *
     * The default value is `false`.
     *
     * @param enableFloatOutput Whether to enable use of floating point audio output, if available.
     * @return This factory, for convenience.
     */
    fun setEnableAudioFloatOutput(enableFloatOutput: Boolean): ExoRenderersFactory {
        this.enableFloatOutput = enableFloatOutput
        return this
    }

    /**
     * Sets whether audio should be played using the offload path.
     *
     *
     * Audio offload disables ExoPlayer audio processing, but significantly reduces the energy
     * consumption of the playback when [ ][ExoPlayer.experimentalSetOffloadSchedulingEnabled] is enabled.
     *
     *
     * Most Android devices can only support one offload [android.media.AudioTrack] at a time
     * and can invalidate it at any time. Thus an app can never be guaranteed that it will be able to
     * play in offload.
     *
     *
     * The default value is `false`.
     *
     * @param enableOffload Whether to enable use of audio offload for supported formats, if
     * available.
     * @return This factory, for convenience.
     */
    fun setEnableAudioOffload(enableOffload: Boolean): ExoRenderersFactory {
        this.enableOffload = enableOffload
        return this
    }

    /**
     * Sets whether to enable setting playback speed using [ ][android.media.AudioTrack.setPlaybackParams], which is supported from API level
     * 23, rather than using application-level audio speed adjustment. This setting has no effect on
     * builds before API level 23 (application-level speed adjustment will be used in all cases).
     *
     *
     * If enabled and supported, new playback speed settings will take effect more quickly because
     * they are applied at the audio mixer, rather than at the point of writing data to the track.
     *
     *
     * When using this mode, the maximum supported playback speed is limited by the size of the
     * audio track's buffer. If the requested speed is not supported the player's event listener will
     * be notified twice on setting playback speed, once with the requested speed, then again with the
     * old playback speed reflecting the fact that the requested speed was not supported.
     *
     * @param enableAudioTrackPlaybackParams Whether to enable setting playback speed using [     ][android.media.AudioTrack.setPlaybackParams].
     * @return This factory, for convenience.
     */
    fun setEnableAudioTrackPlaybackParams(
        enableAudioTrackPlaybackParams: Boolean
    ): ExoRenderersFactory {
        this.enableAudioTrackPlaybackParams = enableAudioTrackPlaybackParams
        return this
    }

    /**
     * Sets the maximum duration for which video renderers can attempt to seamlessly join an ongoing
     * playback.
     *
     *
     * The default value is [.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS].
     *
     * @param allowedVideoJoiningTimeMs The maximum duration for which video renderers can attempt to
     * seamlessly join an ongoing playback, in milliseconds.
     * @return This factory, for convenience.
     */
    fun setAllowedVideoJoiningTimeMs(allowedVideoJoiningTimeMs: Long): ExoRenderersFactory {
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs
        return this
    }

    override fun createRenderers(
        eventHandler: Handler,
        videoRendererEventListener: VideoRendererEventListener,
        audioRendererEventListener: AudioRendererEventListener,
        textRendererOutput: TextOutput,
        metadataRendererOutput: MetadataOutput
    ): Array<Renderer> {
        val renderersList = ArrayList<Renderer>()
        buildVideoRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            enableDecoderFallback,
            eventHandler,
            videoRendererEventListener,
            allowedVideoJoiningTimeMs,
            renderersList
        )
        val audioSink = buildAudioSink(
            context,
            enableFloatOutput,
            enableAudioTrackPlaybackParams,
            enableOffload
        )
        audioSink?.let {
            buildAudioRenderers(
                context,
                extensionRendererMode,
                mediaCodecSelector, enableDecoderFallback, it, eventHandler,
                audioRendererEventListener, renderersList
            )
        }
        buildTextRenderers(
            context,
            textRendererOutput,
            eventHandler.looper,
            extensionRendererMode,
            renderersList
        )
        buildMetadataRenderers(
            context,
            metadataRendererOutput,
            eventHandler.looper,
            extensionRendererMode,
            renderersList
        )
        buildCameraMotionRenderers(context, extensionRendererMode, renderersList)
        buildMiscellaneousRenderers(context, eventHandler, extensionRendererMode, renderersList)
        return renderersList.toTypedArray()
    }

    /**
     * Builds video renderers for use by the player.
     *
     * @param context The [Context] associated with the player.
     * @param extensionRendererMode The extension renderer mode.
     * @param mediaCodecSelector A decoder selector.
     * @param enableDecoderFallback Whether to enable fallback to lower-priority decoders if decoder
     * initialization fails. This may result in using a decoder that is slower/less efficient than
     * the primary decoder.
     * @param eventHandler A handler associated with the main thread's looper.
     * @param eventListener An event listener.
     * @param allowedVideoJoiningTimeMs The maximum duration for which video renderers can attempt to
     * seamlessly join an ongoing playback, in milliseconds.
     * @param out An array to which the built renderers should be appended.
     */
    protected fun buildVideoRenderers(
        context: Context?,
        @ExtensionRendererMode extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector?,
        enableDecoderFallback: Boolean,
        eventHandler: Handler?,
        eventListener: VideoRendererEventListener?,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        val videoRenderer = MediaCodecVideoRenderer(
            context!!,
            getCodecAdapterFactory(),
            mediaCodecSelector!!,
            allowedVideoJoiningTimeMs,
            enableDecoderFallback,
            eventHandler,
            eventListener,
            MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
        )
        out.add(videoRenderer)
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return
        }
        var extensionRendererIndex = out.size
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--
        }
        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz = Class.forName("com.aidong.media.extension.vp9.LibvpxVideoRenderer")
            val constructor = clazz.getConstructor(
                Long::class.javaPrimitiveType,
                Handler::class.java,
                VideoRendererEventListener::class.java,
                Int::class.javaPrimitiveType
            )
            val renderer = constructor.newInstance(
                allowedVideoJoiningTimeMs,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
            ) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded LibvpxVideoRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating VP9 extension", e)
        }
        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz = Class.forName("com.aidong.media.extension.av1.Libgav1VideoRenderer")
            val constructor = clazz.getConstructor(
                Long::class.javaPrimitiveType,
                Handler::class.java,
                VideoRendererEventListener::class.java,
                Int::class.javaPrimitiveType
            )
            val renderer = constructor.newInstance(
                allowedVideoJoiningTimeMs,
                eventHandler,
                eventListener,
                MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY
            ) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded Libgav1VideoRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating AV1 extension", e)
        }
    }

    /**
     * Builds audio renderers for use by the player.
     *
     * @param context The [Context] associated with the player.
     * @param extensionRendererMode The extension renderer mode.
     * @param mediaCodecSelector A decoder selector.
     * @param enableDecoderFallback Whether to enable fallback to lower-priority decoders if decoder
     * initialization fails. This may result in using a decoder that is slower/less efficient than
     * the primary decoder.
     * @param audioSink A sink to which the renderers will output.
     * @param eventHandler A handler to use when invoking event listeners and outputs.
     * @param eventListener An event listener.
     * @param out An array to which the built renderers should be appended.
     */
    protected fun buildAudioRenderers(
        context: Context?,
        @ExtensionRendererMode extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector?,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink?,
        eventHandler: Handler?,
        eventListener: AudioRendererEventListener?,
        out: ArrayList<Renderer>
    ) {
        val audioRenderer = MediaCodecAudioRenderer(
            context!!,
            getCodecAdapterFactory(),
            mediaCodecSelector!!,
            enableDecoderFallback,
            eventHandler,
            eventListener,
            audioSink!!
        )
        out.add(audioRenderer)
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return
        }
        var extensionRendererIndex = out.size
        if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--
        }
        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz = Class.forName("com.aidong.media.extension.LibopusAudioRenderer")
            val constructor = clazz.getConstructor(
                Handler::class.java,
                AudioRendererEventListener::class.java,
                AudioSink::class.java
            )
            val renderer =
                constructor.newInstance(eventHandler, eventListener, audioSink) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded LibopusAudioRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating Opus extension", e)
        }
        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz = Class.forName("com.aidong.media.extension.flac.LibflacAudioRenderer")
            val constructor = clazz.getConstructor(
                Handler::class.java,
                AudioRendererEventListener::class.java,
                AudioSink::class.java
            )
            val renderer =
                constructor.newInstance(eventHandler, eventListener, audioSink) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded LibflacAudioRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating FLAC extension", e)
        }
        try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            val clazz =
                Class.forName("com.aidong.media.extension.ffmpeg.FfmpegAudioRenderer")
            val constructor = clazz.getConstructor(
                Handler::class.java,
                AudioRendererEventListener::class.java,
                AudioSink::class.java
            )
            val renderer =
                constructor.newInstance(eventHandler, eventListener, audioSink) as Renderer
            out.add(extensionRendererIndex++, renderer)
            Log.i(TAG, "Loaded FfmpegAudioRenderer.")
        } catch (e: ClassNotFoundException) {
            // Expected if the app was built without the extension.
        } catch (e: Exception) {
            // The extension is present, but instantiation failed.
            throw RuntimeException("Error instantiating FFmpeg extension", e)
        }
    }

    /**
     * Builds text renderers for use by the player.
     *
     * @param context The [Context] associated with the player.
     * @param output An output for the renderers.
     * @param outputLooper The looper associated with the thread on which the output should be called.
     * @param extensionRendererMode The extension renderer mode.
     * @param out An array to which the built renderers should be appended.
     */
    protected fun buildTextRenderers(
        context: Context?,
        output: TextOutput?,
        outputLooper: Looper?,
        @ExtensionRendererMode extensionRendererMode: Int,
        out: ArrayList<Renderer>
    ) {
        out.add(TextRenderer(output!!, outputLooper))
    }

    /**
     * Builds metadata renderers for use by the player.
     *
     * @param context The [Context] associated with the player.
     * @param output An output for the renderers.
     * @param outputLooper The looper associated with the thread on which the output should be called.
     * @param extensionRendererMode The extension renderer mode.
     * @param out An array to which the built renderers should be appended.
     */
    protected fun buildMetadataRenderers(
        context: Context?,
        output: MetadataOutput?,
        outputLooper: Looper?,
        @ExtensionRendererMode extensionRendererMode: Int,
        out: ArrayList<Renderer>
    ) {
        out.add(MetadataRenderer(output!!, outputLooper))
    }

    /**
     * Builds camera motion renderers for use by the player.
     *
     * @param context The [Context] associated with the player.
     * @param extensionRendererMode The extension renderer mode.
     * @param out An array to which the built renderers should be appended.
     */
    protected fun buildCameraMotionRenderers(
        context: Context?,
        @ExtensionRendererMode extensionRendererMode: Int,
        out: ArrayList<Renderer>
    ) {
        out.add(CameraMotionRenderer())
    }

    /**
     * Builds any miscellaneous renderers used by the player.
     *
     * @param context The [Context] associated with the player.
     * @param eventHandler A handler to use when invoking event listeners and outputs.
     * @param extensionRendererMode The extension renderer mode.
     * @param out An array to which the built renderers should be appended.
     */
    protected fun buildMiscellaneousRenderers(
        context: Context?,
        eventHandler: Handler?,
        @ExtensionRendererMode extensionRendererMode: Int,
        out: ArrayList<Renderer>?
    ) {
        // Do nothing.
    }

    /**
     * Builds an [AudioSink] to which the audio renderers will output.
     *
     * @param context The [Context] associated with the player.
     * @param enableFloatOutput Whether to enable use of floating point audio output, if available.
     * @param enableAudioTrackPlaybackParams Whether to enable setting playback speed using [     ][android.media.AudioTrack.setPlaybackParams], if supported.
     * @param enableOffload Whether to enable use of audio offload for supported formats, if
     * available.
     * @return The [AudioSink] to which the audio renderers will output. May be `null` if
     * no audio renderers are required. If `null` is returned then [     ][.buildAudioRenderers] will not be called.
     */
    protected fun buildAudioSink(
        context: Context?,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
        enableOffload: Boolean
    ): AudioSink? {
        return DefaultAudioSink(
            AudioCapabilities.getCapabilities(context!!),
            DefaultAudioProcessorChain(),
            enableFloatOutput,
            enableAudioTrackPlaybackParams,
            if (enableOffload) DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED else DefaultAudioSink.OFFLOAD_MODE_DISABLED
        )
    }

    /**
     * Returns the [MediaCodecAdapter.Factory] that will be used when creating [ ] instances.
     */
    protected fun getCodecAdapterFactory(): MediaCodecAdapter.Factory {
        return codecAdapterFactory
    }

    companion object {
        /**
         * The default maximum duration for which a video renderer can attempt to seamlessly join an
         * ongoing playback.
         */
        const val DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS: Long = 5000

        /** Do not allow use of extension renderers.  */
        const val EXTENSION_RENDERER_MODE_OFF = 0

        /**
         * Allow use of extension renderers. Extension renderers are indexed after core renderers of the
         * same type. A [TrackSelector] that prefers the first suitable renderer will therefore
         * prefer to use a core renderer to an extension renderer in the case that both are able to play a
         * given track.
         */
        const val EXTENSION_RENDERER_MODE_ON = 1

        /**
         * Allow use of extension renderers. Extension renderers are indexed before core renderers of the
         * same type. A [TrackSelector] that prefers the first suitable renderer will therefore
         * prefer to use an extension renderer to a core renderer in the case that both are able to play a
         * given track.
         */
        const val EXTENSION_RENDERER_MODE_PREFER = 2

        /**
         * The maximum number of frames that can be dropped between invocations of [ ][VideoRendererEventListener.onDroppedFrames].
         */
        const val MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY = 50
        private const val TAG = "ExoRenderersFactory"
    }
}