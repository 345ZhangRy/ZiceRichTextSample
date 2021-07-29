package com.zry.zicerichtext.mention.util;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import com.zry.zicerichtext.MentionEditText;
import com.zry.zicerichtext.range.RangeManager;
import com.zry.zicerichtext.range.Range;

public class MentionInputConnection extends InputConnectionWrapper {
    private final MentionEditText mEditText;
    private final RangeManager mRangeManager;

    public MentionInputConnection(InputConnection target, boolean mutable, MentionEditText editText) {
        super(target, mutable);
        this.mEditText = editText;
        this.mRangeManager = editText.getRangeManager();
    }

    /**
     * 当在软件盘上点击某些按钮（比如退格键，数字键，回车键等），该方法可能会被触发（取决于输入法的开发者），
     * 所以也可以重写该方法并拦截这些事件，这些事件就不会被分发到输入框了
     * <p>
     * 这里判断这次删除行为有没有range，如果有，根据range的from/to判断这次删除行为应该选中还是真正删除
     */
    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {//按下删除
            if (null != mRangeManager) {
                //edittext选中部分的开始和结束坐标，如果没多选，只是光标闪烁，start和end相同
                int selectionStart = mEditText.getSelectionStart();
                int selectionEnd = mEditText.getSelectionEnd();
                Range closestRange =
                        mRangeManager.getRangeOfClosestMentionString(selectionStart, selectionEnd);
                if (closestRange == null) {
                    mEditText.setSelected(false);
                    return super.sendKeyEvent(event);
                }
                //if mention string has been selected or the cursor is at the beginning of mention string, just use default action(delete)
                if (mEditText.isSelected() || selectionStart == closestRange.getFrom()) {
                    mEditText.setSelected(false);
                    return super.sendKeyEvent(event);
                } else {
                    //select the mention string
                    mEditText.setSelected(true);
                    mRangeManager.setLastSelectedRange(closestRange);
                    setSelection(closestRange.getTo(), closestRange.getFrom());
                }
                return true;
            }
        }
        return super.sendKeyEvent(event);
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        if (beforeLength == 1 && afterLength == 0) {
            return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)) && sendKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        }
        return super.deleteSurroundingText(beforeLength, afterLength);
    }
}
