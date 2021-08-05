package com.zry.zicerichtext;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

public class ZiceImageView extends android.support.v7.widget.AppCompatImageView {

    private String mAbsolutePath;

    public ZiceImageView(Context context) {
        super(context);
    }

    public ZiceImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ZiceImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public String getAbsolutePath() {
        return mAbsolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.mAbsolutePath = absolutePath;
    }

}
