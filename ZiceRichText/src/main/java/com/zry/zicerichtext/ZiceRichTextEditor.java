package com.zry.zicerichtext;

import android.app.Activity;
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
import com.zry.zicerichtext.mentions.edit.User;
import com.zry.zicerichtext.mentions.library.edit.MentionEditText;
import com.zry.zicerichtext.mentions.library.model.FormatRange;
import com.zry.zicerichtext.mentions.library.model.Range;
import com.zry.zicerichtext.utils.GlideApp;
import com.zry.zicerichtext.utils.ZiceUtils;

import java.util.ArrayList;
import java.util.List;

public class ZiceRichTextEditor extends ScrollView {

    private Context mContext;
    private Activity activity;

    private LayoutInflater inflater;
    private int viewTagIndex = 1; // The newer view will have a tag, which is unique for each view.
    private LinearLayout allLayout; // This is the container for all child views, the only one inside the scrollView.
    private MentionEditText lastFocusEdit; // Recently focused EditText
    private DeletableImageView item;

    private OnKeyListener keyListener; // SoftText Listener for all EditText
    private OnClickListener btnDeleteListener; // Red cross button listener in the upper right corner of the picture
    private OnFocusChangeListener focusListener; // All EditText focus listeners listener
    private OnRtImageDeleteListener onRtImageDeleteListener;//Interface for deleting pictures
    private OnRtImageClickListener onRtImageClickListener;//Click on the image interface

    private static final int EDIT_PADDING = 10; // Edittext regular padding is 10dp
    private int editNormalPadding = 0; //
    private int disappearingImageIndex = 0;

    StringBuilder updateText = new StringBuilder();  //Text to be uploaded
    private String currentPath = "";
    private ArrayList<String> imagePaths;   //Image address collection
    private int AtColor = 0xff42ca6e;

    /**
     * Custom attribute
     **/
    //The inserted picture shows the height
    private int rtImageHeight = 500;
    //Two adjacent image spacing
    private int rtImageBottom = 10;
    //Text related attributes, initial prompt information, text size and color
    private String rtTextInitHint = "";
    private int rtTextSize = 16;
    private int rtTextColor = Color.parseColor("#42ca6e");
    private int rtTextLineSpace = 8;

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
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RichTextEditor);
        rtImageHeight = ta.getInteger(R.styleable.RichTextEditor_rt_editor_image_height, 500);
        rtImageBottom = ta.getInteger(R.styleable.RichTextEditor_rt_editor_image_bottom, 10);
        rtTextSize = ta.getDimensionPixelSize(R.styleable.RichTextEditor_rt_editor_text_size, 16);
        rtTextLineSpace = ta.getDimensionPixelSize(R.styleable.RichTextEditor_rt_editor_text_line_space, 8);
        rtTextColor = ta.getColor(R.styleable.RichTextEditor_rt_editor_text_color, Color.parseColor("#757575"));
        rtTextInitHint = ta.getString(R.styleable.RichTextEditor_rt_editor_text_init_hint);

        ta.recycle();

        activity = (Activity) context;

        imagePaths = new ArrayList<>();

        inflater = LayoutInflater.from(context);

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
        allLayout = new LinearLayout(mContext);
        allLayout.setOrientation(LinearLayout.VERTICAL);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        allLayout.setPadding(50, 15, 50, 15);//Set the spacing to prevent the text from being too close to the edge when generating the image, not using margin, otherwise there is a black border
        addView(allLayout, layoutParams);
    }

    private void initKeyBoardBackPress() {
        // Mainly used to handle the click-to-delete button, some column merge operations of the view
        keyListener = new OnKeyListener() {

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
     *Handling the soft keyboard backSpace rollback event
     *
     * @param editTxt The text input box where the cursor is located
     */
    private void onBackspacePress(MentionEditText editTxt) {
        int startSelection = editTxt.getSelectionStart();
        // Only when the cursor has been topped to the forefront of the text input box, in the decision whether to delete the previous picture, or two View merge
        if (startSelection == 0) {
            int editIndex = allLayout.indexOfChild(editTxt);
            View preView = allLayout.getChildAt(editIndex - 1); // If editIndex-1<0,
            // Then returns null
            if (null != preView) {
                if (preView instanceof RelativeLayout) {
                    // The previous view of the cursor EditText corresponds to the image
                    onImageCloseClick(preView);
                } else if (preView instanceof MentionEditText) {
                    disappearingImageIndex = editIndex;
                    mergeEditText2();
                } else if (preView instanceof LinearLayout) {
                }
            }
        }
    }

    private void initImgDelete() {
        btnDeleteListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (v instanceof DeletableImageView) {
                    DeletableImageView imageView = (DeletableImageView) v;
                    // Open picture click interface
                    if (onRtImageClickListener != null) {
                        onRtImageClickListener.onRtImageClick(imageView.getAbsolutePath());
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

    public void setOnRtImageDeleteListener(OnRtImageDeleteListener onRtImageDeleteListener) {
        this.onRtImageDeleteListener = onRtImageDeleteListener;
    }

    public interface OnRtImageClickListener {
        void onRtImageClick(String imagePath);
    }

    public void setOnRtImageClickListener(OnRtImageClickListener onRtImageClickListener) {
        this.onRtImageClickListener = onRtImageClickListener;
    }

    private void initFocusChangeListener() {
        focusListener = new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    lastFocusEdit = (MentionEditText) v;
                }
            }
        };

    }

    private void addFirstView() {
        LinearLayout.LayoutParams firstEditParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        MentionEditText firstEdit = createEditText(rtTextInitHint, ZiceUtils.dip2px(mContext, EDIT_PADDING));
        allLayout.addView(firstEdit, firstEditParam);
        lastFocusEdit = firstEdit;
    }

    /**
     * Insert a @somebody
     *
     * @param user user
     * @param mContext mContext
     */
    public void insertMention(User user, Context mContext, int color) {
        this.AtColor = color;
        lastFocusEdit.insert(user, user.getUserId(), mContext);
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
        if (allLayout.getChildCount() == 0) {
            return "RESULT_NULL";
        }
        for (int i = 0; i < allLayout.getChildCount(); i++) {
            View currentView = allLayout.getChildAt(i);
            if (currentView instanceof MentionEditText) {       //Pure characters and @somebody
                MentionEditText editText = (MentionEditText) currentView;
                if (!editText.getFormatCharSequence().toString().equals("")) {
                    updateText = updateText.append("<p>" + editText.getFormatCharSequence().toString() + "</p>");
                }
            }
            if (currentView instanceof RelativeLayout) {     //picture
                item = currentView.findViewById(R.id.edit_imageView);
                currentPath = item.getAbsolutePath();
                updateText.append("<img data-src=\"" + currentPath + "/>");
            }
        }
        String resultText = updateText.toString();
        updateText.delete(0, updateText.length());
        return resultText;
    }


    /**
     * Generate text input box
     */
    public MentionEditText createEditText(String hint, int paddingTop) {
        MentionEditText editText = (MentionEditText) inflater.inflate(R.layout.zice_rich_edittext, null);
        editText.setOnKeyListener(keyListener);
        editText.setTag(viewTagIndex++);
        editText.setPadding(editNormalPadding, paddingTop, editNormalPadding, paddingTop);
        editText.setHint(hint);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, rtTextSize);
        editText.setLineSpacing(rtTextLineSpace, 1.0f);
        editText.setOnFocusChangeListener(focusListener);
        return editText;
    }


    /**
     * Processing after inserting the image
     */
    public void insertImageToEditor(Bitmap bitmap, String imagePath) {
        //lastFocusEdit gets the focus of the EditText
        String lastEditStr = lastFocusEdit.getText().toString();
        CharSequence formatCharSequence = lastFocusEdit.getFormatCharSequence();
        // Actual cursor position
        int actualCursorIndex = 0;
        //Get the location of the cursor
        int cursorIndex = lastFocusEdit.getSelectionStart();
        List<Integer> rangeFromIndexList = new ArrayList<>();
        List<Integer> rangeToIndexList = new ArrayList<>();
        ArrayList<? extends Range> ranges = lastFocusEdit.getRangeManager().get();
        List<Range> formatRanges = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            formatRanges.add(range);
            rangeFromIndexList.add(range.getFrom());
            rangeToIndexList.add(range.getTo());
        }

        String editStr1 = lastEditStr.substring(0, cursorIndex).trim();//Get the string in front of the cursor
        String editStr2 = lastEditStr.substring(cursorIndex).trim();//Get the string after the cursor

        int lastEditIndex = allLayout.indexOfChild(lastFocusEdit);//Get the location of the EditText in focus

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
            lastFocusEdit.clear();
            Editable editable = lastFocusEdit.getText();
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
                    User user = new User(range.getConvert().getUserId(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), AtColor);
                    insertMention(user, getContext(), AtColor);
                } else {
                    editable.append(lastEditStr.substring(editable.length(), cursorIndex));
                }
            }
            editable.append(editStr1.substring(editable.length()));

            //If the cursor is already in the middle of the editText, you need to split the string, split it into two EditTexts, and insert the image between the two EditTexts.
            //Leave the string in front of the cursor and set it to the currently selected EditText (this is the first EditText split)
            // lastFocusEdit.setText(editStr1);

            //Put the string after the cursor in the newly created EditText (this is the second EditText split)
            MentionEditText editText2 = createEditText("", EDIT_PADDING);
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
                            User user = new User(range.getConvert().getUserId(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), AtColor);
                            editText2.insert(user, user.getUserId(), getContext());
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

            editText2.setOnFocusChangeListener(focusListener);

            //Please note that EditText adds, or deletes, does not touch Transition animation.
            allLayout.setLayoutTransition(null);
            allLayout.addView(editText2, lastEditIndex + 1);
            //After inserting a new EditText, modify the pointing of lastFocusEdit
            lastFocusEdit = editText2;
            lastFocusEdit.requestFocus();
            //Insert an empty EditText at the position of the second EditText so that when multiple images are inserted consecutively, there is space to write the text, and the second EditText is moved down.
            addEditTextAtIndex(lastEditIndex + 1, "");
            //Insert the image layout in the position of the empty EditText, move the empty EditText down
            addImageViewAtIndex(lastEditIndex + 1, bitmap, imagePath);
        }
        ZiceUtils.hideKeyBoard(getContext(), lastFocusEdit);
    }

    /**
     *Insert EditText at a specific location
     *
     * @param index   position
     * @param editStr Text displayed by EditText
     */
    public void addEditTextAtIndex(final int index, CharSequence editStr) {
        MentionEditText editText2 = createEditText("", EDIT_PADDING);
        //Determine whether the inserted string is empty. If there is no content, display the hint message.
        if (editStr != null && editStr.length() > 0) {
            editText2.setText(editStr);
        }
        editText2.setOnFocusChangeListener(focusListener);

        // Please note that EditText adds, or deletes, does not touch Transition animation.
        allLayout.setLayoutTransition(null);
        allLayout.addView(editText2, index);
        //After inserting a new EditText, modify the pointing of lastFocusEdit
        lastFocusEdit = editText2;
        lastFocusEdit.requestFocus();
        lastFocusEdit.setSelection(editStr.length(), editStr.length());
    }

    /**
     * Add an ImageView to a specific location
     */
    public void addImageViewAtIndex(final int index, Bitmap bmp, String imagePath) {
        imagePaths.add(imagePath);
        RelativeLayout imageLayout = createImageLayout();
        DeletableImageView imageView = (DeletableImageView) imageLayout.findViewById(R.id.edit_imageView);
        GlideApp.with(getContext()).load(imagePath).centerCrop().into(imageView);
        imageView.setAbsolutePath(imagePath);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);//Cropping
        int imageHeight = allLayout.getWidth() * bmp.getHeight() / bmp.getWidth();
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, imageHeight);//TODO Image height, consider custom attributes
        lp.bottomMargin = rtImageBottom;
        imageView.setLayoutParams(lp);

        if (rtImageHeight > 0) {
            GlideApp.with(getContext()).load(imagePath).centerCrop()
                    .placeholder(R.drawable.img_load_fail).error(R.drawable.img_load_fail).into(imageView);
        } else {
            GlideApp.with(getContext()).load(imagePath)
                    .placeholder(R.drawable.img_load_fail).error(R.drawable.img_load_fail).into(imageView);
        }
        allLayout.addView(imageLayout, index);
    }

    /**
     * Generate image view
     */
    private RelativeLayout createImageLayout() {
        RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.edit_imageview, null);
        layout.setTag(viewTagIndex++);
        View closeView = layout.findViewById(R.id.image_close);
        closeView.setTag(layout.getTag());
        closeView.setOnClickListener(btnDeleteListener);
        DeletableImageView imageView = layout.findViewById(R.id.edit_imageView);
        imageView.setOnClickListener(btnDeleteListener);
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
        disappearingImageIndex = allLayout.indexOfChild(view);
        //Delete the image in the folder
        List<EditData> dataList = buildEditData();
        EditData editData = dataList.get(disappearingImageIndex);
        if (editData.imagePath != null) {
            if (onRtImageDeleteListener != null) {
                //TODO Through the interface callback, the image deletion operation is processed in the note editing interface.
                onRtImageDeleteListener.onRtImageDelete(editData.imagePath);
            }
            imagePaths.remove(editData.imagePath);
        }
        mergeEditText();//Merge upper and lower EditText content
    }

    /**
     * Externally provided interface, generate edit data upload
     */
    public List<EditData> buildEditData() {
        List<EditData> dataList = new ArrayList<EditData>();
        int num = allLayout.getChildCount();
        for (int index = 0; index < num; index++) {
            View itemView = allLayout.getChildAt(index);
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

        View preView = allLayout.getChildAt(disappearingImageIndex - 1);
        View nextView = allLayout.getChildAt(disappearingImageIndex + 1);
        View emptyView = allLayout.getChildAt(disappearingImageIndex);
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
                User user = new User(range.getConvert().getUserId(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), AtColor);
                preEdit.insert(user, user.getUserId(), getContext());
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    preEditable.append(nextStringValue.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                preEditable.append(nextStringValue);
            }

            allLayout.setLayoutTransition(null);
            allLayout.removeView(nextEdit);
            allLayout.removeView(emptyView);
            preEdit.requestFocus();
            preEdit.setSelection(postion);
        }
    }


    private void mergeEditText2() {

        int postion;

        View preView = allLayout.getChildAt(disappearingImageIndex - 1);
        View currentView = allLayout.getChildAt(disappearingImageIndex);
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
                User user = new User(range.getConvert().getUserId(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), AtColor);
                preEdit.insert(user, user.getUserId(), getContext());
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    preEditable.append(nextStringValue.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                preEditable.append(nextStringValue);
            }
            allLayout.setLayoutTransition(null);
            allLayout.removeView(currentEdit);
            preEdit.requestFocus();
            preEdit.setSelection(postion);
        }
    }

    /**
     * When editing, the focus will be at the end of the merge, so add this method, this method will not get the focus
     */
    private void mergeEditText3() {

        int postion;

        View preView = allLayout.getChildAt(disappearingImageIndex - 1);
        View currentView = allLayout.getChildAt(disappearingImageIndex);
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
                User user = new User(range.getConvert().getUserId(), rangeCharSequence.subSequence(1, rangeCharSequence.length()), AtColor);
                preEdit.insert(user, user.getUserId(), getContext());
                if (i == (nextRangeFromIndexList.size() - 1)) {
                    preEditable.append(nextStringValue.substring(to));
                }
            }
            if (nextRangeFromIndexList.size() == 0) {
                preEditable.append(nextStringValue);
            }
            allLayout.setLayoutTransition(null);
            allLayout.removeView(currentEdit);
        }
    }

}
