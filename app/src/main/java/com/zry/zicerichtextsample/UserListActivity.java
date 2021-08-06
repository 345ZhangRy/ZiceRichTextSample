package com.zry.zicerichtextsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zry.zicerichtext.mention.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zry on 2021/8/6.
 */
public class UserListActivity extends AppCompatActivity {

    private RecyclerView mUserRv;
    private UserAdapter mAdapter;
    private List<User> mUserList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        mUserRv = findViewById(R.id.list_user);
        mUserRv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new UserAdapter();
        mUserRv.setAdapter(mAdapter);

        User user0 = new User("0", "吴亦凡");
        User user1 = new User("1", "都美竹");
        User user2 = new User("2", "小G娜");

        mUserList = new ArrayList<>();
        mUserList.add(user0);
        mUserList.add(user1);
        mUserList.add(user2);
        mAdapter.setData(mUserList);
        mAdapter.notifyDataSetChanged();
    }

    class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserHolder> {

        private List<User> mUserList;

        public void setData(List<User> userList) {
            mUserList = userList;
        }

        @Override
        public UserHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_list, parent, false);
            return new UserHolder(view);
        }

        @Override
        public int getItemCount() {
            return mUserList.size();
        }

        @Override
        public void onBindViewHolder(UserHolder holder, int position) {
            holder.mTextView.setText(mUserList.get(position).getUserName());
            holder.mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(Activity.RESULT_OK, new Intent().putExtra("user", mUserList.get(position)));
                    finish();
                }
            });
        }

        class UserHolder extends RecyclerView.ViewHolder {

            private TextView mTextView;

            public UserHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.textView);
            }
        }
    }
}
