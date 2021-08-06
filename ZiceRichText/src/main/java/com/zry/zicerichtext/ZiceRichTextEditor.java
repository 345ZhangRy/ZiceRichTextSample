package com.zry.zicerichtext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.zry.zicerichtext.mention.model.Link;
import com.zry.zicerichtext.mention.model.User;
import com.zry.zicerichtext.range.FormatRange;
import com.zry.zicerichtext.range.Range;
import com.zry.zicerichtext.utils.GlideApp;
import com.zry.zicerichtext.utils.ZiceUtils;

import java.util.ArrayList;
import java.util.List;

public class ZiceRichTextEditor extends ScrollView {

    private static final int LAYOUT_PADDING_TB = 15; // mAllLayout的上下padding
    private static final int LAYOUT_PADDING_LR = 50; // mAllLayout的左右padding
    private static final int EDIT_PADDING_TB = 10; // edittext的上下padding
    private static final int EDIT_PADDING_LR = 0; // edittext的左右padding

    private OnKeyListener mKeyListener; // SoftText Listener for all EditText
    private OnFocusChangeListener mFocusListener; // All EditText focus listeners listener

    private Context mContext;
    private LayoutInflater mInflater;
    private int mViewTagIndex; // The newer view will have a tag, which is unique for each view.
    private LinearLayout mAllLayout; // This is the container for all child views, the only one inside the scrollView.
    private MentionEditText mLastFocusEdit; // Recently focused EditText
    StringBuilder mUpdateText = new StringBuilder();  //Text to be uploaded
    private ArrayList<String> mImagePaths;   //Image address collection

    /**
     * Custom attribute
     **/
    //Text related attributes, initial prompt information, text size and color
    private String mZrtTextInitHint;
    private int mZrtTextSize;
    private int mZrtTextColor;
    private int mZrtMentionColor;
    private int mZrtLinkColor;
    private int mZrtTextLineSpace;

    public ZiceRichTextEditor(Context context) {
        this(context, null);
    }

    public ZiceRichTextEditor(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZiceRichTextEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mContext = context;

        //Get custom attributes
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ZiceRichTextEditor);
        mZrtTextSize = ta.getDimensionPixelSize(R.styleable.ZiceRichTextEditor_rt_editor_text_size, 16);
        mZrtTextLineSpace = ta.getDimensionPixelSize(R.styleable.ZiceRichTextEditor_rt_editor_text_line_space, 8);
        mZrtTextColor = ta.getColor(R.styleable.ZiceRichTextEditor_rt_editor_text_color, Color.parseColor("#757575"));
        mZrtMentionColor = ta.getColor(R.styleable.ZiceRichTextEditor_rt_editor_mention_color, Color.parseColor("#ff42ca6e"));
        mZrtLinkColor = ta.getColor(R.styleable.ZiceRichTextEditor_rt_editor_link_color, Color.parseColor("#ff14326e"));
        mZrtTextInitHint = ta.getString(R.styleable.ZiceRichTextEditor_rt_editor_text_init_hint);

        ta.recycle();

        mImagePaths = new ArrayList<>();

        mInflater = LayoutInflater.from(context);

        //1. Initialize allLayout
        initAllLayout();

        // 2. Initialize keyboard backspace monitoring
        initKeyBoardBackPress();

        //3、Focus change monitor
        initFocusChangeListener();

        //4、Add the first view
        addFirstView();

    }

    private void initAllLayout() {
        mAllLayout = new LinearLayout(mContext);
        mAllLayout.setOrientation(LinearLayout.VERTICAL);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        //设置间距，防止生成图片时文字太靠边，不能用margin，否则有黑边
        mAllLayout.setPadding(LAYOUT_PADDING_LR, LAYOUT_PADDING_TB, LAYOUT_PADDING_LR, LAYOUT_PADDING_TB);
        addView(mAllLayout, layoutParams);
    }

    private void initKeyBoardBackPress() {
        // Mainly used to handle the click-to-delete button, some column merge operations of the view
        mKeyListener = new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    MentionEditText edit = (MentionEditText) v;
                    onBackspacePress(edit);
                }
                return false;
            }
        };
    }

    /**
     * Handling the soft keyboard backSpace rollback event
     *
     * @param editTxt The text input box where the cursor is located
     */
    private void onBackspacePress(MentionEditText editTxt) {
        int startSelection = editTxt.getSelectionStart();
        // Only when the cursor has been topped to the forefront of the text input box, in the decision whether to delete the previous picture, or two View merge
        if (startSelection == 0) {
            int curIndex = mAllLayout.indexOfChild(editTxt);
            View preView = mAllLayout.getChildAt(curIndex - 1); // If editIndex-1<0,
            // Then returns null
            if (null != preView) {
                mergeEditText(curIndex);
            }
        }
    }

    private void initFocusChangeListener() {
        mFocusListener = new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mLastFocusEdit = (MentionEditText) v;
                }
            }
        };
    }

    private void addFirstView() {
        LinearLayout.LayoutParams firstEditParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        MentionEditText firstEdit = createEditText(mZrtTextInitHint);
        mAllLayout.addView(firstEdit, firstEditParam);
        mLastFocusEdit = firstEdit;
    }

    /**
     * Generate text input box
     */
    public MentionEditText createEditText(String hint) {
        MentionEditText editText = (MentionEditText) mInflater.inflate(R.layout.zice_rich_edittext, null);
        editText.setOnKeyListener(mKeyListener);
        editText.setTag(mViewTagIndex++);
        editText.setPadding(EDIT_PADDING_LR, EDIT_PADDING_TB, EDIT_PADDING_LR, EDIT_PADDING_TB);
        editText.setHint(hint);
        editText.setTextColor(mZrtTextColor);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mZrtTextSize);
        editText.setLineSpacing(mZrtTextLineSpace, 1.0f);
        editText.setOnFocusChangeListener(mFocusListener);
        return editText;
    }

    /**
     * Insert a @somebody
     *
     * @param user user
     */
    public void insertMention(User user) {
        user.setColor(mZrtMentionColor);
        mLastFocusEdit.insert(user);
    }

    /**
     * Insert a link
     *
     * @param link link
     */
    public void insertLink(Link link) {
        link.setColor(mZrtLinkColor);
        mLastFocusEdit.insert(link);
    }

    /**
     * Insert a picture
     *
     * @param imagePath
     */
    public void insertImage(final String imagePath, int width) {
        //If it is a web image
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            Glide.with(mContext).asBitmap().load(imagePath).into(new SimpleTarget<Bitmap>(1000, 1000) {
                @Override
                public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                    insertImageToEditor(resource, imagePath);
                }
            });
        } else {
            Bitmap bmp = ZiceUtils.getScaledBitmap(imagePath, width);
            insertImageToEditor(bmp, imagePath);
        }
    }

    /**
     * Insert a custom view
     *
     * @param view
     */
    public void insertCustomView(View view) {
        insertToEditor(false, null, null, view);
    }

    /**
     * Processing after inserting the image
     */
    private void insertImageToEditor(Bitmap bitmap, String imagePath) {
        insertToEditor(true, bitmap, imagePath, null);
    }

    /**
     * Processing after inserting the image
     */
    public void insertToEditor(boolean isDefaultImage, Bitmap bitmap, String imagePath, View customView) {
        //editStr gets the focus of the EditText
        String editStr = mLastFocusEdit.getText().toString();
        //Get the location of the cursor
        int cursorIndex = mLastFocusEdit.getSelectionStart();
        List<Integer> rangeFromIndexList = new ArrayList<>();
        List<Integer> rangeToIndexList = new ArrayList<>();
        ArrayList<? extends Range> ranges = mLastFocusEdit.getRangeManager().get();
        List<Range> formatRanges = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            formatRanges.add(range);
            rangeFromIndexList.add(range.getFrom());
            rangeToIndexList.add(range.getTo());
        }

        String editStrBefore = editStr.substring(0, cursorIndex).trim();//Get the string in front of the cursor
        String editStrAfter = editStr.substring(cursorIndex).trim();//Get the string after the cursor

        int lastEditIndex = mAllLayout.indexOfChild(mLastFocusEdit);//Get the location of the EditText in focus

        if (editStr.length() == 0) {
            //如果当前获取焦点的EditText为空，直接在EditText下方插入view，并且插入空的EditText
            addEditTextAtIndex(lastEditIndex + 1, "");
            if (isDefaultImage) {
                addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
            } else {
                addCustomViewAdIndex(lastEditIndex + 1, customView);
            }
        } else if (editStrBefore.length() == 0) {
            //如果光标已经顶在了editText的最前面，则直接插入view，并且EditText下移即可
            if (isDefaultImage) {
                addImageViewAtIndex(lastEditIndex, bitmap, imagePath);
            } else {
                addCustomViewAdIndex(lastEditIndex, customView);
            }
            //同时插入一个空的EditText，防止插入多张图片无法写文字
            addEditTextAtIndex(lastEditIndex + 1, "");
        } else if (editStrAfter.length() == 0) {
            // 如果光标已经顶在了editText的最末端，则需要添加新的view和EditText
            addEditTextAtIndex(lastEditIndex + 1, "");
            if (isDefaultImage) {
                addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
            } else {
                addCustomViewAdIndex(lastEditIndex + 1, customView);
            }
        } else {
            //如果光标已经顶在了editText的最中间，则需要分割字符串，分割成两个EditText，并在两个EditText中间插入图片
            // Clear the cursor before the EditText
            mLastFocusEdit.clear();
            Editable editable = mLastFocusEdit.getText();
            //把光标前面的字符串保留，设置给当前获得焦点的EditText（此为分割出来的第一个EditText）
            List<Integer> usedIndexList = new ArrayList<>();
            for (int i = 0; i < rangeFromIndexList.size(); i++) {
                Integer from = rangeFromIndexList.get(i);
                Integer to = rangeToIndexList.get(i);
                if (to <= cursorIndex) {    //range在焦点前
                    usedIndexList.add(i);
                    //Explain that there is this user before the cursor
                    editable.append(editStr.substring(editable.length(), from));
                    FormatRange range = (FormatRange) formatRanges.get(i);
                    CharSequence rangeCharSequence = range.getRangeCharSequence();
                    User user = new User(range.getConvert().formatParam(),
                            rangeCharSequence.subSequence(1, rangeCharSequence.length()));
                    insertMention(user);
                } else {    //焦点前面最后一个range和焦点之间的文字append
                    editable.append(editStr.substring(editable.length(), cursorIndex));
                }
            }
            editable.append(editStrBefore.substring(editable.length()));

            //把光标后面的字符串放在新创建的EditText中（此为分割出来的第二个EditText）
            MentionEditText editTextAfter = createEditText("");
            Editable editableAfter = editTextAfter.getText();
            for (int i = 0; i < rangeFromIndexList.size(); i++) {
                if (usedIndexList.contains(i)) {
                    continue;
                }
                Integer from = rangeFromIndexList.get(i);
                Integer to = rangeToIndexList.get(i);
                int afterCount = rangeFromIndexList.size() - usedIndexList.size();
                if (afterCount == 0) {
                    editableAfter.append(editStrAfter);
                    break;
                } else {
                    if (from >= cursorIndex) {
                        // Explain that there is this user after cursor
                        int beginIndex = cursorIndex + editableAfter.length();
                        editableAfter.append(editStr.substring(beginIndex, from));//append焦点后面的第一个range 的前面的文字
                        FormatRange range = (FormatRange) formatRanges.get(i);
                        CharSequence rangeCharSequence = range.getRangeCharSequence();
                        User user = new User(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()));
                        editTextAfter.insert(user);
                        if (i == rangeFromIndexList.size() - 1) {   //如果是最后一个range了，把后面的文字都append
                            editableAfter.append(editStr.substring(to));
                        }
                    }
                }
            }
            if (editableAfter.length() == 0) {  //一个range都没有，就把文字加上
                editableAfter.append(editStrAfter);
            }

            editTextAfter.setOnFocusChangeListener(mFocusListener);
            //Please note that EditText adds, or deletes, does not touch Transition animation.
            mAllLayout.setLayoutTransition(null);
            mAllLayout.addView(editTextAfter, lastEditIndex + 1);
            //After inserting a new EditText, modify the pointing of lastFocusEdit
            mLastFocusEdit = editTextAfter;
            mLastFocusEdit.requestFocus();
            //Insert an empty EditText at the position of the second EditText so that when multiple images are inserted consecutively, there is space to write the text, and the second EditText is moved down.
            addEditTextAtIndex(lastEditIndex + 1, "");
            //Insert the layout in the position of the empty EditText, move the empty EditText down
            if (isDefaultImage) {
                addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
            } else {
                addCustomViewAdIndex(lastEditIndex + 1, customView);
            }
        }
        ZiceUtils.hideKeyBoard(mContext, mLastFocusEdit);
    }

    /**
     * Insert EditText at a specific location
     *
     * @param index   position of allLayout
     * @param editStr Text displayed by EditText
     */
    public void addEditTextAtIndex(final int index, CharSequence editStr) {
        MentionEditText newEdittext = createEditText("");
        //Determine whether the inserted string is empty. If there is no content, display the hint message.
        if (editStr != null && editStr.length() > 0) {
            newEdittext.setText(editStr);
        }
        newEdittext.setOnFocusChangeListener(mFocusListener);

        // Please note that EditText adds, or deletes, does not touch Transition animation.
        mAllLayout.setLayoutTransition(null);
        mAllLayout.addView(newEdittext, index);
        //After inserting a new EditText, modify the pointing of lastFocusEdit
        mLastFocusEdit = newEdittext;
        mLastFocusEdit.requestFocus();
        mLastFocusEdit.setSelection(editStr.length(), editStr.length());
    }

    /**
     * Add an ImageView to a specific location
     */
    public void addImageViewAtIndex(final int index, Bitmap bmp, String imagePath) {
        mImagePaths.add(imagePath);
        ZiceImageView imageView = new ZiceImageView(mContext);
        GlideApp.with(mContext).load(imagePath).centerCrop().into(imageView);
        imageView.setAbsolutePath(imagePath);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);//Cropping
        int imageHeight = mAllLayout.getWidth() * bmp.getHeight() / bmp.getWidth();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, imageHeight);
        lp.bottomMargin = EDIT_PADDING_TB;
        imageView.setLayoutParams(lp);

        GlideApp.with(mContext).load(imagePath).centerCrop()
                .placeholder(R.drawable.img_load_fail).error(R.drawable.img_load_fail).into(imageView);
        mAllLayout.addView(imageView, index);
    }

    public void addCustomViewAdIndex(int index, View customView) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = EDIT_PADDING_TB;
        customView.setLayoutParams(lp);
        mAllLayout.addView(customView, index);
    }

    /**
     * When the view is deleted, if the upper and lower are EditText, the merge processing
     */
    public void mergeEditText(int curIndex) {

        int postion;

        View curView = mAllLayout.getChildAt(curIndex); //当前焦点view，一定是mentionEdit
        View preView = mAllLayout.getChildAt(curIndex - 2);//要处理删除的前面的view
        View deleteView = mAllLayout.getChildAt(curIndex - 1);//要处理删除的view

        if (deleteView instanceof MentionEditText && curView instanceof MentionEditText) {

            MentionEditText deleteEdit = (MentionEditText) deleteView;
            MentionEditText curEdit = (MentionEditText) curView;
            postion = deleteEdit.getText().length();

            //处理后面的edittext，把后面的内容追加到前面
            List<Integer> nextRangeFromIndexList = new ArrayList<>();
            List<Integer> nextRangeToIndexList = new ArrayList<>();
            ArrayList<? extends Range> nextRanges = curEdit.getRangeManager().get();
            List<Range> nextFormatRanges = new ArrayList<>();
            for (int i = 0; i < nextRanges.size(); i++) {
                Range range = nextRanges.get(i);
                nextFormatRanges.add(range);
                nextRangeFromIndexList.add(range.getFrom());
                nextRangeToIndexList.add(range.getTo());
            }

            String nextString = curEdit.getText().toString();
            curEdit.clear();

            Editable curEditable = curEdit.getText();
            Editable deleteEditable = deleteEdit.getText();
            for (int i = 0; i < nextRangeFromIndexList.size(); i++) {
                Integer from = nextRangeFromIndexList.get(i);
                Integer to = nextRangeToIndexList.get(i);
                deleteEditable.append(nextString.substring(curEditable.length(), from));
                curEditable.append(nextString.substring(curEditable.length(), from));
                FormatRange range = (FormatRange) nextFormatRanges.get(i);
                CharSequence rangeCharSequence = range.getRangeCharSequence();
                FormatRange.FormatData formatData = range.getConvert();
                if (formatData instanceof User.UserConvert) {
                    User user = new User(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()));
                    deleteEdit.insert(user);
                    curEditable.append(user.charSequence());
                } else if (formatData instanceof Link.LinkConvert) {
                    Link link = new Link(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()));
                    deleteEdit.insert(link);
                    curEditable.append(link.charSequence());
                }
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    deleteEditable.append(nextString.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                deleteEditable.append(nextString);
            }

            mAllLayout.setLayoutTransition(null);
            //把后面合并过来的edit删除
            mAllLayout.removeView(curEdit);
            deleteEdit.requestFocus();
            deleteEdit.setSelection(postion);
        } else if (preView instanceof MentionEditText && curView instanceof MentionEditText) {
            MentionEditText preEdit = (MentionEditText) preView;
            MentionEditText curEdit = (MentionEditText) curView;
            postion = preEdit.getText().length();

            //处理后面的edittext，把后面的内容追加到前面
            List<Integer> nextRangeFromIndexList = new ArrayList<>();
            List<Integer> nextRangeToIndexList = new ArrayList<>();
            ArrayList<? extends Range> nextRanges = curEdit.getRangeManager().get();
            List<Range> nextFormatRanges = new ArrayList<>();
            for (int i = 0; i < nextRanges.size(); i++) {
                Range range = nextRanges.get(i);
                nextFormatRanges.add(range);
                nextRangeFromIndexList.add(range.getFrom());
                nextRangeToIndexList.add(range.getTo());
            }

            String nextString = curEdit.getText().toString();
            curEdit.clear();

            Editable preEditable = preEdit.getText();
            Editable curEditable = curEdit.getText();
            for (int i = 0; i < nextRangeFromIndexList.size(); i++) {
                Integer from = nextRangeFromIndexList.get(i);
                Integer to = nextRangeToIndexList.get(i);
                preEditable.append(nextString.substring(curEditable.length(), from));
                curEditable.append(nextString.substring(curEditable.length(), from));
                FormatRange range = (FormatRange) nextFormatRanges.get(i);
                CharSequence rangeCharSequence = range.getRangeCharSequence();
                FormatRange.FormatData formatData = range.getConvert();
                if (formatData instanceof User.UserConvert) {
                    User user = new User(formatData.formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()));
                    preEdit.insert(user);
                    curEditable.append(user.charSequence());
                } else if (formatData instanceof Link.LinkConvert) {
                    Link link = new Link(formatData.formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()));
                    preEdit.insert(link);
                    curEditable.append(link.charSequence());
                }
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    preEditable.append(nextString.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                preEditable.append(nextString);
            }

            mAllLayout.setLayoutTransition(null);
            //把后面合并过来的edit删除
            mAllLayout.removeView(curEdit);
            mAllLayout.removeView(deleteView);
            preEdit.requestFocus();
            preEdit.setSelection(postion);
        }
    }

    /**
     * Get content
     */
    public String getAllLayoutText() {
        if (mAllLayout.getChildCount() == 0) {
            return "RESULT_NULL";
        }
        for (int i = 0; i < mAllLayout.getChildCount(); i++) {
            View currentView = mAllLayout.getChildAt(i);
            if (currentView instanceof MentionEditText) {       //Pure characters and @somebody
                MentionEditText editText = (MentionEditText) currentView;
                if (!editText.getFormatCharSequence().toString().equals("")) {
                    mUpdateText = mUpdateText.append("<p>")
                            .append(editText.getFormatCharSequence().toString())
                            .append("</p>");
                }
            }
            if (currentView instanceof ZiceImageView) {     //picture
                ZiceImageView ziceImageView = (ZiceImageView) currentView;
                mUpdateText.append("<img data-src=\"")
                        .append(ziceImageView.getAbsolutePath())
                        .append("\"/>");
            }
            if (currentView instanceof ZiceCustomView) {    //custom view
                ZiceCustomView customView = (ZiceCustomView) currentView;
                mUpdateText.append("<custom viewId=\"")
                        .append(customView.getViewId())
                        .append("\"/>");
            }
        }
        String resultText = mUpdateText.toString();
        mUpdateText.delete(0, mUpdateText.length());
        return resultText;
    }

}