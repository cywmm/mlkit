package com.aidong.media.utils;

import android.content.Context;

import com.aidong.media.audio.exo.ExoRenderersFactory;
import com.google.android.exoplayer2.RenderersFactory;

/**
 * date: 2021/12/27 18:59
 * author: wangming
 * dec:
 */
public class Utils {

    public static boolean useExtensionRenderers() {
        return true;
    }

    public static RenderersFactory buildRenderersFactory(
            Context context, boolean preferExtensionRenderer) {
        @ExoRenderersFactory.ExtensionRendererMode
        int extensionRendererMode =
                useExtensionRenderers()
                        ? (preferExtensionRenderer
                        ? ExoRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : ExoRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : ExoRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
        return new ExoRenderersFactory(context.getApplicationContext())
                .setExtensionRendererMode(extensionRendererMode);
    }
}
