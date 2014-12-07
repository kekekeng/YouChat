package com.acesse.youchat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;



public class VideoCaptureActivity extends Activity implements OnClickListener, SurfaceHolder.Callback {

    private static final String TAG = "YOUC";

    private int MAX_DURATION = 30;

    private MediaRecorder recorder;
    private boolean recording = false;
    private boolean isFront = false;
    private Camera camera;
    private Timer mTimer;
    private int seconds;
    private String outputFilePath;
    private ProgressBar progressBar;
    private TextView timerText;
    private AsyncTask asyncTask;
    private ImageView thumbNail;
    private View doneButton, switchButton;
    private Handler handler = new Handler();
    private Bitmap bitmap;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Config.DEBUG) {
            Log.d(TAG, "VideoCaptureActivity.onCreate");
        }

        setContentView(R.layout.activity_video_capture);

        // Disabling until the Surface is created.
        findViewById(R.id.record_button).setEnabled(false);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.surface_camera);
        cameraView.getHolder().addCallback(this);

        findViewById(R.id.record_button).setOnClickListener(this);
        timerText = (TextView) findViewById(R.id.timer_text);
        progressBar = (ProgressBar) findViewById(R.id.recorder_progress_bar);
        thumbNail = (ImageView) findViewById(R.id.video_thumbnail);
        doneButton = findViewById(R.id.done_button);
        switchButton = findViewById(R.id.switch_button);
        doneButton.setOnClickListener(this);
        switchButton.setOnClickListener(this);
        thumbNail.setOnClickListener(this);


        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        if (Config.DEBUG) {
            Log.d(TAG, "number of cameras: " + numberOfCameras);
            for (int i = 0; i < numberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Log.d(TAG, "camera " + i + " is front facing");
                } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    Log.d(TAG, "camera " + i + " is back facing");
                }
            }
        }

        if (numberOfCameras > 1) {
            switchButton.setVisibility(View.VISIBLE);
        }

        if (Config.DEBUG) {
            if (getIntent().hasExtra("AUTOMATION")) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        MAX_DURATION = 10;
                        findViewById(R.id.record_button).performClick();
                    }
                }, 2000);
            }
        }
    }


    /*
    1.) Open Camera - Use the Camera.open() to get an instance of the camera object.
    2.) Connect Preview - Prepare a live camera image preview by connecting a SurfaceView to the camera using Camera.setPreviewDisplay().
    3.) Start Preview - Call Camera.startPreview() to begin displaying the live camera images.
    4.) Start Recording Video - The following steps must be completed in order to successfully record video:
        a.) Unlock the Camera - Unlock the camera for use by MediaRecorder by calling Camera.unlock().
        b.) Configure MediaRecorder - Call in the following MediaRecorder methods in this order. For more information, see the MediaRecorder reference documentation.
            1.) setCamera() - Set the camera to be used for video capture, use your application's current instance of Camera.
            2.) setAudioSource() - Set the audio source, use MediaRecorder.AudioSource.CAMCORDER.
            3.) setVideoSource() - Set the video source, use MediaRecorder.VideoSource.CAMERA.
            4.) Set the video output format and encoding. For Android 2.2 (API Level 8) and higher, use the MediaRecorder.setProfile method, and get a profile instance using CamcorderProfile.get(). 
                i.) setOutputFormat() - Set the output format, specify the default setting or MediaRecorder.OutputFormat.MPEG_4.
                ii.) setAudioEncoder() - Set the sound encoding type, specify the default setting or MediaRecorder.AudioEncoder.AMR_NB.
                iii.) setVideoEncoder() - Set the video encoding type, specify the default setting or MediaRecorder.VideoEncoder.MPEG_4_SP.
            5.) setOutputFile() - Set the output file, use getOutputMediaFile(MEDIA_TYPE_VIDEO).toString() from the example method in the Saving Media Files section.
            6.) setPreviewDisplay() - Specify the SurfaceView preview layout element for your application. Use the same object you specified for Connect Preview.
            Caution: You must call these MediaRecorder configuration methods in this order, otherwise your application will encounter errors and the recording will fail.
        c.) Prepare MediaRecorder - Prepare the MediaRecorder with provided configuration settings by calling MediaRecorder.prepare().
        d.) Start MediaRecorder - Start recording video by calling MediaRecorder.start().
    5.) Stop Recording Video - Call the following methods in order, to successfully complete a video recording:
        a.) Stop MediaRecorder - Stop recording video by calling MediaRecorder.stop().
        b.) Reset MediaRecorder - Optionally, remove the configuration settings from the recorder by calling MediaRecorder.reset().
        c.) Release MediaRecorder - Release the MediaRecorder by calling MediaRecorder.release().
        d.) Lock the Camera - Lock the camera so that future MediaRecorder sessions can use it by calling Camera.lock(). Starting with Android 4.0 (API level 14), this call is not required unless the MediaRecorder.prepare() call fails.
    6.) Stop the Preview - When your activity has finished using the camera, stop the preview using Camera.stopPreview().
    7.) Release Camera - Release the camera so that other applications can use it by calling Camera.release().
     */
    private void initRecorderSync(SurfaceHolder holder) {

        if (Config.DEBUG) {
            Log.d(TAG, "------------------------------------------------INIT RECORDER");
        }

        int cameraId = isFront ? 1 : 0;
        try {
            // Step 1 open the camera
            camera = Camera.open(cameraId); 
        } catch (Exception e) {
            Log.w(TAG, "CAMERA OPEN FAILED", e);
            finish();
            return;
        }

        Camera.Parameters params = null;
        List<Camera.Size> supportedVideoSizes = null;
        try {
            params = camera.getParameters();
            supportedVideoSizes = params.getSupportedVideoSizes();
            if (supportedVideoSizes == null || supportedVideoSizes.size() == 0) {
                if (Config.DEBUG) {
                   Log.d(TAG, "No Supported Video Sizes, Trying Supported Preview Sizes.");
                }
                supportedVideoSizes = params.getSupportedPreviewSizes();
            }
            if (Config.DEBUG) {
                if (supportedVideoSizes != null) {
                    for (Camera.Size size : supportedVideoSizes) {
                        Log.d(TAG, "SUPPORTED SIZES: " + size.width + "," + size.height);
                    }
                } else {
                    Log.w(TAG, "NO SUPPORTED SIZES!!!");
                }
            }

            if (params.getSupportedFocusModes() != null) {
                if (Config.DEBUG) {
                    for (String mode : params.getSupportedFocusModes()) {
                        Log.d(TAG, "SUPPORTED FOCUS MODE: " + mode);
                    }
                }
                if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                camera.setParameters(params);
            }

        } catch (Exception ex) {
            Log.e(TAG, "Error setting camera params", ex);
            //
            // How to recover from this?  Try another setting.
            //
        }


        // what step is this?
        camera.setDisplayOrientation(90);

        // Step 2.
        try {
            camera.setPreviewDisplay(holder);
        } catch (Exception e) {
            Log.w(TAG, "CAMERA SET PREVIEW DISPLAY FAILED", e);
            finish();
            return;
        }

        // Step 3.
        try {
            camera.startPreview();
        } catch (Exception e) {
            Log.w(TAG, "CAMERA START PREVIEW FAILED", e);
            finish();
            return;
        }

        // Step 4.a.
        try {
            camera.unlock();
        } catch (Exception e) {
            Log.w(TAG, "CAMERA UNLOCK FAILED", e);
            finish();
            return;
        }

        recorder = new MediaRecorder();
        recorder.setPreviewDisplay(holder.getSurface());

        // Step 4.b.1
        recorder.setCamera(camera);

        /*
        D/YOUC    (18290): SUPPORTED SIZES: 1280,720
        D/YOUC    (18290): SUPPORTED SIZES: 800,480
        D/YOUC    (18290): SUPPORTED SIZES: 720,480
        D/YOUC    (18290): SUPPORTED SIZES: 640,480
        D/YOUC    (18290): SUPPORTED SIZES: 480,320
        D/YOUC    (18290): SUPPORTED SIZES: 352,288
        D/YOUC    (18290): SUPPORTED SIZES: 320,240
         */

        boolean found = false;

        if (supportedVideoSizes != null && supportedVideoSizes.size() > 0) {

            recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

            for (int profile : new int[] { CamcorderProfile.QUALITY_480P, CamcorderProfile.QUALITY_720P, CamcorderProfile.QUALITY_CIF }) {
                if (CamcorderProfile.hasProfile(cameraId, profile)) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "Using CamcorderProfile: " + profile);
                    }
                    recorder.setProfile(CamcorderProfile.get(cameraId, profile)); 
                    found = true;
                    break;
                }
            }


        } else {

//            Camera.Size psize = params.getPreviewSize();
//            if (Config.DEBUG) {
//                Log.d(TAG, "Manual Setup of MediaRecorder. Using Size: " + psize.width + "," + psize.height);
//            }
//
//            // Step 4.b.2
//            recorder.setAudioSource(MediaRecorder.AudioSource.MIC); 
//            // Step 4.b.3
//            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//            // Step 4.b.4.i
//            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//            // Step 4.b.4.ii
//            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); 
//            // Step 4.b.4.iii
//            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); 
//
//            recorder.setVideoFrameRate(15);
//
//            recorder.setVideoSize(psize.width, psize.height);

        }

        if (!found) {
            if (Config.DEBUG) {
                Log.d(TAG, "Using QUALITY_HIGH CamcorderProfile");
            }
            // LOW or HIGH setting is guaranteed to work...
            recorder.setProfile(CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)); 
        }

        // Step 4.b.5
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        File file = new File(MainApplication.getChatStorageDirectory(), timeStamp + ".mp4");
        recorder.setOutputFile(outputFilePath = file.getAbsolutePath());

        // Where does this go?
        recorder.setOrientationHint(isFront ? 270 : 90);

        // Step 4.c
        try {
            recorder.prepare();
        } catch (Exception e) {
            Log.w(TAG, "FAILED TO PREPARE", e);
            finish();
        }
    }


    private void initRecorderAsync(final SurfaceHolder holder, final long delay) {
        asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // Weird GS4 timing issue that requires delay before switching cameras...
                    Thread.sleep(delay);
                } catch (Exception ex) {
                }
                if (!isCancelled()) {
                    initRecorderSync(holder);
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                findViewById(R.id.record_button).setEnabled(true);
            };
        }.execute();
    }


    private void setVideoThumbnail() {
        try { 
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(outputFilePath);
            bitmap = retriever.getFrameAtTime(-1);
            if (bitmap != null) {
                thumbNail.setVisibility(View.VISIBLE);
                thumbNail.setImageBitmap(bitmap);
                thumbNail.setTag(outputFilePath);
            }
        } catch (Exception ex) {
            outputFilePath = null;
            Log.w(TAG, "FAILED TO GENERATE VIDEO THUMBNAIL", ex);
        }
    }


    public void onClick(View v) {
        if (v.getId() == R.id.record_button) {
            findViewById(R.id.record_button).setEnabled(false);
            if (recording) {
                if (Config.DEBUG) {
                    Log.d(TAG, "------------------------------------------------STOPPING");
                }
                doneButton.setVisibility(View.VISIBLE);
                switchButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                ((ImageView) v).setImageResource(R.drawable.ic_action_record);
                recording = false;
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                try {
                    recorder.stop();
                } catch (Exception ex) {
                    Log.w(TAG, "FAILED TO STOP", ex);
                    release();
                    initRecorderSync(((SurfaceView) findViewById(R.id.surface_camera)).getHolder());
                }
                setVideoThumbnail();
                if (Config.DEBUG) {
                    if (getIntent().hasExtra("AUTOMATION")) {
                        doneButton.performClick();
                    }
                }
            } else {
                if (thumbNail.getVisibility() == View.VISIBLE) {
                    release();
                    initRecorderSync(((SurfaceView) findViewById(R.id.surface_camera)).getHolder());
                }
                doneButton.setVisibility(View.GONE);
                thumbNail.setVisibility(View.INVISIBLE);
                switchButton.setVisibility(View.INVISIBLE);
                timerText.setVisibility(View.VISIBLE);
                ((ImageView) v).setImageResource(R.drawable.ic_action_stop);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
                if (Config.DEBUG) {
                    Log.d(TAG, "------------------------------------------------STARTING");
                }
                recording = true;
                // Step 4.d
                try {
                    if(recorder != null) {
                        recorder.start();
                        initTimerTask();
                    }
                } catch (Exception ex) {
                    Log.w(TAG, "FAILED TO START", ex);
                    finish();
                }
            }
            findViewById(R.id.record_button).setEnabled(true);
        } else if (v.getId() == R.id.done_button) {
            if (Config.DEBUG) {
                Log.d(TAG, "------------------------------------------------DONE");
            }
            outputFilePath = (String) thumbNail.getTag();
            if (Config.DEBUG) {
                Log.d(TAG, "OUTPUT FILE: " + outputFilePath);
            }
            if (outputFilePath != null) {
                setResult(Activity.RESULT_OK, new Intent().setData(Uri.parse("file:" + outputFilePath)));
            } else {
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
        } else if (v.getId() == R.id.switch_button) {
            isFront = !isFront;
            thumbNail.setVisibility(View.INVISIBLE);
            doneButton.setVisibility(View.GONE);
            timerText.setVisibility(View.GONE);
            release();
            initRecorderAsync(((SurfaceView) findViewById(R.id.surface_camera)).getHolder(), 50);
        } else if (v.getId() == R.id.video_thumbnail) {
            startActivity(new Intent(this, VideoPlaybackActivity.class).putExtra("video.file", (String) thumbNail.getTag()));
        }
    }



    private void initTimerTask() {
        timerText.setVisibility(View.VISIBLE);
        seconds = 0;
        timerText.setText(String.valueOf(seconds));
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
                        if (timerText.getVisibility() == View.VISIBLE) {
                            timerText.setText(String.valueOf(seconds));
                        }
                        progressBar.setProgress(seconds);
                    }
                });
            }
        }, 999, 1000);
    }


    private void release() {
        if (Config.DEBUG) {
            Log.d(TAG, "------------------------------------------------RELEASE BEGIN");
        }
        try {
            // Step 5.a
            if (recording) {
                recorder.stop();
                recording = false;
            }
        } catch (Exception ex) {
        }
        try {
            // Step 5.b
            //recorder.reset(); // optional: remove config settings...
        } catch (Exception ex) {
        }
        try {
            // Step 5.c
            recorder.release();
        } catch (Exception ex) {
        }
        try {
            // Step 5.d
            //Lock the Camera - Lock the camera so that future MediaRecorder sessions can use it by calling Camera.lock(). Starting with Android 4.0 (API level 14), this call is not required unless the MediaRecorder.prepare() call fails.
            camera.lock();
        } catch (Exception ex) {
        }
        try {
            // Step 6
            camera.stopPreview();
        } catch (Exception ex) {
        }
        try {
            // Step 7
            camera.release();
        } catch (Exception ex) {
        }
        try {
            asyncTask.cancel(false);
        } catch (Exception ex) {
        }
        if (Config.DEBUG) {
            Log.d(TAG, "------------------------------------------------RELEASE END");
        }
    }


    public void surfaceCreated(SurfaceHolder holder) {
        initRecorderAsync(holder, 0);
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        if (Config.DEBUG) {
            Log.d(TAG, "------------------------------------------------SURFACE DESTROYED");
        }
        release();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (bitmap != null && !bitmap.isRecycled()) {
            try {
                bitmap.recycle();
            } catch (Exception ex) {
            }
        }
    }
}
