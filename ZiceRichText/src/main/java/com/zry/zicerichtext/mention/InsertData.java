package com.zry.zicerichtext.mention;


import com.zry.zicerichtext.range.FormatRange;

public interface InsertData {

  CharSequence charSequence();

  FormatRange.FormatData formatData();

  int color();
}
