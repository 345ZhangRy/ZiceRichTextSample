package com.zry.zicerichtextsample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.zry.zicerichtext.ZiceCustomView;

/**
 * Created by zry on 2021/8/6.
 */
public class CustomView extends ZiceCustomView {

    private Context mContext;

    public CustomView(Context context) {
        this(context, null);
    }

    public CustomView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        LayoutInflater.from(mContext).inflate(R.layout.view_custom, null);
    }

}
