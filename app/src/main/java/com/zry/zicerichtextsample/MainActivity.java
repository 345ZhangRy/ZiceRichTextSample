package com.zry.zicerichtextsample;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zry.zicerichtext.ZiceRichTextEditor;
import com.zry.zicerichtext.mention.model.User;
import com.zry.zicerichtext.utils.MyGlideEngine;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ZiceRichTextEditor mEditor;

    private TextView mShowTv;
    private Button Btn1, Btn2;
    private Button Btn11;

    private String showText;

    public static final int REQUEST_ALBUM = 1;

    private int AtColor = 0xff42ca6e;   //@颜色

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String imagePath = (String) msg.obj;
                    mEditor.insertImage(imagePath, mEditor.getMeasuredWidth());
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditor = findViewById(R.id.et_new_content);
        mShowTv = findViewById(R.id.tv_show);
        Btn1 = findViewById(R.id.btn_1);
        Btn2 = findViewById(R.id.btn_2);
        Btn11 = findViewById(R.id.btn_11);
        Btn1.setOnClickListener(this);
        Btn2.setOnClickListener(this);
        Btn11.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_1:
                User user = new User("10", "小燕子", AtColor);
                mEditor.insertMention(user);
                break;
            case R.id.btn_2:
//                new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE)
//                        .subscribe(granted -> {
//                            if (granted) {
                Matisse.from(MainActivity.this)
                        .choose(MimeType.ofAll())
                        .countable(true)
                        .maxSelectable(9)
                        .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                        .thumbnailScale(0.85f)
                        .imageEngine(new MyGlideEngine())
                        .forResult(REQUEST_ALBUM);
//                            } else {
//                                // At least one permission is denied
//                                Toast.makeText(this, "您没有授权该权限，请在设置中打开授权", Toast.LENGTH_SHORT).show();
//                            }
//                        });
                break;
            case R.id.btn_11:
                mShowTv.setText("");
                showText = mEditor.getAllLayoutText();
                mShowTv.setText(showText);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ALBUM:
                    insertImagesSync(data);
                    break;
            }
        }
    }

    private void insertImagesSync(Intent data) {
        mEditor.measure(0, 0);
        List<Uri> mSelected = Matisse.obtainResult(data);
        // 可以同时插入多张图片
        for (Uri imageUri : mSelected) {
            String imagePath = CommonUtils.getFilePathFromUri(this, imageUri);
            Bitmap bitmap = CommonUtils.getSmallBitmap(imagePath, CommonUtils.getScreenWidth(this), CommonUtils.getScreenHeight(this));//压缩图片
            imagePath = CommonUtils.saveToSdCard(bitmap);
            Message message = new Message();
            message.what = 1;
            message.obj = imagePath;
            handler.sendMessage(message);
        }
    }

}
