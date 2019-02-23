package com.zry.zicerichtext.mentions.library.edit.util;

import com.zry.zicerichtext.mentions.library.model.Range;

import java.util.ArrayList;
import java.util.Iterator;

public class RangeManager {

  private ArrayList<Range> mRanges;

  public RangeManager() {
    mRanges = new ArrayList<>();
  }

  public ArrayList<? extends Range> get() {
    ensureListNonNull();
    return mRanges;
  }

  public <T extends Range>void add(T range) {
    ensureListNonNull();
    mRanges.add(range);
  }

  public void clear() {
    ensureListNonNull();
    mRanges.clear();
  }

  public boolean isEmpty() {
    ensureListNonNull();
    return mRanges.isEmpty();
  }

  public Iterator<? extends Range> iterator(){
    ensureListNonNull();
    return mRanges.iterator();
  }

  private void ensureListNonNull() {
    if (null == mRanges) {
      mRanges = new ArrayList<>();
    }
  }
  public Range getRangeOfClosestMentionString(int selStart, int selEnd) {
    if (mRanges == null) {
      return null;
    }
    for (Range range : mRanges) {
      if (range.contains(selStart, selEnd)) {
        return range;
      }
    }
    return null;
  }

  public Range getRangeOfNearbyMentionString(int selStart, int selEnd) {
    if (mRanges == null) {
      return null;
    }
    for (Range range : mRanges) {
      if (range.isWrappedBy(selStart, selEnd)) {
        return range;
      }
    }
    return null;
  }
  ////////////
  private Range mLastSelectedRange;

  public boolean isEqual(int selStart, int selEnd) {
    return null != mLastSelectedRange && mLastSelectedRange.isEqual(selStart, selEnd);
  }

  public void setLastSelectedRange(Range lastSelectedRange) {
    mLastSelectedRange = lastSelectedRange;
  }

}
