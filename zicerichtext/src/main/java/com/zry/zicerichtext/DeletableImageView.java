package com.zry.zicerichtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

public class DeletableImageView extends android.support.v7.widget.AppCompatImageView {

    private boolean showBorder = false; //Whether to display the border
    private int borderColor = Color.GRAY;//Border color
    private int borderWidth = 5;//Border size

    private String absolutePath;
    private Bitmap bitmap;

    public DeletableImageView(Context context) {
        super(context);
    }

    public DeletableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DeletableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (showBorder) {
            //Paint border
            Rect rec = canvas.getClipBounds();
            //These two sentences can make the bottom and right side borders larger
            //rec.bottom -= 2;
            //rec.right -= 2;
            Paint paint = new Paint();
            paint.setColor(borderColor);//Set color
            paint.setStrokeWidth(borderWidth);//Set the width of the brush
            paint.setStyle(Paint.Style.STROKE);//Set the style of the brush - can't be set to fill FILL or you can't see the picture
            canvas.drawRect(rec, paint);
        }
    }

}
