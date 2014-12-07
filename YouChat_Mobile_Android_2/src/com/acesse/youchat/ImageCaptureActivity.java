package com.acesse.youchat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;




public class ImageCaptureActivity extends Activity implements OnClickListener, SurfaceHolder.Callback {

    private static final String TAG = "YOUC";

    private Handler handler = new Handler();
    private boolean isFront = false;
    private Camera mCamera;
    private String outputFilePath;
    private MediaPlayer mAudioMP;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_capture);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.surface_camera);
        cameraView.getHolder().addCallback(this);

        findViewById(R.id.record_button).setOnClickListener(this);
        findViewById(R.id.done_button).setOnClickListener(this);
        findViewById(R.id.switch_button).setOnClickListener(this);
        findViewById(R.id.image_thumbnail).setOnClickListener(this);

        if (Config.DEBUG) {
            if (getIntent().hasExtra("AUTOMATION")) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        findViewById(R.id.record_button).performClick();
                    }
                }, 2000);
            }
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }

    private void initCamera(SurfaceHolder holder) {

        if (Config.DEBUG) {
            Log.d(TAG, "INIT CAMERA");
        }
        int cameraId = isFront ? 1 : 0;
        if (Camera.getNumberOfCameras() > 1) {
            findViewById(R.id.switch_button).setVisibility(View.VISIBLE);
        }

        try {
            // Step 1 open the camera
            mCamera = Camera.open(cameraId); 
        } catch (Exception e) {
            Log.w(TAG, "CAMERA OPEN FAILED", e);
            return;
        }

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes;
        if (params.getSupportedVideoSizes() != null) {
            sizes = params.getSupportedVideoSizes();
        } else {
            // Video sizes may be null, which indicates that all the supported 
            // preview sizes are supported for video recording.
            sizes = params.getSupportedPreviewSizes();
        }

        if (Config.DEBUG) {
            for (Camera.Size size : sizes) {
                Log.d(TAG, "SUPPORTED SIZE: " + size.width + "," + size.height);
            }
        }

        //
        // Assumption is that highest resolution sizes are first in the list
        //
        for (Camera.Size size : sizes) {
            if (Math.min(size.width, size.height) <= 800) {
                if (Config.DEBUG) {
                    Log.d(TAG, "TRYING PICTURE SIZE: " + size.width + "," + size.height);
                }
                try {
                    if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    } else if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    params.setPictureSize(size.width, size.height);
                    params.setJpegQuality(100);
                    params.setRotation(isFront ? 270 : 90);
                    //params.set("orientation", "portrait");
                    mCamera.setParameters(params);
                    break;
                } catch (Exception ex) {
                    Log.w(TAG, "Set camera params failed for size: " + size.width + "," + size.height);
                    // Try the next resolution setting...
                }
            }
        }

        if (Config.DEBUG) {
            Camera.Size size = mCamera.getParameters().getPictureSize();
            Log.d(TAG, "CAPTURE AT SIZE: " + size.width + "," + size.height);
        }

        // what step is this?
        mCamera.setDisplayOrientation(90);

        //mCamera.enableShutterSound(true);

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }


    public void surfaceCreated(SurfaceHolder holder) {
        initCamera(holder);
    }



    public void surfaceDestroyed(SurfaceHolder holder) {
        release();
    }


    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }


    public void onClick(View v) {
        if (v.getId() == R.id.record_button) {
            if (outputFilePath == null) {
                if (mCamera == null) {
                    initCamera(((SurfaceView) findViewById(R.id.surface_camera)).getHolder());
                    if (mCamera == null) {
                        new AlertDialog.Builder(ImageCaptureActivity.this).setTitle(getResources().getString(R.string.error)).setMessage(getResources().getString(R.string.camera_not_initializing))
                        .setCancelable(false)
                        .setNegativeButton(getResources().getString(R.string.ok),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        }).create().show();
                        return;
                    }
                }
                try {
                    mCamera.takePicture(null, null, mPictureCallback);
                } catch (Exception ex) {
                    Toast.makeText(this, R.string.error_take_picture, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error Taking Picture", ex);
                }
                if (Config.DEBUG) {
                    if (getIntent().hasExtra("AUTOMATION")) {
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                findViewById(R.id.done_button).performClick();
                            }
                        }, 2000);
                    }
                }
            } else {
                new File(outputFilePath).delete();
                findViewById(R.id.image_thumbnail).setVisibility(View.INVISIBLE);
                ((ImageView) findViewById(R.id.record_button)).setImageResource(R.drawable.ic_action_record);
                findViewById(R.id.done_button).setVisibility(View.GONE);
                outputFilePath = null;
                release();
                initCamera(((SurfaceView) findViewById(R.id.surface_camera)).getHolder());
            }
        } else if (v.getId() == R.id.done_button) {
            if (outputFilePath != null) {
                setResult(Activity.RESULT_OK, new Intent().setData(Uri.parse("file:" + outputFilePath)));
            } else {
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
        } else if (v.getId() == R.id.switch_button) {
            isFront = !isFront;
            release();
            initCamera(((SurfaceView) findViewById(R.id.surface_camera)).getHolder());
        } else if (v.getId() == R.id.image_thumbnail) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ImageDialogFragment f = ImageDialogFragment.newInstance(outputFilePath);
            f.show(ft, "dialog");
        }
    }


    PictureCallback mPictureCallback = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            AudioManager aman = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (aman.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
                if (mAudioMP != null) {
                    mAudioMP.release();
                    mAudioMP = null;
                }
                if (mAudioMP == null) {
                    mAudioMP = MediaPlayer.create(ImageCaptureActivity.this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
                }
                if (mAudioMP != null) {
                    mAudioMP.start();
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            File file = new File(MainApplication.getChatStorageDirectory(), timeStamp + ".jpg");
            outputFilePath = file.getAbsolutePath();

            try {
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                os.write(data);
                os.close();
            } catch (Exception ex) {
                Log.d(TAG, "Error Writing Picture", ex);
            }

            int angle = 0;
            try {
                ExifInterface exif = new ExifInterface(outputFilePath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                    angle = 90;
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                    angle = 180;
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                    angle = 270;
                }
                if (Config.DEBUG) {
                    Log.d(TAG, "EXIF ROTATION ANGLE: " + angle);
                }
            } catch (Exception ex) {
                Log.w(TAG, "EXIF FAILED: "  + outputFilePath);
            }

            try {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = Helper.getSampleSize(outputFilePath, 1000);
                opts.inDither = false;
                opts.inScaled = false;
                opts.inPreferredConfig = Bitmap.Config.RGB_565;

                Bitmap bmp = BitmapFactory.decodeFile(outputFilePath, opts);

                if (angle > 0) {
                    Matrix mat = new Matrix();
                    mat.postRotate(angle);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
                }

                // store again with 95 compression...
                OutputStream out = new FileOutputStream(outputFilePath);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, out);
                out.close();

                if (Config.DEBUG) {
                    Log.d(TAG, "RESAMPLED SIZE: " + bmp.getWidth() + "," + bmp.getHeight());
                }
                
                bmp.recycle();
            } catch (Exception ex) {
                Log.w(TAG, "RESAMPLE FAILED: "  + outputFilePath);
            }

            findViewById(R.id.image_thumbnail).setVisibility(View.VISIBLE);
            ((ImageView) findViewById(R.id.image_thumbnail)).setImageBitmap(MainApplication.getImageThumbnail(outputFilePath, false));
            findViewById(R.id.done_button).setVisibility(View.VISIBLE);
            ((ImageView) findViewById(R.id.record_button)).setImageResource(android.R.drawable.ic_menu_close_clear_cancel);

            handler.postDelayed(new Runnable() {
                public void run() {
                    try {
                        mCamera.startPreview();
                    } catch (Exception ex) {
                    }
                }
            }, 1000);
        }
    };


    ShutterCallback mShutterCallback = new ShutterCallback(){
        @Override
        public void onShutter() {
        }
    };

    PictureCallback mPictureCallback_RAW = new PictureCallback(){
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
        }
    };



    private void release() {
        try {
            // Step 5.c
            mCamera.lock();
        } catch (Exception ex) {
            Log.w(TAG, "CAMERA LOCK FAILED", ex);
        }
        try {
            // Step 6
            mCamera.stopPreview();
        } catch (Exception ex) {
            Log.w(TAG, "RECORDER STOP PREVIEW FAILED", ex);
        }
        try {
            // Step 7
            mCamera.release();
        } catch (Exception ex) {
            Log.w(TAG, "RECORDER RELEASE FAILED", ex);
        }
        if (mAudioMP != null) {
            try {
                mAudioMP.release();
            } catch (Exception ex) {
            }
        }
    }
}