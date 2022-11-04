package com.google.mlkit.vision.demo.video;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.SimpleExoPlayer;

public class VideoTextureViewActivity extends VideoBaseActivity implements TextureView.SurfaceTextureListener {

    private SimpleExoPlayer player;
    private TextureView textureView;
    private Surface playerSurface;
    private SurfaceTexture surfaceTexture;
    private Handler handler;

    @Override
    protected SimpleExoPlayer createPlayer() {
        handler = new Handler(Looper.getMainLooper());
        player = new SimpleExoPlayer.Builder(getContext()).build();
        return player;
    }

    @Override
    protected View createVideoFrameView() {
        textureView = new TextureView(getContext());
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

    boolean isSave = false;
    int sl = 0;

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        Size size = getSizeForDesiredSize(textureView.getWidth(), textureView.getHeight(), 500);
        sl++;
//        handler.post(() -> {
//            isSave = player.getCurrentPosition() / 100 % 3 == 0L;
//            Log.d("PoseDetectorProcessor", "onDrawFrame: " + isSave + "--" + player.getCurrentPosition());
//        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Bitmap bitmap = textureView.getBitmap(size.getWidth(), size.getHeight());
                processFrame(bitmap, isSave);

//            vvvvv.setImageBitmap(bitmap);
        }
    }
}
