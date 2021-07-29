package com.zry.zicerichtext;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
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
    private OnClickListener mBtnDeleteListener; // Red cross button listener in the upper right corner of the picture
    private OnFocusChangeListener mFocusListener; // All EditText focus listeners listener
    private OnRtImageDeleteListener mOnRtImageDeleteListener;//Interface for deleting pictures
    private OnRtImageClickListener mOnRtImageClickListener;//Click on the image interface

    private Context mContext;
    private LayoutInflater mInflater;
    private int mViewTagIndex; // The newer view will have a tag, which is unique for each view.
    private LinearLayout mAllLayout; // This is the container for all child views, the only one inside the scrollView.
    private MentionEditText mLastFocusEdit; // Recently focused EditText
    private DeletableImageView mDeletableImageView;
    private int mDisappearingImageIndex = 0;
    StringBuilder mUpdateText = new StringBuilder();  //Text to be uploaded
    private String mCurrentPath = "";
    private ArrayList<String> mImagePaths;   //Image address collection
    private int mAtColor = 0xff42ca6e;

    /**
     * Custom attribute
     **/
    //The inserted picture shows the height
    private int rtImageHeight;
    //Text related attributes, initial prompt information, text size and color
    private String mZrtTextInitHint;
    private int mZrtTextSize;
    private int mZrtTextColor;
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
        rtImageHeight = ta.getInteger(R.styleable.ZiceRichTextEditor_rt_editor_image_height, 500);
        mZrtTextSize = ta.getDimensionPixelSize(R.styleable.ZiceRichTextEditor_rt_editor_text_size, 16);
        mZrtTextLineSpace = ta.getDimensionPixelSize(R.styleable.ZiceRichTextEditor_rt_editor_text_line_space, 8);
        mZrtTextColor = ta.getColor(R.styleable.ZiceRichTextEditor_rt_editor_text_color, Color.parseColor("#757575"));
        mZrtTextInitHint = ta.getString(R.styleable.ZiceRichTextEditor_rt_editor_text_init_hint);

        ta.recycle();

        mImagePaths = new ArrayList<>();

        mInflater = LayoutInflater.from(context);

        //1. Initialize allLayout
        initAllLayout();

        // 2. Initialize keyboard backspace monitoring
        initKeyBoardBackPress();

        // 3. Picture fork processing
        initImgDelete();

        //4、Focus change monitor
        initFocusChangeListener();

        //5、Add the first view
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
            int editIndex = mAllLayout.indexOfChild(editTxt);
            View preView = mAllLayout.getChildAt(editIndex - 1); // If editIndex-1<0,
            // Then returns null
            if (null != preView) {
                if (preView instanceof RelativeLayout) {
                    // The previous view of the cursor EditText corresponds to the image
                    onImageCloseClick(preView);
                } else if (preView instanceof MentionEditText) {
                    mDisappearingImageIndex = editIndex;
                    mergeEditText2();
                } else if (preView instanceof LinearLayout) {
                }
            }
        }
    }

    private void initImgDelete() {
        mBtnDeleteListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v instanceof DeletableImageView) {
                    DeletableImageView imageView = (DeletableImageView) v;
                    // Open picture click interface
                    if (mOnRtImageClickListener != null) {
                        mOnRtImageClickListener.onRtImageClick(imageView.getAbsolutePath());
                    }
                }
                //                                else if (v instanceof ClosePlantImageView) {
                //                                    LinearLayout parentView = (LinearLayout) v.getParent();
                //                                    onPlantCloseClick(parentView);
                //                                }
                else if (v instanceof ImageView) {
                    RelativeLayout parentView = (RelativeLayout) v.getParent();
                    onImageCloseClick(parentView);
                }
            }
        };
    }

    public interface OnRtImageDeleteListener {
        void onRtImageDelete(String imagePath);
    }

    public void setOnRtImageDeleteListener(OnRtImageDeleteListener mOnRtImageDeleteListener) {
        this.mOnRtImageDeleteListener = mOnRtImageDeleteListener;
    }

    public interface OnRtImageClickListener {
        void onRtImageClick(String imagePath);
    }

    public void setOnRtImageClickListener(OnRtImageClickListener mOnRtImageClickListener) {
        this.mOnRtImageClickListener = mOnRtImageClickListener;
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
     * Insert a @somebody
     *
     * @param user user
     */
    public void insertMention(User user) {
        this.mAtColor = user.color();
        mLastFocusEdit.insert(user);
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

                @Override
                public void onLoadStarted(Drawable placeholder) {
                    return;
                }

                @Override
                public void onLoadFailed(Drawable errorDrawable) {
                    return;
                }
            });
        } else {
            Bitmap bmp = ZiceUtils.getScaledBitmap(imagePath, width);
            insertImageToEditor(bmp, imagePath);
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
                    mUpdateText = mUpdateText.append("<p>" + editText.getFormatCharSequence().toString() + "</p>");
                }
            }
            if (currentView instanceof RelativeLayout) {     //picture
                mDeletableImageView = currentView.findViewById(R.id.edit_imageView);
                mCurrentPath = mDeletableImageView.getAbsolutePath();
                mUpdateText.append("<img data-src=\"" + mCurrentPath + "/>");
            }
        }
        String resultText = mUpdateText.toString();
        mUpdateText.delete(0, mUpdateText.length());
        return resultText;
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
     * Processing after inserting the image
     */
    public void insertImageToEditor(Bitmap bitmap, String imagePath) {
        //lastFocusEdit gets the focus of the EditText
        String lastEditStr = mLastFocusEdit.getText().toString();
        CharSequence formatCharSequence = mLastFocusEdit.getFormatCharSequence();
        // Actual cursor position
        int actualCursorIndex = 0;
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

        String editStr1 = lastEditStr.substring(0, cursorIndex).trim();//Get the string in front of the cursor
        String editStr2 = lastEditStr.substring(cursorIndex).trim();//Get the string after the cursor

        int lastEditIndex = mAllLayout.indexOfChild(mLastFocusEdit);//Get the location of the EditText in focus

        if (lastEditStr.length() == 0) {
            //If the currently selected EditText is empty, insert the image directly below the EditText and insert an empty EditText.
            addEditTextAtIndex(lastEditIndex + 1, "");
            addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
        } else if (editStr1.length() == 0) {
            //If the cursor is already at the top of the editText, insert the image directly and the EditText will move down.
            addImageViewAtIndex(lastEditIndex, bitmap, imagePath);
            //Insert an empty EditText at the same time to prevent multiple images from being inserted.
            addEditTextAtIndex(lastEditIndex + 1, "");
        } else if (editStr2.length() == 0) {
            // If the cursor is already at the very end of editText, you need to add a new imageView and EditText
            addEditTextAtIndex(lastEditIndex + 1, "");
            addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
        } else {

            // Clear the cursor before the EditText
            mLastFocusEdit.clear();
            Editable editable = mLastFocusEdit.getText();
            List<Integer> usedIndexList = new ArrayList<>();
            for (int i = 0; i < rangeFromIndexList.size(); i++) {
                Integer from = rangeFromIndexList.get(i);
                Integer to = rangeToIndexList.get(i);
                if (to <= cursorIndex) {
                    usedIndexList.add(i);
                    //Explain that there is this user before the cursor
                    editable.append(lastEditStr.substring(editable.length(), from));
                    FormatRange range = (FormatRange) formatRanges.get(i);
                    CharSequence rangeCharSequence = range.getRangeCharSequence();
                    User user = new User(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), mAtColor);
                    insertMention(user);
                } else {
                    editable.append(lastEditStr.substring(editable.length(), cursorIndex));
                }
            }
            editable.append(editStr1.substring(editable.length()));

            //If the cursor is already in the middle of the editText, you need to split the string, split it into two EditTexts, and insert the image between the two EditTexts.
            //Leave the string in front of the cursor and set it to the currently selected EditText (this is the first EditText split)
            // lastFocusEdit.setText(editStr1);

            //Put the string after the cursor in the newly created EditText (this is the second EditText split)
            MentionEditText editText2 = createEditText("");
            //Determine whether the inserted string is empty. If there is no content, display the hint message.
            if (editStr2.length() > 0) {
                Editable editable1 = editText2.getText();
                for (int i = 0; i < rangeFromIndexList.size(); i++) {
                    if (usedIndexList.contains(i)) {
                        continue;
                    }
                    Integer from = rangeFromIndexList.get(i);
                    Integer to = rangeToIndexList.get(i);
                    int shengxiade = rangeFromIndexList.size() - usedIndexList.size();
                    if (shengxiade == 0) {
                        editable1.append(editStr2);
                        break;
                    } else {
                        if (from >= cursorIndex) {
                            // Explain that there is this user after cursor
                            int beginIndex = cursorIndex + editable1.length();
                            editable1.append(lastEditStr.substring(beginIndex, from));
                            FormatRange range = (FormatRange) formatRanges.get(i);
                            CharSequence rangeCharSequence = range.getRangeCharSequence();
                            User user = new User(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), mAtColor);
                            editText2.insert(user);
                            if (i == rangeFromIndexList.size() - 1) {
                                editable1.append(lastEditStr.substring(to));
                            }
                        }
                    }
                }
                if (editable1.length() == 0) {
                    editable1.append(editStr2);
                }
            }

            editText2.setOnFocusChangeListener(mFocusListener);

            //Please note that EditText adds, or deletes, does not touch Transition animation.
            mAllLayout.setLayoutTransition(null);
            mAllLayout.addView(editText2, lastEditIndex + 1);
            //After inserting a new EditText, modify the pointing of lastFocusEdit
            mLastFocusEdit = editText2;
            mLastFocusEdit.requestFocus();
            //Insert an empty EditText at the position of the second EditText so that when multiple images are inserted consecutively, there is space to write the text, and the second EditText is moved down.
            addEditTextAtIndex(lastEditIndex + 1, "");
            //Insert the image layout in the position of the empty EditText, move the empty EditText down
            addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
        }
        ZiceUtils.hideKeyBoard(getContext(), mLastFocusEdit);
    }

    /**
     * Insert EditText at a specific location
     *
     * @param index   position
     * @param editStr Text displayed by EditText
     */
    public void addEditTextAtIndex(final int index, CharSequence editStr) {
        MentionEditText editText2 = createEditText("");
        //Determine whether the inserted string is empty. If there is no content, display the hint message.
        if (editStr != null && editStr.length() > 0) {
            editText2.setText(editStr);
        }
        editText2.setOnFocusChangeListener(mFocusListener);

        // Please note that EditText adds, or deletes, does not touch Transition animation.
        mAllLayout.setLayoutTransition(null);
        mAllLayout.addView(editText2, index);
        //After inserting a new EditText, modify the pointing of lastFocusEdit
        mLastFocusEdit = editText2;
        mLastFocusEdit.requestFocus();
        mLastFocusEdit.setSelection(editStr.length(), editStr.length());
    }

    /**
     * Add an ImageView to a specific location
     */
    public void addImageViewAtIndex(final int index, Bitmap bmp, String imagePath) {
        mImagePaths.add(imagePath);
        RelativeLayout imageLayout = createImageLayout();
        DeletableImageView imageView = (DeletableImageView) imageLayout.findViewById(R.id.edit_imageView);
        GlideApp.with(getContext()).load(imagePath).centerCrop().into(imageView);
        imageView.setAbsolutePath(imagePath);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);//Cropping
        int imageHeight = mAllLayout.getWidth() * bmp.getHeight() / bmp.getWidth();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, imageHeight);//TODO Image height, consider custom attributes
        lp.bottomMargin = EDIT_PADDING_TB;
        imageView.setLayoutParams(lp);

        if (rtImageHeight > 0) {
            GlideApp.with(getContext()).load(imagePath).centerCrop()
                    .placeholder(R.drawable.img_load_fail).error(R.drawable.img_load_fail).into(imageView);
        } else {
            GlideApp.with(getContext()).load(imagePath)
                    .placeholder(R.drawable.img_load_fail).error(R.drawable.img_load_fail).into(imageView);
        }
        mAllLayout.addView(imageLayout, index);
    }

    /**
     * Generate image view
     */
    private RelativeLayout createImageLayout() {
        RelativeLayout layout = (RelativeLayout) mInflater.inflate(R.layout.edit_imageview, null);
        layout.setTag(mViewTagIndex++);
        View closeView = layout.findViewById(R.id.image_close);
        closeView.setTag(layout.getTag());
        closeView.setOnClickListener(mBtnDeleteListener);
        DeletableImageView imageView = layout.findViewById(R.id.edit_imageView);
        imageView.setOnClickListener(mBtnDeleteListener);
        return layout;
    }

    /**
     * Handling the click event of the image
     *
     * @param view The relativeLayout view corresponding to the entire image
     * @type Delete type 0 means backspace delete 1 means press red cross button to delete
     */
    private void onImageCloseClick(View view) {
        //if (!mTransitioner.isRunning()) {
        mDisappearingImageIndex = mAllLayout.indexOfChild(view);
        //Delete the image in the folder
        List<EditData> dataList = buildEditData();
        EditData editData = dataList.get(mDisappearingImageIndex);
        if (editData.imagePath != null) {
            if (mOnRtImageDeleteListener != null) {
                //TODO Through the interface callback, the image deletion operation is processed in the note editing interface.
                mOnRtImageDeleteListener.onRtImageDelete(editData.imagePath);
            }
            mImagePaths.remove(editData.imagePath);
        }
        mergeEditText();//Merge upper and lower EditText content
    }

    /**
     * Externally provided interface, generate edit data upload
     */
    public List<EditData> buildEditData() {
        List<EditData> dataList = new ArrayList<EditData>();
        int num = mAllLayout.getChildCount();
        for (int index = 0; index < num; index++) {
            View itemView = mAllLayout.getChildAt(index);
            EditData itemData = new EditData();
            if (itemView instanceof MentionEditText) {
                MentionEditText item = (MentionEditText) itemView;
                itemData.inputStr = item.getText().toString();
            } else if (itemView instanceof RelativeLayout) {
                DeletableImageView item = (DeletableImageView) itemView.findViewById(R.id.edit_imageView);
                itemData.imagePath = item.getAbsolutePath();
            }
            dataList.add(itemData);
        }

        return dataList;
    }

    /**
     * When the image is deleted, if the upper and lower are EditText, the merge processing
     */
    private void mergeEditText() {

        int postion;

        View preView = mAllLayout.getChildAt(mDisappearingImageIndex - 1);
        View nextView = mAllLayout.getChildAt(mDisappearingImageIndex + 1);
        View emptyView = mAllLayout.getChildAt(mDisappearingImageIndex);
        if (preView != null && preView instanceof MentionEditText && null != nextView && nextView instanceof MentionEditText) {
            MentionEditText preEdit = (MentionEditText) preView;
            MentionEditText nextEdit = (MentionEditText) nextView;
            postion = preEdit.getText().length();

            List<Integer> nextRangeFromIndexList = new ArrayList<>();
            List<Integer> nextRangeToIndexList = new ArrayList<>();
            ArrayList<? extends Range> nextRanges = nextEdit.getRangeManager().get();
            List<Range> nextFormatRanges = new ArrayList<>();
            for (int i = 0; i < nextRanges.size(); i++) {
                Range range = nextRanges.get(i);
                nextFormatRanges.add(range);
                nextRangeFromIndexList.add(range.getFrom());
                nextRangeToIndexList.add(range.getTo());
            }

            String nextString = nextEdit.getText().toString();
            String nextStringValue = nextString;
            nextEdit.clear();
            Editable nextEditable = nextEdit.getText();
            Editable preEditable = preEdit.getText();
            for (int i = 0; i < nextRangeFromIndexList.size(); i++) {
                Integer from = nextRangeFromIndexList.get(i);
                Integer to = nextRangeToIndexList.get(i);
                preEditable.append(nextStringValue.substring(nextEditable.length(), from));
                FormatRange range = (FormatRange) nextFormatRanges.get(i);
                CharSequence rangeCharSequence = range.getRangeCharSequence();
                User user = new User(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), mAtColor);
                preEdit.insert(user);
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    preEditable.append(nextStringValue.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                preEditable.append(nextStringValue);
            }

            mAllLayout.setLayoutTransition(null);
            mAllLayout.removeView(nextEdit);
            mAllLayout.removeView(emptyView);
            preEdit.requestFocus();
            preEdit.setSelection(postion);
        }
    }


    private void mergeEditText2() {

        int postion;

        View preView = mAllLayout.getChildAt(mDisappearingImageIndex - 1);
        View currentView = mAllLayout.getChildAt(mDisappearingImageIndex);
        if (preView != null && preView instanceof MentionEditText && null != currentView && currentView instanceof MentionEditText) {
            MentionEditText preEdit = (MentionEditText) preView;
            MentionEditText currentEdit = (MentionEditText) currentView;
            postion = preEdit.getText().length();
            List<Integer> nextRangeFromIndexList = new ArrayList<>();
            List<Integer> nextRangeToIndexList = new ArrayList<>();
            ArrayList<? extends Range> nextRanges = currentEdit.getRangeManager().get();
            List<Range> nextFormatRanges = new ArrayList<>();
            for (int i = 0; i < nextRanges.size(); i++) {
                Range range = nextRanges.get(i);
                nextFormatRanges.add(range);
                nextRangeFromIndexList.add(range.getFrom());
                nextRangeToIndexList.add(range.getTo());
            }
            String nextString = currentEdit.getText().toString();
            String nextStringValue = nextString;
            currentEdit.clear();
            Editable nextEditable = currentEdit.getText();
            Editable preEditable = preEdit.getText();
            for (int i = 0; i < nextRangeFromIndexList.size(); i++) {
                Integer from = nextRangeFromIndexList.get(i);
                Integer to = nextRangeToIndexList.get(i);
                preEditable.append(nextStringValue.substring(nextEditable.length(), from));
                FormatRange range = (FormatRange) nextFormatRanges.get(i);
                CharSequence rangeCharSequence = range.getRangeCharSequence();
                User user = new User(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), mAtColor);
                preEdit.insert(user);
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    preEditable.append(nextStringValue.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                preEditable.append(nextStringValue);
            }
            mAllLayout.setLayoutTransition(null);
            mAllLayout.removeView(currentEdit);
            preEdit.requestFocus();
            preEdit.setSelection(postion);
        }
    }

    /**
     * When editing, the focus will be at the end of the merge, so add this method, this method will not get the focus
     */
    private void mergeEditText3() {

        int postion;

        View preView = mAllLayout.getChildAt(mDisappearingImageIndex - 1);
        View currentView = mAllLayout.getChildAt(mDisappearingImageIndex);
        if (preView != null && preView instanceof MentionEditText && null != currentView && currentView instanceof MentionEditText) {
            MentionEditText preEdit = (MentionEditText) preView;
            MentionEditText currentEdit = (MentionEditText) currentView;
            postion = preEdit.getText().length();
            List<Integer> nextRangeFromIndexList = new ArrayList<>();
            List<Integer> nextRangeToIndexList = new ArrayList<>();
            ArrayList<? extends Range> nextRanges = currentEdit.getRangeManager().get();
            List<Range> nextFormatRanges = new ArrayList<>();
            for (int i = 0; i < nextRanges.size(); i++) {
                Range range = nextRanges.get(i);
                nextFormatRanges.add(range);
                nextRangeFromIndexList.add(range.getFrom());
                nextRangeToIndexList.add(range.getTo());
            }
            String nextString = currentEdit.getText().toString();
            String nextStringValue = nextString;
            currentEdit.clear();
            Editable nextEditable = currentEdit.getText();
            Editable preEditable = preEdit.getText();
            for (int i = 0; i < nextRangeFromIndexList.size(); i++) {
                Integer from = nextRangeFromIndexList.get(i);
                Integer to = nextRangeToIndexList.get(i);
                preEditable.append(nextStringValue.substring(nextEditable.length(), from));
                FormatRange range = (FormatRange) nextFormatRanges.get(i);
                CharSequence rangeCharSequence = range.getRangeCharSequence();
                User user = new User(range.getConvert().formatParam(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), mAtColor);
                preEdit.insert(user);
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    preEditable.append(nextStringValue.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                preEditable.append(nextStringValue);
            }
            mAllLayout.setLayoutTransition(null);
            mAllLayout.removeView(currentEdit);
        }
    }

}
