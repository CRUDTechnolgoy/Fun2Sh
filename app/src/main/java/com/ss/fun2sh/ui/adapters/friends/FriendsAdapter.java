package com.ss.fun2sh.ui.adapters.friends;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.HexagonImageView;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_core.qb.helpers.QBFriendListHelper;
import com.quickblox.q_municate_core.utils.OnlineStatusUtils;
import com.quickblox.q_municate_db.models.User;
import com.quickblox.users.model.QBUser;
import com.ss.fun2sh.CRUD.Utility;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.base.BaseActivity;
import com.ss.fun2sh.ui.adapters.base.BaseClickListenerViewHolder;
import com.ss.fun2sh.ui.adapters.base.BaseFilterAdapter;
import com.ss.fun2sh.ui.adapters.base.BaseViewHolder;
import com.ss.fun2sh.utils.DateUtils;
import com.ss.fun2sh.utils.helpers.TextViewHelper;

import java.util.List;

import butterknife.Bind;

public class FriendsAdapter extends BaseFilterAdapter<User, BaseClickListenerViewHolder<User>> {

    private boolean withFirstLetter;
    private QBFriendListHelper qbFriendListHelper;

    public FriendsAdapter(BaseActivity baseActivity, List<User> usersList, boolean withFirstLetter) {
        super(baseActivity, usersList);
        this.withFirstLetter = withFirstLetter;
    }

    @Override
    protected boolean isMatch(User item, String query) {
        return item.getFullName() != null && item.getFullName().toLowerCase().contains(query);
    }

    @Override
    public BaseClickListenerViewHolder<User> onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(this, layoutInflater.inflate(R.layout.item_friend, parent, false));
    }

    @Override
    public void onBindViewHolder(BaseClickListenerViewHolder<User> baseClickListenerViewHolder, final int position) {
        User user = getItem(position);
        ViewHolder viewHolder = (ViewHolder) baseClickListenerViewHolder;

        if (withFirstLetter) {
            initFirstLetter(viewHolder, position, user);
        } else {
            viewHolder.firstLatterTextView.setVisibility(View.GONE);
        }

        viewHolder.nameTextView.setText(Utility.capitalize(user.getFullName()));

        displayAvatarImage(user.getAvatar(), viewHolder.avatarImageView);

        if (!TextUtils.isEmpty(query)) {
            TextViewHelper.changeTextColorView(baseActivity, viewHolder.nameTextView, query);
        }

        setLabel(viewHolder, user);
    }

    public void setFriendListHelper(QBFriendListHelper qbFriendListHelper) {
        this.qbFriendListHelper = qbFriendListHelper;
        notifyDataSetChanged();
    }

    private void initFirstLetter(ViewHolder viewHolder, int position, User user) {
        if (TextUtils.isEmpty(user.getFullName())) {
            return;
        }

        viewHolder.firstLatterTextView.setVisibility(View.INVISIBLE);

        Character firstLatter = user.getFullName().toUpperCase().charAt(0);
        if (position == 0) {
            setLetterVisible(viewHolder, firstLatter);
        } else {
            Character beforeFirstLatter;
            User beforeUser = getItem(position - 1);
            if (beforeUser != null && beforeUser.getFullName() != null) {
                beforeFirstLatter = beforeUser.getFullName().toUpperCase().charAt(0);

                if (!firstLatter.equals(beforeFirstLatter)) {
                    setLetterVisible(viewHolder, firstLatter);
                }
            }
        }
    }

    private void setLetterVisible(ViewHolder viewHolder, Character character) {
        viewHolder.firstLatterTextView.setText(String.valueOf(character));
        viewHolder.firstLatterTextView.setVisibility(View.VISIBLE);
    }

    private void setLabel(ViewHolder viewHolder, User user) {
        boolean online = qbFriendListHelper != null && qbFriendListHelper.isUserOnline(user.getUserId());

        if (isMe(user)) {
            online = true;
        }

        if (online) {
            viewHolder.labelTextView.setText(OnlineStatusUtils.getOnlineStatus(online));
            viewHolder.labelTextView.setTextColor(baseActivity.getResources().getColor(R.color.green));
        } else {
            viewHolder.labelTextView.setText(baseActivity.getString(R.string.last_seen,
                    DateUtils.toTodayYesterdayShortDateWithoutYear2(user.getLastLogin()),
                    DateUtils.formatDateSimpleTime(user.getLastLogin())));
            viewHolder.labelTextView.setTextColor(baseActivity.getResources().getColor(R.color.dark_gray));
        }
    }

    private boolean isMe(User inputUser) {
        QBUser currentUser = AppSession.getSession().getUser();
        return currentUser.getId() == inputUser.getUserId();
    }

    protected static class ViewHolder extends BaseViewHolder<User> {

        @Bind(R.id.first_latter_textview)
        TextView firstLatterTextView;

        @Bind(R.id.avatar_imageview)
        HexagonImageView avatarImageView;

        @Bind(R.id.name_textview)
        TextView nameTextView;

        @Bind(R.id.label_textview)
        TextView labelTextView;

        public ViewHolder(FriendsAdapter adapter, View view) {
            super(adapter, view);
        }
    }
}