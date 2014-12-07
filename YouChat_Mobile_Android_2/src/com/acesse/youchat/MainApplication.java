package com.acesse.youchat;

import java.io.BufferedReader;

import com.crittercism.app.Crittercism;

import java.io.File;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Application;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.LruCache;

import com.crittercism.app.Crittercism;
import com.yml.youchatlib.YouChatManager;



public class MainApplication extends Application {


    private static MainApplication instance;
    private static String TAG = "YOUC";
    private static LruCache<Integer, Bitmap> mBitmapCache;
    private static Map<Integer, String> mFileCache;
    private static int thumbnailSize;
    private static List<MessageBean> mTaskCache;
    //private UncaughtExceptionHandler defaultUEH;
    private static final int DEFAULT_CACHE_SIZE = 1024 * 1024 * 6; // 6 mb
    private static Paint bitmapPaint;
    private static Bitmap imagePlaceholderBitmap, videoPlaceholderBitmap, audioPlaceholderBitmap;



    @Override
    public void onCreate() {
        if (Config.DEBUG) {
            Log.d(TAG, "MainApplication.onCreate (SDK VERS: " + Build.VERSION.SDK_INT + ")");
        }
        super.onCreate();

        this.instance = this;

        YouChatManager.init(this);

        Crittercism.initialize(this, "53bf460e1787841f12000009");

        //defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        //Thread.setDefaultUncaughtExceptionHandler(this);

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        if (Config.DEBUG) {
            Log.d(TAG, "MAX MEMORY: " + maxMemory);
        }
        // Use the smaller: 1/10th of the available memory or 6 MB
        int cacheSize = Math.min(maxMemory / 10, DEFAULT_CACHE_SIZE);
        if (Config.DEBUG) {
            Log.d(TAG, "BITMAP CACHE SIZE: " + cacheSize);
        }
        mBitmapCache = new LruCache<Integer, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(Integer key, Bitmap bmap) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? bmap.getAllocationByteCount() : bmap.getByteCount();
            }
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Bitmap oldBitmap, Bitmap newBitmap) {
                if (Config.DEBUG) {
                    Log.d(TAG, "EVICT BITMAP: " + key);
                }                                                                                                                                                                                                                                                                                                                                         
                mFileCache.remove(key);
            }
        };

        mFileCache = new HashMap<Integer,String>();
        mTaskCache = Collections.synchronizedList(new ArrayList<MessageBean>());

        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //bitmapPaint.setMaskFilter(new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.5f, 12, 2f));

        Resources resources = getResources();
        videoPlaceholderBitmap = createRoundedBitmap(BitmapFactory.decodeResource(resources, R.drawable.video_placeholder));
        imagePlaceholderBitmap = createRoundedBitmap(BitmapFactory.decodeResource(resources, R.drawable.image_placeholder));
        audioPlaceholderBitmap = createRoundedBitmap(BitmapFactory.decodeResource(resources, R.drawable.audio_placeholder));

        thumbnailSize = resources.getDimensionPixelSize(R.dimen.chat_thumbnail_min_size) / 3;
        if (Config.DEBUG) {
            Log.d(TAG, "THUMBNAIL SIZE: " + thumbnailSize);
        }


        for (String dirName : new String[] { "contact", "chat" }) {
            File dir = new File(getFilesDir(), dirName);
            if (!dir.exists()) {
                try {
                    if (!dir.mkdir()) {
                        throw new Exception();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "OH NO, VERY BAD!!! PROBLEM CREATING APP DIRS", ex);
                }
            }
        }

    }


    @Override
    public void onTrimMemory(int level) {
        if (Config.DEBUG) {
            Log.d(TAG, "MainApplication.onTrimMemory " + level);
        }
        Crittercism.leaveBreadcrumb("MainApplication.onTrimMemory " + level);
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            instance.mBitmapCache.evictAll();
        }
        super.onTrimMemory(level);
    }


    /*
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // Using Criticism...
        String txt = "";
        try {
            PackageInfo pinfo = instance.getPackageManager().getPackageInfo(instance.getPackageName(), 0);
            txt += "Version: " + pinfo.versionName + " (" + pinfo.versionCode + ") ";
            txt += Config.DEBUG ? "DEBUG " : "";
            txt += Config.DOMAIN_XMPP.substring(0, Config.DOMAIN_XMPP.indexOf("-"));
        } catch (Exception e) {
            Log.w(TAG, "FAILED TO GET PACKAGE INFO");
        }

        txt += "\n\n--------- Device ---------\n";
        txt += "Brand: " + Build.BRAND + "\n";
        txt += "Device: " + Build.DEVICE + "\n";
        txt += "Model: " + Build.MODEL + "\n";
        txt += "SDK: " + Build.VERSION.SDK_INT + "\n";
        txt += "Release: " + Build.VERSION.RELEASE + "\n";
        txt += "\n--------- Stack Trace ---------\n";
        txt += Log.getStackTraceString(ex); 
        txt += "\n--------- Logs ---------\n";
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"logcat", "-d"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            List<String> list = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            reader.close();
            int size = list.size();
            list = size > 300 ? list.subList(size-300, size) : list;
            for (String s : list) {
                txt += s + "\n";
            }
        } catch (Exception e) {
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(instance);
        preferences.edit().putString("crash", txt).commit();
        if (Config.DEBUG) {
            Log.d(TAG, "CRASH REPORT IN PREFS\n" + txt);
        }
        // re-throw critical exception further to the os (important)
        defaultUEH.uncaughtException(thread, ex);
    }
     */


    public final static MainApplication getInstance() {
        return instance;
    }


    public final static File getChatStorageDirectory() {
        return new File(instance.getFilesDir(), "chat");
    }


    public final static File getContactStorageDirectory() {
        return new File(instance.getFilesDir(), "contact");
    }


    public final static LruCache<Integer, Bitmap> getBitmapCache() {
        return mBitmapCache;
    }


    public final static void clearCaches() {
        mFileCache.clear();
        mTaskCache.clear();
        mBitmapCache.evictAll();
    }


    public final static List<MessageBean> getTaskCache() {
        return mTaskCache;
    }


    public final static void addTask(MessageBean mbean) {
        mTaskCache.add(mbean);
    }


    public final static void removeTask(MessageBean mbean) {
        mTaskCache.remove(mbean);
    }


    public final static void addFilePath(String key, String pathName) {
        mFileCache.put(key.hashCode(), pathName);
    }

    public final static Bitmap getImagePlaceholderBitmap() {
        return imagePlaceholderBitmap;
    }

    public final static Bitmap getVideoPlaceholderBitmap() {
        return videoPlaceholderBitmap;
    }

    public final static Bitmap getAudioPlaceholderBitmap() {
        return audioPlaceholderBitmap;
    }


    public final static synchronized void addBitmap(String key, String pathName, Bitmap bmap) {
        addBitmap(key, bmap);
        addFilePath(key, pathName);
    }


    public final static synchronized void addBitmap(String key, Bitmap bmap) {
        if (key != null && bmap != null) {
            if (Config.DEBUG) {
                Log.d(TAG, "CACHE BITMAP SIZE: " + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? bmap.getAllocationByteCount() : bmap.getByteCount()));
            }                                                                                                                                                                                                                                                                                                                                         
            mBitmapCache.put(key.hashCode(), bmap);
        }
    }


    public final static void removeBitmap(String key) {
        mBitmapCache.remove(key.hashCode());
    }


    public final static Bitmap getBitmap(String key) {
        if (key != null) {
            if (Config.DEBUG) {
                Log.d(TAG, "GET BITMAP " + key);
            }
            return mBitmapCache.get(key.hashCode());
        }
        return null;
    }


    public final static String getLocalFilePath(String key) {
        return key == null ? null : mFileCache.get(key.hashCode());
    }


    public final static void removeLocalFilePath(String key) {
        if (key != null) {
            mFileCache.remove(key.hashCode());
        }
    }


    public final static void deleteContent(String url) {
        String name = url.substring(url.lastIndexOf("/")+1);
        new File(getChatStorageDirectory(), name).delete();
        removeBitmap(url);
        removeLocalFilePath(url);
    }


    public final static void restartApp() {
        Intent intent = new Intent(instance, YouChatLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("logout", true);
        if (!hasNetworkConnectivity()) {
            intent.putExtra("error", "no-network");
        } else {
            intent.putExtra("error", "other");
        }
        instance.startActivity(intent);
    }


    public final static boolean hasNetworkConnectivity(){
        ConnectivityManager cm = (ConnectivityManager) instance.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo eventInfo = cm.getActiveNetworkInfo();
        return eventInfo != null && eventInfo.getState() == NetworkInfo.State.CONNECTED;
    }


    public final static void showNetworkUnavailableDialog(FragmentManager manager) {
        DialogFragment f = (DialogFragment) manager.findFragmentByTag("network.dialog");
        if (f == null || !f.getDialog().isShowing()) {
            try {
                FragmentTransaction ft = manager.beginTransaction();
                f = NetworkAlertDialogFragment.newInstance();
                f.show(ft, "network.dialog");
            } catch (Exception ex) {
                Log.w(TAG, "Failed to show error network dialog.", ex);
            }
        }
    }


    public final static Bitmap getThumbnail(String filePath) {
        if (filePath == null) {
            return null;
        }
        if (filePath.endsWith(".jpg")) {
            return getImageThumbnail(filePath, true);
        } else if (filePath.endsWith(".mp4")) {
            return getVideoThumbnail(filePath);
        }
        return null;
    }


    public final static Bitmap getVideoThumbnail(String filePath) {
        if (filePath == null) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        Bitmap bmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try { 
            retriever.setDataSource(filePath);
            Bitmap frame = retriever.getFrameAtTime(-1);
            float factor = (float) (thumbnailSize * 2f) / (float) frame.getHeight();
            bmap = Bitmap.createScaledBitmap(frame, (int) (frame.getWidth() * factor), (int) (frame.getHeight() * factor), false);
            if (frame.getWidth() != bmap.getWidth() && frame.getHeight() != bmap.getHeight()) {
                frame.recycle();
                frame = null;
            }
        } catch (Exception ex) {
            Log.w(TAG, "VIDEO THUMBNAIL FAILED", ex);
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        if (Config.DEBUG) {
            if (bmap != null) {
                Log.d(TAG, "VID THUMBNAIL " + bmap.getWidth() + "x" + bmap.getHeight() + ", LOADED IN " + (System.currentTimeMillis() - startTime) + "ms");
            }
        }
        return bmap == null ? null : createRoundedBitmap(bmap);
    }


    public final static int[] getImageDimensions(String filePath) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        try {
            opts.inJustDecodeBounds = true;
            if (filePath.startsWith("content:")) {
                //
                // we shouldn't be here...
                //
                BitmapFactory.decodeFileDescriptor(instance.getContentResolver().openFileDescriptor(Uri.parse(filePath), "r").getFileDescriptor(), null, opts);
            } else {
                BitmapFactory.decodeFile(filePath, opts);
            }
        } catch (Exception ex) {
            Log.w(TAG, "IMAGE BOUNDS FAILED");
        }
        return new int[] { opts.outWidth, opts.outHeight };
    }


    public final static int getInSampleSize(int width, int height, int imageSize) {
        int inSampleSize = 1;
        final int halfHeight = width / 2;
        final int halfWidth = height / 2;
        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (Math.max(halfHeight / inSampleSize, halfWidth / inSampleSize) > imageSize) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }



    public final static Bitmap getImageThumbnail(String filePath, boolean scaleUpEnabled) {
        if (filePath == null) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        int inSampleSize = 1;
        Bitmap bmap = null;
        try {
            int dims[] = getImageDimensions(filePath);
            inSampleSize = getInSampleSize(dims[0], dims[1], thumbnailSize);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = inSampleSize; 
            opts.inDither = false;
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            if (filePath.startsWith("content:")) {
                //
                // we shouldn't be here...
                //
                bmap = BitmapFactory.decodeFileDescriptor(instance.getContentResolver().openFileDescriptor(Uri.parse(filePath), "r").getFileDescriptor(), null, opts);
            } else {
                bmap = BitmapFactory.decodeFile(filePath, opts);
            }
            if (scaleUpEnabled && inSampleSize == 1 && bmap.getHeight() < thumbnailSize) {
                // scale up to thumbnail size...
                float factor = (float) thumbnailSize / (float) bmap.getHeight();
                bmap = Bitmap.createScaledBitmap(bmap, (int) (bmap.getWidth() * factor), (int) (bmap.getHeight() * factor), false);
            }
        } catch (Exception ex) {
            Log.w(TAG, "IMAGE THUMBNAIL FAILED");
        }
        if (Config.DEBUG) {
            if (bmap != null) {
                Log.d(TAG, "IMG THUMBNAIL " + bmap.getWidth() + "x" + bmap.getHeight() + ", LOADED IN " + (System.currentTimeMillis() - startTime) + "ms, INSAMPLESIZE: " + inSampleSize + " " );
            }
        }
        return bmap == null ? null : createRoundedBitmap(bmap);
    }


    public static Bitmap createRoundedBitmap(Bitmap bmap) {
        float rxy = Math.round(instance.getResources().getDisplayMetrics().density * 4);
        Bitmap output = Bitmap.createBitmap(bmap.getWidth(), bmap.getHeight(), Bitmap.Config.ARGB_8888);
        bitmapPaint.setShader(new BitmapShader(bmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        Canvas canvas = new Canvas(output);
        canvas.drawRoundRect(new RectF(0.0f, 0.0f, output.getWidth(), output.getHeight()), rxy, rxy, bitmapPaint);
        bmap.recycle();
        return output;
    }


    public final static int[] getVideoDimensions(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(filePath);
            String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            return new int[] { Integer.parseInt(width), Integer.parseInt(height) };
        } catch (Exception ex) {
            Log.w(TAG, "MEDIA METADATA RETRIEVER FAILED " + filePath);
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        return new int[] { 0, 0 };
    }


    public final static String getMediaDuration(String filePath) {
        if (filePath == null) {
            return null;
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (filePath.startsWith("content:/")) {
                retriever.setDataSource(instance, Uri.parse(filePath));
            } else {
                retriever.setDataSource(filePath);
            }
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            Log.d(TAG, "MEDIA DURATION: " + Double.parseDouble(duration)/1000 );
            if (duration != null && !duration.isEmpty()) {
                int time = Integer.parseInt(duration);
                int minutes = time / (60 * 1000);
                int seconds = (time / 1000) % 60;
                return String.format("%d:%02d", minutes, seconds);
            }
        } catch (Exception ex) {
            Log.w(TAG, "MEDIA METADATA RETRIEVER FAILED " + filePath);
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
            }
        }
        return null;
    }


    public final static void registerLocalReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        LocalBroadcastManager.getInstance(instance).registerReceiver(receiver, filter);
    }


    public final static void unregisterLocalReceiver(BroadcastReceiver receiver) {
        try {
            LocalBroadcastManager.getInstance(instance).unregisterReceiver(receiver);
        } catch (Exception ex) {
        }
    }


    public final static void sendLocalBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(instance).sendBroadcast(intent);
    }

}