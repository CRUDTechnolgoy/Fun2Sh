package com.ss.fun2sh.ui.activities.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.siyamed.shapeimageview.HexagonImageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.q_municate_core.core.command.Command;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate_core.utils.UserFriendUtils;
import com.quickblox.q_municate_db.models.User;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.authorization.LoginActivity;
import com.ss.fun2sh.ui.activities.base.BaseLoggableActivity;
import com.ss.fun2sh.ui.activities.changepassword.ChangePasswordActivity;
import com.ss.fun2sh.ui.activities.feedback.FeedbackActivity;
import com.ss.fun2sh.ui.activities.invitefriends.InviteFriendsActivity;
import com.ss.fun2sh.ui.activities.profile.MyProfileActivity;
import com.ss.fun2sh.ui.fragments.dialogs.base.TwoButtonsDialogFragment;
import com.ss.fun2sh.utils.ToastUtils;
import com.ss.fun2sh.utils.image.ImageLoaderUtils;

import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class SettingsActivity extends BaseLoggableActivity {

    @Bind(R.id.avatar_imageview)
    HexagonImageView avatarImageView;

    @Bind(R.id.full_name_edittext)
    TextView fullNameTextView;

    @Bind(R.id.push_notification_switch)
    SwitchCompat pushNotificationSwitch;

    @Bind(R.id.change_password_view)
    RelativeLayout changePasswordView;

    private User user;

    public static void start(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int getContentResId() {
        return R.layout.activity_settings;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFields();
        setUpActionBarWithUpButton();

        addActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIData();
    }

    private void updateUIData() {
        user = UserFriendUtils.createLocalUser(AppSession.getSession().getUser());
        fillUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeActions();
    }

    @OnClick(R.id.edit_profile_imagebutton)
    void editProfile() {
        MyProfileActivity.start(this);
    }

    @OnCheckedChanged(R.id.push_notification_switch)
    void enablePushNotification(boolean enable) {
        appSharedHelper.saveEnablePushNotifications(enable);
    }

    @OnClick(R.id.invite_friends_button)
    void inviteFriends() {
        InviteFriendsActivity.start(this);
    }

    @OnClick(R.id.give_feedback_button)
    void giveFeedback() {
        FeedbackActivity.start(this);
    }

    @OnClick(R.id.change_password_button)
    void changePassword() {
        ChangePasswordActivity.start(this);
    }

    @OnClick(R.id.logout_button)
    void logout() {
        if (checkNetworkAvailableWithError()) {
            TwoButtonsDialogFragment
                    .show(getSupportFragmentManager(), R.string.dlg_logout, R.string.dlg_confirm,
                            new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);
                                    showProgress();
                                    LoginActivity.start(SettingsActivity.this);
                                }
                            });
        }
    }

    @OnClick(R.id.delete_my_account_button)
    void deleteAccount() {
        ToastUtils.longToast(R.string.coming_soon);
    }

    private void initFields() {
        title = getString(R.string.settings_title);
        user = UserFriendUtils.createLocalUser(AppSession.getSession().getUser());
    }

    private void fillUI() {
        pushNotificationSwitch.setChecked(appSharedHelper.isEnablePushNotifications());
        //changePasswordView.setVisibility(        LoginType.FACEBOOK.equals(AppSession.getSession().getLoginType()) ? View.GONE : View.VISIBLE);
        fullNameTextView.setText(user.getFullName());

        showUserAvatar();
    }

    private void showUserAvatar() {
        ImageLoader.getInstance().displayImage(
                user.getAvatar(),
                avatarImageView,
                ImageLoaderUtils.UIL_USER_AVATAR_DISPLAY_OPTIONS);
    }

    private void addActions() {
        addAction(QBServiceConsts.LOGOUT_SUCCESS_ACTION, new LogoutSuccessAction());
        addAction(QBServiceConsts.LOGOUT_FAIL_ACTION, failAction);

        updateBroadcastActionList();
    }

    private void removeActions() {
        removeAction(QBServiceConsts.LOGOUT_SUCCESS_ACTION);
        removeAction(QBServiceConsts.LOGOUT_FAIL_ACTION);

        updateBroadcastActionList();
    }

    private class LogoutSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            startLandingScreen();
        }
    }
}