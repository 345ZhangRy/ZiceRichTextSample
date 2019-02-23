package com.zry.zicerichtext.mentions.library.edit.listener;


import com.zry.zicerichtext.mentions.library.model.FormatRange;

public interface InsertData {

  CharSequence charSequence();

  FormatRange.FormatData formatData();

  int color();
}
