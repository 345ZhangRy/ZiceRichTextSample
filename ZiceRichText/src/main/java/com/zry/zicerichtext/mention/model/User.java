package com.zry.zicerichtext.mention.model;

import com.zry.zicerichtext.mention.util.InsertData;
import com.zry.zicerichtext.range.FormatRange;

import java.io.Serializable;

public class User implements Serializable, InsertData {

    private final CharSequence mUserId;
    private final CharSequence mUserName;
    private final int mColor;

    public User(CharSequence userId, CharSequence userName, int color) {
        this.mUserId = userId;
        this.mUserName = userName;
        this.mColor = color;
    }

    public CharSequence getUserId() {
        return mUserId;
    }

    public CharSequence getUserName() {
        return mUserName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        if (mUserId == null || user.getUserId() == null || mUserName == null || user.getUserName() == null) {
            return false;
        }
        return mUserId.equals(user.getUserId()) && mUserName.equals(user.getUserName());
    }

    @Override
    public CharSequence charSequence() {
        return "@" + mUserName;
    }

    @Override
    public FormatRange.FormatData formatData() {
        return new UserConvert(this);
    }

    @Override
    public int color() {
        return mColor;
    }

    public static class UserConvert implements FormatRange.FormatData {

        private static final String USER_FORMAT = "<ZiceUser id=%s>%s</ZiceUser>";
        private final User mUser;

        public UserConvert(User user) {
            this.mUser = user;
        }

        @Override
        public CharSequence formatCharSequence() {
            return String.format(USER_FORMAT, mUser.getUserId(), mUser.getUserName());
        }

        @Override
        public CharSequence formatParam() {
            return mUser.getUserId();
        }
    }
}
