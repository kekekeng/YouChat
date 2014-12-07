package com.acesse.youchat;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;


public class RecyclingImageView extends ImageView {


    private static final String TAG = "YOUC";

    private String key;


    public RecyclingImageView(Context context) {
        super(context);
    }


    public RecyclingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @see android.widget.ImageView#onDetachedFromWindow()
     */
    @Override
    protected void onDetachedFromWindow() {
        //Log.d(TAG, "---------------------RIV.onDetachedFromWindow");
        final Drawable drawable = getDrawable();
        if (drawable instanceof BitmapDrawable) {
//            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
//            if (bitmap != null && !bitmap.isRecycled()) {
//                bitmap.recycle();
//            }
//            if (key != null) {
//                MainApplication.removeBitmap(key);
//            }
            setImageDrawable(null);
        }

        super.onDetachedFromWindow();
    }


    @Override 
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable instanceof BitmapDrawable) {
            if (((BitmapDrawable) drawable).getBitmap() != null && !((BitmapDrawable) drawable).getBitmap().isRecycled()) {
                super.onDraw(canvas);
            }
        } else {
            super.onDraw(canvas);
        }
    }
}
