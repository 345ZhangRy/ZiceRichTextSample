package com.zry.zicerichtext.mention;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;

import com.zry.zicerichtext.MentionEditText;
import com.zry.zicerichtext.range.RangeManager;
import com.zry.zicerichtext.range.Range;

import java.util.Iterator;

public class MentionTextWatcher implements TextWatcher {
  private final MentionEditText mEditText;
  private final RangeManager mRangeManager;

  public MentionTextWatcher(MentionEditText editText) {
    this.mEditText = editText;
    this.mRangeManager = mEditText.getRangeManager();
  }

  //If you insert characters from the entire string, you need to shift the range after the insertion position accordingly.
  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    Editable editable = mEditText.getText();
    //Adding at the end does not need to be processed.
    if (start < editable.length()) {
      int end = start + count;
      int offset = after - count;

      //Clean up the span between start and start + count
      //If range.from = 0, it will also be obtained by getSpans(0,0,ForegroundColorSpan.class)
      if (start != end && !mRangeManager.isEmpty()) {
        ForegroundColorSpan[] spans = editable.getSpans(start, end, ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
          editable.removeSpan(span);
        }
      }

      //Clean up the range that has been cleared above in the arraylist
      //Move the span after the end to the offset position
      Iterator iterator = mRangeManager.iterator();
      while (iterator.hasNext()) {
        Range range = (Range) iterator.next();
        if (range.isWrapped(start, end)) {
          iterator.remove();
          continue;
        }

        if (range.getFrom() >= end) {
          range.setOffset(offset);
        }
      }
    }
  }

  @Override
  public void onTextChanged(CharSequence charSequence, int index, int i1, int count) {
  }

  @Override
  public void afterTextChanged(Editable editable) {
  }
}
