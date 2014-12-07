package com.acesse.youchat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.crittercism.app.Crittercism;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;



public class AudioCaptureActivity extends Activity implements OnClickListener {

    private static final String TAG = "YOUC";

    private static final int MAX_DURATION = 30;

    private Timer mTimer;
    private int seconds;
    private String mFilePath;
    private MediaRecorder mRecorder;
    private MediaPlayer   mPlayer;
    private ProgressBar progressBar;
    private SharedPreferences preferences;
    private boolean recording = false;


    @Override
    public void onCreate(Bundle icicle) {
        Log.d(TAG, getClass().getSimpleName() + ".onCreate");
        Crittercism.leaveBreadcrumb("AudioCaptureActivity : onCreate");
        super.onCreate(icicle);
        setContentView(R.layout.activity_audio_capture);
        findViewById(R.id.record_button).setOnClickListener(this);
        findViewById(R.id.done_button).setOnClickListener(this);
        progressBar = (ProgressBar) findViewById(R.id.recorder_progress_bar);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (getIntent().hasExtra("audio.file")) {
            findViewById(R.id.record_button).setVisibility(View.GONE);
            findViewById(R.id.done_button).setVisibility(View.VISIBLE);
            mFilePath = getIntent().getStringExtra("audio.file");
            initTimerTask();
            startPlaying();
        }

        //getActionBar().setSubtitle(ContactDAO.getInstance().getAccountFullName());
    }


    @Override
    public void onPause() {
        Log.d(TAG, getClass().getSimpleName() + ".onPause");
        Crittercism.leaveBreadcrumb("AudioCaptureActivity : onPause");
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }


    @Override
    public void onClick(View v) {
        Log.d(TAG, getClass().getSimpleName() + ".onClick");
        if (v.getId() == R.id.record_button) {
            if (recording) {
                findViewById(R.id.done_button).setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                ((ImageView) findViewById(R.id.record_button)).setImageResource(R.drawable.ic_action_record);
                preferences.edit().putString("audio.file", mFilePath).commit();
                recording = false;
                stopRecording();
            } else {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
                File file = new File(MainApplication.getChatStorageDirectory(), timeStamp + ".aac");
                mFilePath = file.getAbsolutePath();
                findViewById(R.id.done_button).setVisibility(View.GONE);
                findViewById(R.id.timer_text).setVisibility(View.VISIBLE);
                ((ImageView) findViewById(R.id.record_button)).setImageResource(R.drawable.ic_action_stop);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                recording = true;
                startRecording();
            }
        } else if (v.getId() == R.id.done_button) {
            mFilePath = preferences.getString("audio.file", mFilePath);
            if (mFilePath != null) {
                setResult(Activity.RESULT_OK, new Intent().setData(Uri.parse("file:" + mFilePath)));
            } else {
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
            preferences.edit().remove("audio.file").commit();
        }
    }

    private void startPlaying() {
        Log.d(TAG, getClass().getSimpleName() + ".startPlaying");
        String duration = MainApplication.getMediaDuration(mFilePath);
        if (duration != null) {
            try {
                int dur = Integer.parseInt(duration.substring(duration.indexOf(":")+1));
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                progressBar.setMax(dur);
            } catch (Exception ex) {
            }
        }
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "AUDIO COMPLETED");
                    finish();
                }
            });
            mPlayer.setDataSource(mFilePath);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "mediaplayer prepare() failed");
        }
    }


    private void stopPlaying() {
        Log.d(TAG, getClass().getSimpleName() + ".stopPlaying");
        try {
            mPlayer.release();
            mPlayer = null;
        } catch (Exception ex) {
            Log.e(TAG, "mediaplayer release failed");
        }
    }


    private void startRecording() {
        Log.d(TAG, getClass().getSimpleName() + ".startRecording");
        
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioEncodingBitRate(96000);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setOutputFile(mFilePath);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "media recorder prepare() failed");
        }

        mRecorder.start();
        initTimerTask();
    }

    private void stopRecording() {
        Log.d(TAG, getClass().getSimpleName() + ".stopRecording");
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
            } catch (Exception ex) {
                Log.e(TAG, "stop recording failed");
            }
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void initTimerTask() {
        findViewById(R.id.timer_text).setVisibility(View.VISIBLE);
        seconds = 0;
        ((TextView) findViewById(R.id.timer_text)).setText(String.valueOf(seconds));
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (++seconds == MAX_DURATION) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            findViewById(R.id.record_button).performClick();
                        }
                    });
                    mTimer.cancel();
                    mTimer = null;
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        ((TextView) findViewById(R.id.timer_text)).setText(String.valueOf(seconds));
                        progressBar.setProgress(seconds);
                    }
                });
            }
        }, 999, 1000);
    }
}