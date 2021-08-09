# ZiceRichTextSample
Android富文本编辑器，实现@某人、超链接、支持插入图片、自定义view，图文混排，支持任意位置删除、编辑
## 效果展示
![img](https://github.com/345ZhangRy/ZiceRichTextSample/blob/master/demo.gif)
## 使用方法
### 1、引入module
implementation project(':ZiceRichText')
### 2、在xml中添加ZiceRichTextEditor
    <com.zry.zicerichtext.ZiceRichTextEditor
        android:id="@+id/et_new_content"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        app:rt_editor_text_color="@color/text_color"
        app:rt_editor_text_init_hint="帖子正文描述(必填)"
        app:rt_editor_text_line_space="6dp"
        app:rt_editor_text_size="15sp" />
## 主要方法
### 插入一个@某人 
ZiceRichTextEditor.insertMention(User)
### 插入一个链接 
ZiceRichTextEditor.insertLink(Link)
### 插入一张普通图片 
ZiceRichTextEditor.insertImage(imagePath, imageWidth)
### 插入一个自定义view 
ZiceRichTextEditor.insertCustomView(customView extends ZiceCustomView)
### 查看当前编辑页所有内容标签
ZiceRichTextEditor.getAllLayoutText()
## 感谢
### 参考了大神的作品
https://github.com/sendtion/XRichText
