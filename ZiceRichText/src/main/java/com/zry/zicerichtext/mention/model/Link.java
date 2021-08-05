package com.zry.zicerichtext.mention.model;

import com.zry.zicerichtext.mention.util.InsertData;
import com.zry.zicerichtext.range.FormatRange;

import java.io.Serializable;

/**
 * Created by zry on 2021/7/29.
 */
public class Link implements Serializable, InsertData {

    private CharSequence mUrl;
    private CharSequence mText;
    private int mColor;

    public Link(CharSequence url, CharSequence text, int color) {
        mUrl = url;
        mText = text;
        mColor = color;
    }

    public CharSequence getUrl() {
        return mUrl;
    }

    public CharSequence getText() {
        return mText;
    }

    @Override
    public CharSequence charSequence() {
        return "#" + mText;
    }

    @Override
    public FormatRange.FormatData formatData() {
        return new LinkConvert(this);
    }

    @Override
    public int color() {
        return mColor;
    }

    public static class LinkConvert implements FormatRange.FormatData {

        private static final String LINK_FORMAT = "<ZiceLink url=%s>%s</ZiceLink>";
        private final Link mLink;

        public LinkConvert(Link link) {
            mLink = link;
        }

        @Override
        public CharSequence formatCharSequence() {
            return String.format(LINK_FORMAT, mLink.getUrl(), mLink.getText());
        }

        @Override
        public CharSequence formatParam() {
            return mLink.getUrl();
        }
    }
}
