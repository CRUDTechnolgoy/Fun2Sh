package com.ss.fun2sh.ui.adapters.chats;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.quickblox.q_municate_core.models.CombinationMessage;
import com.quickblox.q_municate_core.qb.commands.chat.QBUpdateStatusMessageCommand;
import com.quickblox.q_municate_core.utils.ChatUtils;
import com.quickblox.q_municate_db.models.Attachment;
import com.quickblox.q_municate_db.models.Dialog;
import com.quickblox.q_municate_db.models.State;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.base.BaseActivity;
import com.ss.fun2sh.ui.adapters.base.BaseClickListenerViewHolder;
import com.ss.fun2sh.utils.DateUtils;
import com.ss.fun2sh.utils.FileUtils;
import com.ss.fun2sh.utils.listeners.ChatUIHelperListener;

import java.io.File;
import java.util.List;

public class GroupDialogMessagesAdapter extends BaseDialogMessagesAdapter {

    public GroupDialogMessagesAdapter(BaseActivity baseActivity, List<CombinationMessage> objectsList,
                                      ChatUIHelperListener chatUIHelperListener, Dialog dialog) {
        super(baseActivity, objectsList);
        this.chatUIHelperListener = chatUIHelperListener;
        this.dialog = dialog;
    }

    @Override
    public PrivateDialogMessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        switch (viewType) {
            case TYPE_REQUEST_MESSAGE:
                return new ViewHolder(this, layoutInflater.inflate(R.layout.item_notification_message, viewGroup, false));
            case TYPE_OWN_MESSAGE:
                return new ViewHolder(this, layoutInflater.inflate(R.layout.item_message_own, viewGroup, false));
            case TYPE_OPPONENT_MESSAGE:
                return new ViewHolder(this, layoutInflater.inflate(R.layout.item_group_message_opponent, viewGroup, false));
            default:
                return null;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return getItemViewType(getItem(position));
    }

    @Override
    public void onBindViewHolder(BaseClickListenerViewHolder<CombinationMessage> baseClickListenerViewHolder, int position) {
        ViewHolder viewHolder = (ViewHolder) baseClickListenerViewHolder;

        final CombinationMessage combinationMessage = getItem(position);
        boolean ownMessage = !combinationMessage.isIncoming(currentUser.getId());
        boolean notificationMessage = combinationMessage.getNotificationType() != null;

        String avatarUrl = null;
        String senderName;
        if (viewHolder.verticalProgressBar != null) {
            viewHolder.verticalProgressBar.setProgressDrawable(baseActivity.getResources().getDrawable(R.drawable.vertical_progressbar));
        }

        if (notificationMessage) {
            viewHolder.messageTextView.setText(combinationMessage.getBody());
            viewHolder.timeTextMessageTextView.setText(DateUtils.formatDateSimpleTime(combinationMessage.getCreatedDate()));
        } else {

            resetUI(viewHolder);

            if (ownMessage) {
                avatarUrl = combinationMessage.getDialogOccupant().getUser().getAvatar();
                ownMessage(combinationMessage, viewHolder);
            } else {
                senderName = combinationMessage.getDialogOccupant().getUser().getFullName();
                avatarUrl = combinationMessage.getDialogOccupant().getUser().getAvatar();
                setViewVisibility(viewHolder.nameTextView,View.VISIBLE);
                viewHolder.nameTextView.setTextColor(colorUtils.getRandomTextColorById(combinationMessage.getDialogOccupant().getUser().getUserId()));
                viewHolder.nameTextView.setText(senderName);
                opponentMessage(combinationMessage, viewHolder);
            }

            if (combinationMessage.getAttachment() != null) {
                if (combinationMessage.getAttachment().getType().equals(Attachment.Type.PICTURE)) {
                    setViewVisibility(viewHolder.progressRelativeLayout, View.VISIBLE);
                    displayAttachImageById(combinationMessage.getAttachment().getAttachmentId(), viewHolder);
                } else {
                    setViewVisibility(viewHolder.attachOtherFileRelativeLayout, View.VISIBLE);
                    if (combinationMessage.getAttachment().getName() != null) {
                        final String[] tokens = combinationMessage.getAttachment().getName().split("\\.(?=[^\\.]+$)");
                        viewHolder.fileName.setText(tokens[0]);
                        viewHolder.fileType.setText(tokens[1].toUpperCase());
                        final String directory;
                        if (combinationMessage.getAttachment().getType().equals(Attachment.Type.AUDIO)) {
                            directory = FileUtils.audioFolderName;
                        } else if (combinationMessage.getAttachment().getType().equals(Attachment.Type.VIDEO)) {
                            directory = FileUtils.videoFolderName;
                        } else if (combinationMessage.getAttachment().getType().equals(Attachment.Type.DOC)) {
                            directory = FileUtils.docFolderName;
                        } else {
                            directory = FileUtils.otherFolderName;
                        }
                        final File file = new File(Environment.getExternalStorageDirectory().toString() + directory, combinationMessage.getAttachment().getName());
                        final boolean check = file.exists();
                        if (check)
                            viewHolder.downloadButton.setText("OPEN");
                        viewHolder.downloadButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //download file
                                if (!check) {
                                    new DownloadFileAsync(directory).execute(combinationMessage.getAttachment().getRemoteUrl(), combinationMessage.getAttachment().getName());
                                } else {
                                    MimeTypeMap myMime = MimeTypeMap.getSingleton();
                                    Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                    String mimeType = myMime.getMimeTypeFromExtension(tokens[1]);
                                    newIntent.setDataAndType(Uri.fromFile(file), mimeType);
                                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    try {
                                        baseActivity.startActivity(newIntent);
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(baseActivity, "No handler for this type of file.", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                /*try {
                    String token;
                    String privateUrl;
                    token = QBAuth.getBaseService().getToken();
                    privateUrl = String.format("%s/blobs/%s?token=%s", BaseService.getServiceEndpointURL(), combinationMessage.getAttachment().getAttachmentId(), token);
                    M.E(privateUrl);
                } catch (BaseServiceException e) {
                    e.printStackTrace();
                }*/
                    }
                }
            } else {
                setViewVisibility(viewHolder.textMessageView, View.VISIBLE);
                viewHolder.timeTextMessageTextView.setText(
                        DateUtils.formatDateSimpleTime(combinationMessage.getCreatedDate()));
                viewHolder.messageTextView.setText(combinationMessage.getBody());
            }
        }

        if (!State.READ.equals(combinationMessage.getState()) && !ownMessage && baseActivity.isNetworkAvailable()) {
            combinationMessage.setState(State.READ);
            QBUpdateStatusMessageCommand.start(baseActivity,
                    ChatUtils.createQBDialogFromLocalDialog(dataManager, dialog), combinationMessage, false);
        } else if (ownMessage) {
            combinationMessage.setState(State.READ);
            dataManager.getMessageDataManager().update(combinationMessage.toMessage(), false);
        }

        displayAvatarImage(avatarUrl, viewHolder.avatarImageView);
    }
}