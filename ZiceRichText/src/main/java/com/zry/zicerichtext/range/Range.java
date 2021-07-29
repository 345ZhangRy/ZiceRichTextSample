package com.zry.zicerichtext.range;

import android.support.annotation.NonNull;

public class Range implements Comparable<Range> {

    private int mFrom;
    private int mTo;

    public Range(int from, int to) {
        this.mFrom = from;
        this.mTo = to;
    }

    //range是子集
    public boolean isWrapped(int start, int end) {
        return mFrom >= start && mTo <= end;
    }

    //有交集
    public boolean isWrappedBy(int start, int end) {
        return (start > mFrom && start < mTo) || (end > mFrom && end < mTo);
    }

    //是range的子集
    public boolean contains(int start, int end) {
        return mFrom <= start && mTo >= end;
    }

    //完全相等
    public boolean isEqual(int start, int end) {
        return (mFrom == start && mTo == end) || (mFrom == end && mTo == start);
    }

    //得到range的头或尾位置，设置锚点，防止焦点落在range中间
    public int getAnchorPosition(int value) {
        if ((value - mFrom) - (mTo - value) >= 0) {
            return mTo;
        } else {
            return mFrom;
        }
    }

    public void setOffset(int offset) {
        mFrom += offset;
        mTo += offset;
    }

    @Override
    public int compareTo(@NonNull Range o) {
        return mFrom - o.mFrom;
    }

    public int getFrom() {
        return mFrom;
    }

    public void setFrom(int from) {
        this.mFrom = from;
    }

    public int getTo() {
        return mTo;
    }

    public void setTo(int to) {
        this.mTo = to;
    }

}

