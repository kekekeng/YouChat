package com.acesse.youchat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.crittercism.app.Crittercism;


public class DownloadMediaAsyncTask extends AsyncTask<MessageBean, Integer, Object> {

    private static final String TAG = "YOUC";
    private MessageBean mbean;


    protected Object doInBackground(MessageBean... args) {

        mbean = args[0];
        MainApplication.addTask(mbean);

        HttpURLConnection conn = null;
        InputStream is = null;
        try {

            URL url = new URL(mbean.externalBodyUrl); 
            if (Config.DEBUG) {
                Log.d(TAG, "DOWNLOAD: " + url);
            }

            String name = mbean.externalBodyUrl.substring(mbean.externalBodyUrl.lastIndexOf("/")+1);
            File file = new File(MainApplication.getChatStorageDirectory(), name);
            if (Config.DEBUG) {
                Log.d(TAG, "SAVE TO: " + file);
            }

            long startTime = System.currentTimeMillis();

            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(20000); 

            mbean.downloadSize = conn.getContentLength();
            float size = mbean.downloadSize;
            if (Config.DEBUG) {
                Log.d(TAG, "NUM BYTES: " + size);
            }

            is = new BufferedInputStream(conn.getInputStream());

            OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            byte[] data = new byte[1024 * 8];
            int bytesRead = 0;
            int bytesRcvd = 0;
            int prevProgress = 0;
            while ((bytesRead = is.read(data)) > 0) {
                os.write(data, 0, bytesRead);
                bytesRcvd += bytesRead;
                int progress = (int) ((float) bytesRcvd / size * 100f);
                if (progress != prevProgress) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "PROGRESS: " + progress + " RCVD: " + bytesRcvd + " " + size);
                    }
                    publishProgress(prevProgress = progress);
                    try {
                        // allow ui to update and don't starve network pipe...
                        Thread.sleep(2);
                    } catch (Exception ex) {
                    }
                }
            }
            os.flush();
            os.close();

            mbean.downloadDuration = System.currentTimeMillis() - startTime;
            if (Config.DEBUG) {
                Log.d(TAG, "DOWNLOAD TIME: " + mbean.downloadDuration + "ms");
            }

            mbean.localPath = file.getAbsolutePath();

            if (name.endsWith(".jpg")) {


                //
                // ensure proper rotation...
                //
                try {
                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    int angle = 0;
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                        angle = 90;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                        angle = 180;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        angle = 270;
                    }

                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = Helper.getSampleSize(file.getAbsolutePath(), 800);
                    opts.inDither = false;
                    opts.inScaled = false;
                    opts.inPreferredConfig = Bitmap.Config.RGB_565;

                    if (angle > 0) {
                        if (Config.DEBUG) {
                            Log.d(TAG, "APPLYING ROTATION ANGLE: " + angle);
                        }

                        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);

                        Matrix mat = new Matrix();
                        mat.postRotate(angle);
                        Bitmap rotatedBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);

                        // store again...
                        OutputStream out = new FileOutputStream(file);
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                        out.close();

                        bmp.recycle();
                        rotatedBitmap.recycle();

                    } else if (opts.inSampleSize > 1) {

                        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);

                        // store again...
                        OutputStream out = new FileOutputStream(file);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 95, out);
                        out.close();

                        bmp.recycle();
                    }
                } catch (Exception ex) {
                    Log.d(TAG, "EXIF FAILED: "  + file);
                }

            } else if (name.endsWith(".mp4") || name.endsWith(".aac")) {
                mbean.duration = MainApplication.getMediaDuration(mbean.localPath);
            }

            return "SUCCESS";

        } catch (IOException e) {
            Crittercism.logHandledException(e);
            //
            // set localPath to empty so we don't automatically try downloading again...
            //
            mbean.localPath = "";  
            Log.w(TAG, "ERROR DOWNLOADING",  e);
            return e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
            mbean.isDownloading = false;
            MainApplication.removeTask(mbean);
            MainApplication.sendLocalBroadcast(new Intent("MESSAGE.UPDATE").putExtra("cmd", "download").putExtra("time", mbean.time).putExtra("id", mbean.id));
        }
    }


    @Override
    protected void onPostExecute(Object result) {
        if (result instanceof Exception) {
            Toast.makeText(MainApplication.getInstance(), R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onProgressUpdate(Integer... progress) {
        //Log.d(TAG, "PROGRESS: " + progress[0]);
        if (mbean.progressBar != null) {
            mbean.progressBar.setProgress(progress[0]);
        }
    }
}
