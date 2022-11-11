package com.google.mlkit.vision.demo.video;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;

import java.io.IOException;

public class VideoTextureViewActivity extends VideoBaseActivity implements TextureView.SurfaceTextureListener, Player.Listener {
    private static final String TAG = "VideoTextureViewActivit";
    private Long key = 0L;
    private ExoPlayer player;
    private TextureView textureView;
    private Surface playerSurface;
    private SurfaceTexture surfaceTexture;
    private Handler handler;

    @Override
    protected ExoPlayer createPlayer() {
        handler = new Handler(Looper.getMainLooper());
        player = new ExoPlayer.Builder(this).build();
        player.addListener(this);
        return player;
    }

    @Override
    protected View createVideoFrameView() {
        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(this);
        return textureView;
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        surfaceTexture = surface;
        playerSurface = new Surface(surface);
        player.setVideoSurface(playerSurface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        player.setVideoSurface(null);
        playerSurface.release();
        playerSurface = null;
        surfaceTexture.release();
        surfaceTexture = null;
        return true;
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_ENDED) {
            Log.d(TAG, "onPlaybackStateChanged: 播放结束");
            try {
                imageProcessor.saveToJson("test002.json");
            } catch (IOException e) {
                Log.d(TAG, "onPlaybackStateChanged: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    boolean isSave = false;
    long l = 0;

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        Size size = getSizeForDesiredSize(textureView.getWidth(), textureView.getHeight(), 500);
        handler.post(() -> {
            l = player.getCurrentPosition() / 1000;
            if (key != l) {
                isSave = l % 3 == 0L;
                key = l;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Bitmap bitmap = textureView.getBitmap(size.getWidth(), size.getHeight());
                    processFrame(bitmap, isSave, l);

                    Log.d(TAG, "onSurfaceTextureUpdated: size:" + bitmap.getWidth() + "---" + bitmap.getHeight());
//            preView.setImageBitmap(bitmap);
                }
            }
        });

    }
}
