package com.acesse.youchat;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.MediaController;
import android.widget.VideoView;



public class VideoPlaybackActivity extends Activity {


    private static final String TAG = "YOUC";

    private MediaController mediaController;
    private VideoView videoView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    

        setContentView(R.layout.activity_video_playback);

        videoView = (VideoView) findViewById(R.id.video_view);
        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        //        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        //            public void onCompletion(MediaPlayer mp) {
        //                mediaController.show(0);
        //            }
        //        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                videoView.requestFocus();
                videoView.start();
                mediaController.setEnabled(true);
                mediaController.show();
            }
        });
        String videoFile = getIntent().getStringExtra("video.file");
        if (Config.DEBUG) {
            Log.d(TAG, "Playing Video File: " + videoFile);
        }
        videoView.setVideoURI(Uri.parse(videoFile));
    }


    @Override
    public void onPause() {
        if (Config.DEBUG) {
            Log.d(TAG, "VideoPlaybackActivity.onPause");
        }
        super.onPause();
        videoView.stopPlayback();
    }


    @Override
    public void onBackPressed() {
        if (Config.DEBUG) {
            Log.d(TAG, "VideoPlaybackActivity.onBackPressed");
        }
        super.onBackPressed();
    }


    @Override 
    public boolean onTouchEvent(MotionEvent event) { 
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            try {
                mediaController.show(); 
            } catch (Exception ex) {
            }
        }
        return false; 
    }
}
