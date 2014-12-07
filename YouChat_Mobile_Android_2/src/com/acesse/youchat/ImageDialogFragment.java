package com.acesse.youchat;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;


public class ImageDialogFragment extends DialogFragment {

    private static final String TAG = "YOUC";

    private String filePath;
    private Bitmap bmap;


    static ImageDialogFragment newInstance(String filePath) {
        ImageDialogFragment f = new ImageDialogFragment();
        f.filePath = filePath;
        return f;
    }

    public ImageDialogFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            if (Config.DEBUG) {
                Log.d(TAG, "DECODE FILE " + filePath);
            }

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            if (filePath.startsWith("content:")) {
                BitmapFactory.decodeFileDescriptor(getActivity().getContentResolver().openFileDescriptor(Uri.parse(filePath), "r").getFileDescriptor(), null, opts);
            } else {
                BitmapFactory.decodeFile(filePath, opts);
            }

            if (opts.outWidth > 0 && opts.outHeight > 0) {
                int inSampleSize = 1;
                int imageSize = 800;
                if (opts.outHeight > imageSize || opts.outWidth > imageSize) {
                    final int halfHeight = opts.outHeight / 2;
                    final int halfWidth = opts.outWidth / 2;
                    // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                    // height and width larger than the requested height and width.
                    while ((halfHeight / inSampleSize) > imageSize && (halfWidth / inSampleSize) > imageSize) {
                        inSampleSize *= 2;
                    }
                }
                opts.inJustDecodeBounds = false;
                opts.inSampleSize = inSampleSize;
                if (filePath.startsWith("content:")) {
                    bmap = BitmapFactory.decodeFileDescriptor(getActivity().getContentResolver().openFileDescriptor(Uri.parse(filePath), "r").getFileDescriptor(), null, opts);
                } else {
                    bmap = BitmapFactory.decodeFile(filePath, opts);
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Bitmap decoding failed.", ex);
        }
        ImageView iview = (ImageView) inflater.inflate(R.layout.chat_image, container);
        if (bmap != null) {
            iview.setImageBitmap(bmap);
        }
        return iview;
    }
    
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bmap != null && !bmap.isRecycled()) {
            bmap.recycle();
        }
    }
}