package com.ss.fun2sh.ui.adapters.chats;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.quickblox.q_municate_core.models.CombinationMessage;
import com.quickblox.q_municate_core.qb.commands.chat.QBUpdateStatusMessageCommand;
import com.quickblox.q_municate_core.utils.ChatUtils;
import com.quickblox.q_municate_db.managers.DataManager;
import com.quickblox.q_municate_db.models.Attachment;
import com.quickblox.q_municate_db.models.State;
import com.ss.fun2sh.CRUD.Const;
import com.ss.fun2sh.CRUD.M;
import com.ss.fun2sh.CRUD.NetworkUtil;
import com.ss.fun2sh.CRUD.Utility;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.base.BaseActivity;
import com.ss.fun2sh.ui.activities.chats.BaseDialogActivity;
import com.ss.fun2sh.ui.activities.main.MainActivity;
import com.ss.fun2sh.ui.adapters.base.BaseClickListenerViewHolder;
import com.ss.fun2sh.utils.DateUtils;

import java.io.File;
import java.util.List;

import static com.ss.fun2sh.CRUD.Utility.getDirectoryName;

public class FavouriteAdapter extends BaseDialogMessagesAdapter {


    public FavouriteAdapter(
            BaseActivity baseActivity,
            List<CombinationMessage> objectsList) {
        super(baseActivity, objectsList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case TYPE_OWN_MESSAGE:
                return new ViewHolder(this, layoutInflater.inflate(R.layout.item_message_own, viewGroup, false));
            case TYPE_OPPONENT_MESSAGE:
                return new ViewHolder(this, layoutInflater.inflate(R.layout.item_group_message_opponent, viewGroup, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(BaseClickListenerViewHolder<CombinationMessage> baseClickListenerViewHolder, final int position) {
        final CombinationMessage combinationMessage = getItem(position);
        final boolean ownMessage = !combinationMessage.isIncoming(currentUser.getId());

        final ViewHolder viewHolder = (ViewHolder) baseClickListenerViewHolder;


        String avatarUrl = combinationMessage.getDialogOccupant().getUser().getAvatar();
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                favOption(combinationMessage);
                return true;
            }
        });

        if (viewHolder.verticalProgressBar != null) {
            viewHolder.verticalProgressBar.setProgressDrawable(baseActivity.getResources().getDrawable(R.drawable.vertical_progressbar));
        }
        if (!ownMessage) {
            setFullName(combinationMessage, viewHolder);
        }
        if (combinationMessage.getAttachment() != null) {
            resetUI(viewHolder);
            if (combinationMessage.getAttachment().getType().equals(Attachment.Type.PICTURE)) {
                setViewVisibility(viewHolder.progressRelativeLayout, View.VISIBLE);
                displayAttachImageById(combinationMessage.getAttachment().getRemoteUrl(), viewHolder);
                displayAvatarImage(avatarUrl, viewHolder.avatarAttachImageView);
                viewHolder.timeAttachMessageTextView.setText(DateUtils.formatDateSimpleTime(combinationMessage.getCreatedDate()));

                if (ownMessage && combinationMessage.getState() != null) {
                    setMessageStatus(viewHolder.attachDeliveryStatusImageView, State.DELIVERED.equals(
                            combinationMessage.getState()), State.READ.equals(combinationMessage.getState()));
                }
                viewHolder.attachImageView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        favOption(combinationMessage);
                        return true;
                    }
                });
            } else {
                if (combinationMessage.getAttachment().getName() != null) {
                    setViewVisibility(viewHolder.attachOtherFileRelativeLayout, View.VISIBLE);
                    final String[] tokens = combinationMessage.getAttachment().getName().split("\\.(?=[^\\.]+$)");
                    viewHolder.fileName.setText(tokens[0]);
                    viewHolder.fileType.setText(tokens[1].toUpperCase());
                    final File file = new File(Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage), combinationMessage.getAttachment().getName());
                    final boolean check = file.exists();
                    if (check)
                        viewHolder.downloadButton.setText("OPEN");

                    viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //download file
                            if (!viewHolder.downloadButton.getText().equals("OPEN")) {
                                if (NetworkUtil.getConnectivityStatus(baseActivity)) {
                                    new DownloadFileAsync(getDirectoryName(combinationMessage), viewHolder, combinationMessage.getAttachment().getName()).execute(combinationMessage.getAttachment().getRemoteUrl());
                                } else {
                                    M.dError(baseActivity, "Unable to connect internet.");
                                    viewHolder.downloadButton.setText("DOWNLOAD");
                                }
                            } else {
                                openFile(tokens[1], file);
                            }
                        }
                    });

                    displayAvatarImage(avatarUrl, viewHolder.avatarOtherAttaheImageView);
                    viewHolder.timeOtherAttachMessageTextView.setText(DateUtils.formatDateSimpleTime(combinationMessage.getCreatedDate()));

                    if (ownMessage && combinationMessage.getState() != null) {
                        setMessageStatus(viewHolder.otherAttachDeliveryStatusImageView, State.DELIVERED.equals(
                                combinationMessage.getState()), State.READ.equals(combinationMessage.getState()));
                    }
                }
            }
        } else {
            resetUI(viewHolder);

            setViewVisibility(viewHolder.textMessageView, View.VISIBLE);
            viewHolder.messageTextView.setText(combinationMessage.getBody());
            viewHolder.timeTextMessageTextView.setText(DateUtils.formatDateSimpleTime(combinationMessage.getCreatedDate()));

            if (ownMessage && combinationMessage.getState() != null) {
                setMessageStatus(viewHolder.messageDeliveryStatusImageView, State.DELIVERED.equals(
                        combinationMessage.getState()), State.READ.equals(combinationMessage.getState()));
            } else if (ownMessage && combinationMessage.getState() == null) {
                viewHolder.messageDeliveryStatusImageView.setImageResource(android.R.color.transparent);
            }
            displayAvatarImage(avatarUrl, viewHolder.avatarImageView);
        }

        if (!State.READ.equals(combinationMessage.getState()) && !ownMessage && baseActivity.isNetworkAvailable()) {
            combinationMessage.setState(State.READ);
            QBUpdateStatusMessageCommand.start(baseActivity, ChatUtils.createQBDialogFromLocalDialog(dataManager, dialog), combinationMessage, true);
        }

    }

    private void favOption(final CombinationMessage combinationMessage) {
        new MaterialDialog.Builder(baseActivity)
                .title(R.string.new_message_select_option)
                .items(R.array.new_messages_fav)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            if (DataManager.getInstance().getMessageDataManager().updateFav(combinationMessage.getMessageId(), 0) > 0) {
                                removeItem(combinationMessage);
                                M.T(baseActivity, "Removed from favourite");
                            } else {
                                M.E("Error in remove to favourite");
                            }
                        } else if (which == 1) {
                            //Copy
                            if (combinationMessage.getAttachment() != null) {
                                //forward images or file ka code
                                Const.FORWARD_MESSAGE = null;
                                Const.FORWARD_MESSAGE = Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage) + combinationMessage.getAttachment().getName();
                                MainActivity.start(baseActivity);
                            } else {
                                Utility.msgInClipBoard(baseActivity, combinationMessage.getBody());
                            }
                        }
                    }
                })
                .show();
    }

    @Override
    public int getItemViewType(int position) {
        return getItemViewType(getItem(position));
    }

}