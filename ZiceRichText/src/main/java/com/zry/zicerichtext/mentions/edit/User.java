package com.zry.zicerichtext.mentions.edit;

import com.zry.zicerichtext.mentions.library.edit.listener.InsertData;
import com.zry.zicerichtext.mentions.library.model.FormatRange;

import java.io.Serializable;

public class User implements Serializable, InsertData {

    private final CharSequence userId;
    private final CharSequence userName;
    private int color;

    public User(CharSequence userId, CharSequence userName, int color) {
        this.userId = userId;
        this.userName = userName;
        this.color = color;
    }

    public CharSequence getUserId() {
        return userId;
    }

    public CharSequence getUserName() {
        return userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        User user = (User) o;

        if (userId != null ? !userId.equals(user.userId) : user.userId != null)
            return false;
        if (userName != null ? !userName.equals(user.userName) : user.userName != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        return result;
    }

    @Override
    public CharSequence charSequence() {
        return "@" + userName;
    }

    @Override
    public FormatRange.FormatData formatData() {
        return new UserConvert(this);
    }

    @Override
    public int color() {
        return color;
    }

    private class UserConvert implements FormatRange.FormatData {

        public static final String USER_FORMART = "<dmUser id=%s>%s</dmUser>";
        private final User user;

        public UserConvert(User user) {
            this.user = user;
        }

        @Override
        public CharSequence formatCharSequence() {
            return String.format(USER_FORMART, user.getUserId(), user.getUserName(), user.getUserName());
        }

        @Override
        public CharSequence getUserId() {
            return user.getUserId();
        }
    }
}
