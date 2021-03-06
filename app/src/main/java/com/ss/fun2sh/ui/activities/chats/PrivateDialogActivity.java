package com.ss.fun2sh.ui.activities.chats;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.q_municate_core.core.command.Command;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_core.models.CombinationMessage;
import com.quickblox.q_municate_core.qb.commands.friend.QBAcceptFriendCommand;
import com.quickblox.q_municate_core.qb.commands.friend.QBRejectFriendCommand;
import com.quickblox.q_municate_core.service.QBService;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate_core.utils.OnlineStatusUtils;
import com.quickblox.q_municate_core.utils.UserFriendUtils;
import com.quickblox.q_municate_core.utils.helpers.CoreSharedHelper;
import com.quickblox.q_municate_db.managers.DataManager;
import com.quickblox.q_municate_db.managers.FriendDataManager;
import com.quickblox.q_municate_db.models.Dialog;
import com.quickblox.q_municate_db.models.State;
import com.quickblox.q_municate_db.models.User;
import com.quickblox.q_municate_db.utils.ErrorUtils;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCTypes;
import com.ss.fun2sh.CRUD.Utility;
import com.ss.fun2sh.R;
import com.ss.fun2sh.oldutils.Constants;
import com.ss.fun2sh.ui.activities.groupcall.activities.GroupCallActivity;
import com.ss.fun2sh.ui.activities.profile.UserProfileActivity;
import com.ss.fun2sh.ui.adapters.chats.PrivateDialogMessagesAdapter;
import com.ss.fun2sh.ui.fragments.dialogs.base.TwoButtonsDialogFragment;
import com.ss.fun2sh.utils.DateUtils;
import com.ss.fun2sh.utils.ToastUtils;
import com.ss.fun2sh.utils.listeners.FriendOperationListener;
import com.ss.fun2sh.utils.listeners.simple.SimpleOnRecycleItemClickListener;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import butterknife.OnClick;

public class PrivateDialogActivity extends BaseDialogActivity {

    private FriendOperationAction friendOperationAction;
    private FriendObserver friendObserver;
    private int operationItemPosition;
    private final String TAG = "PrivateDialogActivity";

    public static void start(Context context, User opponent, Dialog dialog) {
        Intent intent = new Intent(context, PrivateDialogActivity.class);
        intent.putExtra(QBServiceConsts.EXTRA_OPPONENT, opponent);
        intent.putExtra(QBServiceConsts.EXTRA_DIALOG, dialog);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFields();

        if (dialog == null) {
            finish();
        }

        setUpActionBarWithUpButton();

        if (isNetworkAvailable()) {
            deleteTempMessages();
        }

        addObservers();
        initMessagesRecyclerView();
    }

    @Override
    protected void addActions() {
        super.addActions();

        addAction(QBServiceConsts.ACCEPT_FRIEND_SUCCESS_ACTION, new AcceptFriendSuccessAction());
        addAction(QBServiceConsts.ACCEPT_FRIEND_FAIL_ACTION, failAction);
        addAction(QBServiceConsts.REJECT_FRIEND_SUCCESS_ACTION, new RejectFriendSuccessAction());
        addAction(QBServiceConsts.REJECT_FRIEND_FAIL_ACTION, failAction);

        updateBroadcastActionList();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForCorrectChat();

        if (isNetworkAvailable()) {
            startLoadDialogMessages();
        }

        checkMessageSendingPossibility();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteObservers();
    }

    @Override
    protected void updateActionBar() {
        setOnlineStatus(opponentUser);
        checkActionBarLogo(opponentUser.getAvatar(), R.drawable.placeholder_user);
    }

    @Override
    protected void onConnectServiceLocally(QBService service) {
        onConnectServiceLocally();
        setOnlineStatus(opponentUser);
    }

    @Override
    protected void onFileLoaded(QBFile file) {
        try {
            privateChatHelper.sendPrivateMessageWithAttachImage(file, opponentUser.getUserId());
        } catch (QBResponseException exc) {
            ErrorUtils.showError(this, "Unable to send file");
        }
    }

    @Override
    protected Bundle generateBundleToInitDialog() {
        Bundle bundle = new Bundle();
        bundle.putInt(QBServiceConsts.EXTRA_OPPONENT_ID, opponentUser.getUserId());
        return bundle;
    }

    @Override
    protected void initMessagesRecyclerView() {
        super.initMessagesRecyclerView();
        messagesAdapter = new PrivateDialogMessagesAdapter(this, friendOperationAction, combinationMessagesList, this, dialog);
        messagesRecyclerView.addItemDecoration(
                new StickyRecyclerHeadersDecoration((StickyRecyclerHeadersAdapter) messagesAdapter));
        findLastFriendsRequest();

        messagesRecyclerView.setAdapter(messagesAdapter);
        messagesAdapter.setOnRecycleItemClickListener(new SimpleOnRecycleItemClickListener<CombinationMessage>() {


            @Override
            public void onItemLongClicked(View view, CombinationMessage combinationMessage, int position) {
                boolean ownMessage = !combinationMessage.isIncoming(AppSession.getSession().getUser().getId());
                if (ownMessage) {
                    //own
                    if (State.DELIVERED.equals(combinationMessage.getState()) || State.READ.equals(combinationMessage.getState())) {
                        opponentMessage(combinationMessage);
                    } else {
                        ownMessage(combinationMessage);
                    }
                } else {
                    //opponent
                    opponentMessage(combinationMessage);
                }
            }
        });
        scrollMessagesToBottom();
    }

    @Override
    protected void updateMessagesList() {
        initActualExtras();
        checkForCorrectChat();

        int oldMessagesCount = messagesAdapter.getAllItems().size();

        this.combinationMessagesList = createCombinationMessagesList();
        //Log.d(TAG, "combinationMessagesList = " + combinationMessagesList);
        messagesAdapter.setList(combinationMessagesList);
        findLastFriendsRequest();

        checkForScrolling(oldMessagesCount);
    }

    private void initActualExtras() {
        opponentUser = (User) getIntent().getExtras().getSerializable(QBServiceConsts.EXTRA_OPPONENT);
        dialog = (Dialog) getIntent().getExtras().getSerializable(QBServiceConsts.EXTRA_DIALOG);
    }

    @Override
    public void notifyChangedUserStatus(int userId, boolean online) {
        super.notifyChangedUserStatus(userId, online);

        if (opponentUser != null && opponentUser.getUserId() == userId) {
            setOnlineStatus(opponentUser);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.private_dialog_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isFriend = DataManager.getInstance().getFriendDataManager().getByUserId(
                opponentUser.getUserId()) != null;
        if (!isFriend && item.getItemId() != android.R.id.home) {
            ToastUtils.longToast(R.string.dialog_user_is_not_friend);
            return true;
        }
        switch (item.getItemId()) {
            case R.id.action_audio_call:
                if (!dataManager.getUserDataManager().isBlocked(opponentUser.getUserId())) {
                    callToUser(opponentUser, QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO);
                    CoreSharedHelper.getInstance().savePref(Constants.AUDIOONETOONECALL, "true");
                } else {
                    Utility.blockContactMessage(this, "Unblock " + opponentUser.getFullName() + " to place a FunChat voice call", opponentUser.getUserId());
                }
                break;
            case R.id.switch_camera_toggle:
                if (!dataManager.getUserDataManager().isBlocked(opponentUser.getUserId())) {
                    callToUser(opponentUser, QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO);
                    CoreSharedHelper.getInstance().savePref(Constants.VIDEOONETOONECALL, "true");
                } else {
                    Utility.blockContactMessage(this, "Unblock " + opponentUser.getFullName() + " to place a FunChat video call", opponentUser.getUserId());
                }
                break;
            default:
                super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void checkMessageSendingPossibility() {
        boolean enable = dataManager.getFriendDataManager().existsByUserId(opponentUser.getUserId()) && isNetworkAvailable();
        checkMessageSendingPossibility(enable);
    }

    @OnClick(R.id.toolbar)
    void openProfile(View view) {
        UserProfileActivity.start(this, opponentUser.getUserId(),opponentUser);
    }

    private void initFields() {
        chatHelperIdentifier = QBService.PRIVATE_CHAT_HELPER;
        friendOperationAction = new FriendOperationAction();
        friendObserver = new FriendObserver();
        initActualExtras();
//        opponentUser = (User) getIntent().getExtras().getSerializable(QBServiceConsts.EXTRA_OPPONENT);
//        dialog = (Dialog) getIntent().getExtras().getSerializable(QBServiceConsts.EXTRA_DIALOG);
        combinationMessagesList = createCombinationMessagesList();
        title = opponentUser.getFullName();
    }

    private void addObservers() {
        dataManager.getFriendDataManager().addObserver(friendObserver);
    }

    private void deleteObservers() {
        dataManager.getFriendDataManager().deleteObserver(friendObserver);
    }

    private void findLastFriendsRequest() {
        ((PrivateDialogMessagesAdapter) messagesAdapter).findLastFriendsRequestMessagesPosition();
        messagesAdapter.notifyDataSetChanged();
    }

    private void setOnlineStatus(User user) {
        if (user != null) {
            if (friendListHelper != null) {
                String offlineStatus = getString(R.string.last_seen, DateUtils.toTodayYesterdayShortDateWithoutYear2(user.getLastLogin()),
                        DateUtils.formatDateSimpleTime(user.getLastLogin()));
                setActionBarSubtitle(
                        OnlineStatusUtils.getOnlineStatus(this, friendListHelper.isUserOnline(user.getUserId()), offlineStatus));
            }
        }
    }

    public void sendMessage(View view) {
        sendMessage(true);
    }

    private void callToUser(User user, QBRTCTypes.QBConferenceType qbConferenceType) {
        if (!isChatInitializedAndUserLoggedIn()) {
            ToastUtils.longToast(R.string.call_chat_service_is_initializing);
            return;
        }
        List<QBUser> qbUserList = new ArrayList<>(1);
        qbUserList.add(UserFriendUtils.createQbUser(user));
        GroupCallActivity.start(PrivateDialogActivity.this, qbUserList, qbConferenceType, null);

    }

    private void acceptUser(final int userId) {
        if (isNetworkAvailable()) {
            if (!isChatInitializedAndUserLoggedIn()) {
                ToastUtils.longToast(R.string.call_chat_service_is_initializing);
                return;
            }

            showProgress();
            QBAcceptFriendCommand.start(this, userId);
        } else {
            ToastUtils.longToast(R.string.dlg_fail_connection);
            return;
        }
    }

    private void rejectUser(final int userId) {
        if (isNetworkAvailable()) {
            if (!isChatInitializedAndUserLoggedIn()) {
                ToastUtils.longToast(R.string.call_chat_service_is_initializing);
                return;
            }
            showRejectUserDialog(userId);
        } else {
            ToastUtils.longToast(R.string.dlg_fail_connection);
            return;
        }
    }

    private void showRejectUserDialog(final int userId) {
        User user = DataManager.getInstance().getUserDataManager().get(userId);
        if (user == null) {
            return;
        }

        TwoButtonsDialogFragment.show(getSupportFragmentManager(),
                getString(R.string.dialog_message_reject_friend, user.getFullName()),
                new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        showProgress();
                        QBRejectFriendCommand.start(PrivateDialogActivity.this, userId);
                    }
                });
    }

    private void checkForCorrectChat() {
        Dialog updatedDialog = null;
        if (dialog != null) {
            updatedDialog = dataManager.getDialogDataManager().getByDialogId(dialog.getDialogId());
        } else {
            finish();
        }

        if (updatedDialog == null) {
            finish();
        } else {
            dialog = updatedDialog;
        }
    }

    private class FriendOperationAction implements FriendOperationListener {

        @Override
        public void onAcceptUserClicked(int position, int userId) {
            operationItemPosition = position;
            acceptUser(userId);
        }

        @Override
        public void onRejectUserClicked(int position, int userId) {
            operationItemPosition = position;
            rejectUser(userId);
        }
    }

    private class AcceptFriendSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            ((PrivateDialogMessagesAdapter) messagesAdapter).clearLastRequestMessagePosition();
            messagesAdapter.notifyItemChanged(operationItemPosition);
            startLoadDialogMessages();
            hideProgress();
        }
    }

    private class RejectFriendSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            ((PrivateDialogMessagesAdapter) messagesAdapter).clearLastRequestMessagePosition();
            messagesAdapter.notifyItemChanged(operationItemPosition);
            startLoadDialogMessages();
            hideProgress();
        }
    }

    private class FriendObserver implements Observer {

        @Override
        public void update(Observable observable, Object data) {
            if (data != null && data.equals(FriendDataManager.OBSERVE_KEY)) {
                checkForCorrectChat();
                checkMessageSendingPossibility();
            }
        }
    }
}