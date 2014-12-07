package com.acesse.youchat;

import java.util.Timer;
import java.util.TimerTask;

import android.app.DialogFragment;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;


public class VideoDialogFragment extends DialogFragment implements OnCompletionListener, OnClickListener {


    private static final String TAG = "YOUC";

    private String uri;
    private VideoView videoView;
    private TextView timerText;
    private Timer mTimer;
    private int seconds;
    private ImageView pauseButton;


    static VideoDialogFragment newInstance(String uri) {
        VideoDialogFragment f = new VideoDialogFragment();
        f.uri = uri;
        return f;
    }

    public VideoDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_view, container, false);
        videoView = (VideoView) view.findViewById(R.id.video_view);
        MediaController mediaController = new MediaController(getActivity());
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(uri));
        videoView.setOnCompletionListener(this);
        timerText = (TextView) view.findViewById(R.id.timer_text);
        pauseButton = (ImageView) view.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(this);
        return view;
    }


    @Override
    public void onResume() {
        if (Config.DEBUG) {
            Log.d(TAG, "VideoDialogFragment.onResume " + uri);
        }
        super.onResume();
        videoView.requestFocus();
        videoView.start();
        seconds = 0;
        startTimer();
    }


    @Override
    public void onPause() {
        if (Config.DEBUG) {
            Log.d(TAG, "VideoDialogFragment.onPause");
        }
        super.onPause();
        videoView.stopPlayback();
        stopTimer();
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        if (Config.DEBUG) {
            Log.d(TAG, "VideoDialogFragment.onCompletion");
        }
        stopTimer();
        pauseButton.setImageResource(R.drawable.ic_action_play);
        seconds = 0;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pause_button) {
            if (videoView.isPlaying()) {
                videoView.pause();
                stopTimer();
                ((ImageView) v).setImageResource(R.drawable.ic_action_play);
            } else {
                videoView.start();
                startTimer();
                ((ImageView) v).setImageResource(R.drawable.ic_action_stop);
            }
        }
    }


    private void startTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if (timerText.getVisibility() == View.VISIBLE) {
                                timerText.setText(String.valueOf(seconds++));
                            }
                        }
                    });
                }
            }
        }, 50, 1000);
    }


    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
}