package com.zry.zicerichtext;

import android.content.Context;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;

/**
 * Created by zry on 2021/8/6.
 */
public class ZiceCustomView extends ConstraintLayout {

    private Context mContext;
    private int mViewId;

    public ZiceCustomView(Context context) {
        this(context, null);
    }

    public ZiceCustomView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZiceCustomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        LayoutInflater.from(mContext).inflate(R.layout.view_custom, this);
    }

    /**
     * 配置标识，用于自定义view标签的解析
     */
    public void setViewId(int viewId) {
        mViewId = viewId;
    }

    public int getViewId() {
        return mViewId;
    }
}
