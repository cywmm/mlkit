package com.google.mlkit.vision.demo.video;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

public abstract class VideoBaseActivity extends Activity {
    private static final String TAG = VideoBaseActivity.class.getSimpleName();

    private static final int REQUEST_CHOOSE_VIDEO = 1001;
    private static final String SELFIE_POSE = "Pose";

    private GraphicOverlay graphicOverlay;
    private ExoPlayer player;
    private StyledPlayerView playerView;
//    private VideoView playerView;

    private VisionVideoProcessorBase imageProcessor;
    private String selectedProcessor = SELFIE_POSE;

    private int frameWidth, frameHeight;

    private boolean processing;
    private boolean pending;
    private Bitmap lastFrame;
    private final Uri parse = Uri.parse("/storage/emulated/0/DCIM/output.mp4");
//    private final Uri parse = Uri.parse("https://online-resources.oss-cn-shanghai.aliyuncs.com/VIRTUAL/AI/16x9/hls/A1/resource.m3u8");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_video);

        player = createPlayer();

        playerView = findViewById(R.id.player_view);
        graphicOverlay = findViewById(R.id.graphic_overlay2);

        playerView.setPlayer(player);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
//        FrameLayout contentFrame = playerView.findViewById(R.id.exo_content_frame);
//        View videoFrameView = createVideoFrameView();
//        if (videoFrameView != null) contentFrame.addView(videoFrameView);

        setupPlayer(parse);
    }
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.activity_base_video, container, false);
//        initView(view);
//        return view;
//    }

    private void initView(View view) {
        player = createPlayer();

        playerView = view.findViewById(R.id.player_view);
        graphicOverlay = view.findViewById(R.id.graphic_overlay2);


//        playerView.setVideoURI(parse);
//        playerView.setOnPreparedListener(mp -> playerView.start());
        playerView.setPlayer(player);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        FrameLayout contentFrame = playerView.findViewById(R.id.exo_content_frame);
        View videoFrameView = createVideoFrameView();
        if (videoFrameView != null) contentFrame.addView(videoFrameView);

//        setupPlayer(parse);
        setupPlayer(parse);
    }

    protected abstract @NonNull
    ExoPlayer createPlayer();

    protected abstract @Nullable
    View createVideoFrameView();

    protected Size getSizeForDesiredSize(int width, int height, int desiredSize) {
        int w, h;
        if (width > height) {
            w = desiredSize;
            h = Math.round((height / (float) width) * w);
        } else {
            h = desiredSize;
            w = Math.round((width / (float) height) * h);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new Size(w, h);
        }

        return null;
    }

    protected void processFrame(Bitmap frame, boolean isSave) {
        lastFrame = frame;
        if (imageProcessor != null) {
            pending = processing;
            if (!processing) {
                processing = true;
                if (frameWidth != frame.getWidth() || frameHeight != frame.getHeight()) {
                    frameWidth = frame.getWidth();
                    frameHeight = frame.getHeight();
                    graphicOverlay.setImageSourceInfo(frameWidth, frameHeight, false);
                }
                imageProcessor.setOnProcessingCompleteListener(() -> {
                    processing = false;
                    onProcessComplete(frame);
                    if (pending) processFrame(lastFrame, isSave);
                });
//                imageProcessor.processBitmap(frame, graphicOverlay);
                imageProcessor.processBitmap(frame, graphicOverlay, isSave);
            }
        }
    }

    protected void onProcessComplete(Bitmap frame) {
    }

    @Override
    public void onResume() {
        super.onResume();
        createImageProcessor();

    }

    @Override
    public void onPause() {
        super.onPause();
        player.pause();
        stopImageProcessor();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
    }

    private void setupPlayer(Uri uri) {
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.stop();
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void startChooseVideoIntentForResult() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_CHOOSE_VIDEO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHOOSE_VIDEO && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            setupPlayer(data.getData());
        }
    }

    private void createImageProcessor() {
        stopImageProcessor();

        try {
            switch (selectedProcessor) {
                case SELFIE_POSE:
                    PoseDetectorOptionsBase poseDetectorOptions =
                            PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
                    boolean shouldShowInFrameLikelihood =
                            PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this);
                    boolean visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this);
                    boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
                    boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
                    PoseDetectorOptions.Builder builder =
                            new PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE);
                    builder.setPreferredHardwareConfigs(PoseDetectorOptions.CPU);
                    imageProcessor = new PoseDetectorVideoProcessor(this,
                            builder.build(),
                            shouldShowInFrameLikelihood,
                            visualizeZ,
                            rescaleZ,
                            false,
                            /* isStreamMode = */ true);
                    break;
                default:
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + selectedProcessor, e);
            Toast.makeText(
                            this,
                            "Can not create image processor: " + e.getMessage(),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void stopImageProcessor() {
        if (imageProcessor != null) {
            imageProcessor.stop();
            imageProcessor = null;
            processing = false;
            pending = false;
        }
    }
}