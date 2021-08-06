package com.zry.zicerichtext;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

public class ZiceImageView extends androidx.appcompat.widget.AppCompatImageView {

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
