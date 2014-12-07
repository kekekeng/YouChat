package com.acesse.youchat;

import java.text.SimpleDateFormat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;



public final class Helper {

    private static final String TAG = "YOUC";

    public static void createAlertDialog(Context context, String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if(title != null && title.length() > 0)
            builder.setTitle(title);
        if(message != null && message.length() > 0)
        {
            builder.setMessage(message);
            builder.setPositiveButton(context.getResources().getString(R.string.ok), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.create().show();
        }
        else
            return;
    }

    public static void hideSoftKeyboard(Context context, IBinder token)
    {
        if(context != null)
        {
            InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, 0);
        }
    }

    public static boolean isSoftKeyboardVisible(Context context)
    {
        if(context != null)
        {
            InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            return manager.isActive();
        }
        else
            return false;
    }


    /**
     * Rotate the bitmap using Exif
     * 
     * @param uri
     * @param bitmap
     * @return Bitmap
     */
    public static Bitmap rotateImage(Uri uri, Bitmap bitmap) { 
        try {
            String filePath = null;
            if (Config.DEBUG) {
                Log.d(TAG, "Helper.rotateImageToPortrait URI: " + uri);
            }
            Cursor cursor = MainApplication.getInstance().getContentResolver().query(uri, new String[] { MediaStore.Images.Media.DATA }, null, null, null);
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                filePath = cursor.getString(0);
                Log.d(TAG, "Helper.rotateImageToPortrait " + filePath);
            } else {
                Log.w(TAG, "UNEXPECTED CURSOR COUNT: " + cursor.getCount());
            }
            if (filePath != null) {
                return rotateImage(filePath, bitmap);
            }
        } catch (Exception ex) {
            Log.w(TAG, "Unable to rotate image");
        }
        return bitmap;
    }


    /**
     * Rotate the bitmap using Exif
     * 
     * @param pathName
     * @param bitmap
     * @return Bitmap
     * @throws Exception
     */
    public static Bitmap rotateImage(String pathName, Bitmap bitmap) {
        try {
            ExifInterface exif = new ExifInterface(pathName);
            int pictureOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            if (Config.DEBUG) {
                Log.d(TAG,"Helper.rotateImage ORIENTATION " + pictureOrientation);
            }
            Matrix matrix = new Matrix();
            if (pictureOrientation == 6) {
                matrix.postRotate(90);
            } else if (pictureOrientation == 3) {
                matrix.postRotate(180);
            } else if (pictureOrientation == 8) {
                matrix.postRotate(270);
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception ex) {
            Log.w(TAG, "Unable to rotate image");
        }
        return bitmap;
    }


    public static synchronized void updateTimeStamp(final TextView dateText, long time) {
        if (dateText == null) {
            return;
        }
        if (time == 0) {
            dateText.setText("");
            return;
        }
        String prevText = dateText.getText().toString();
        String currText = "";
        long diff = System.currentTimeMillis() - time;
        boolean animate = false;
        if (diff < 30000L) {
            currText = "Now";
        } else if (diff < 60000L) {
            long diffSeconds = diff / 1000L % 60L;
            currText = Math.max(1,diffSeconds) + " secs ago";
            animate = true;
        } else if (diff < 3600000L) {
            long diffMinutes = diff / (60L * 1000L) % 60L;
            currText = diffMinutes + " min" + (diffMinutes > 1 ? "s" : "") + " ago";
            int idx = prevText.indexOf(" ");
            try {
                animate = idx != -1 && Long.parseLong(prevText.substring(0, idx)) != diffMinutes;
            } catch (Exception ex) {
            }
        } else if (diff < 43200000L) { 
            // 12 hours
            long diffHours = diff / (60L * 60L * 1000L);
            currText = diffHours + " hour" + (diffHours > 1 ? "s" : "") + " ago";
            int idx = prevText.indexOf(" ");
            try {
                animate = idx != -1 && Long.parseLong(prevText.substring(0, idx)) != diffHours;
            } catch (Exception ex) {
            }
        } else {
            currText = new SimpleDateFormat("MMM, dd h:mm a").format(time);
            /*
            int diffInDays = (int) ((diff) / (1000L * 60L * 60L * 24L));
            if (diffInDays < 4) {
                currText = diffInDays + " day" + (diffInDays > 1 ? "s" : "") + " ago";
            } else {
                currText = new SimpleDateFormat("MMM, dd h:mm a").format(time);
            }
            */
        }
        //if (Config.DEBUG) {
            //Log.d(TAG, "PREVIOUS TEXT: " + prevText + " CURRENT TEXT: " + currText + " TIME: " + new SimpleDateFormat("HH:mm:ss").format(time) + " DIFF: " + diff);
        //}
        if (animate) {
            final String text = currText;
            dateText.animate().alpha(0).setDuration(100).setListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    dateText.setText(text);
                    dateText.animate().alpha(1f).setDuration(100).setListener(null);
                }
            });
        } else {
            dateText.setText(currText);
        }
    }


    public static final int getSampleSize(String filePath, int imageSize) throws Exception {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opts);
        if (Config.DEBUG) {
            Log.d(TAG, "SIZE: " + opts.outWidth + "x" + opts.outHeight);
        }
        if (opts.outWidth == 0 || opts.outHeight == 0) {
            throw new Exception("Error decoding image. Width or Height equal 0.");
        }
        return MainApplication.getInSampleSize(opts.outWidth, opts.outHeight, imageSize);
    }
}
