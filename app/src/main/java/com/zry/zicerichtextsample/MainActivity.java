package com.zry.zicerichtextsample;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zry.zicerichtext.ZiceRichTextEditor;
import com.zry.zicerichtext.mention.model.Link;
import com.zry.zicerichtext.mention.model.User;
import com.zry.zicerichtext.utils.MyGlideEngine;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ZiceRichTextEditor mEditor;

    private Button mContentBtn;

    private String showText;

    public static final int REQUEST_ALBUM = 1;

    private ActivityResultLauncher<Intent> startActivity;

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
        mContentBtn = findViewById(R.id.btn_content);
        mContentBtn.setOnClickListener(this);

        startActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    Intent resultData = result.getData();
                    if (resultData != null) {
                        User user = (User) resultData.getSerializableExtra("user");
                        mEditor.insertMention(user);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mention:
                Intent intent = new Intent(MainActivity.this, UserListActivity.class);
                startActivity.launch(intent);
                break;
            case R.id.action_link:
                Link link = new Link("https://www.baidu.com", "震惊！...");
                mEditor.insertLink(link);
                break;
            case R.id.action_album:
                Matisse.from(MainActivity.this)
                        .choose(MimeType.ofAll())
                        .countable(true)
                        .maxSelectable(9)
                        .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                        .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
                        .thumbnailScale(0.85f)
                        .imageEngine(new MyGlideEngine())
                        .forResult(REQUEST_ALBUM);
                break;
            case R.id.action_custom:
                CustomView customView = new CustomView(this);
                customView.setViewId(1024);
                mEditor.insertCustomView(customView);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_content:
                showText = mEditor.getAllLayoutText();
                new AlertDialog.Builder(this).setMessage(showText).show();
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
