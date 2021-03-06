package com.ss.fun2sh.ui.adapters.chats;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.github.siyamed.shapeimageview.HexagonImageView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.quickblox.auth.QBAuth;
import com.quickblox.core.exception.BaseServiceException;
import com.quickblox.core.server.BaseService;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_core.models.CombinationMessage;
import com.quickblox.q_municate_core.utils.ConstsCore;
import com.quickblox.q_municate_db.managers.DataManager;
import com.quickblox.q_municate_db.models.Attachment;
import com.quickblox.q_municate_db.models.Dialog;
import com.quickblox.users.model.QBUser;
import com.ss.fun2sh.CRUD.Const;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.base.BaseActivity;
import com.ss.fun2sh.ui.activities.chats.BaseDialogActivity;
import com.ss.fun2sh.ui.activities.others.PreviewImageActivity;
import com.ss.fun2sh.ui.adapters.base.BaseClickListenerViewHolder;
import com.ss.fun2sh.ui.adapters.base.BaseRecyclerViewAdapter;
import com.ss.fun2sh.ui.adapters.base.BaseViewHolder;
import com.ss.fun2sh.ui.views.maskedimageview.MaskedImageView;
import com.ss.fun2sh.utils.ColorUtils;
import com.ss.fun2sh.utils.DateUtils;
import com.ss.fun2sh.utils.FileUtils;
import com.ss.fun2sh.utils.image.ImageLoaderUtils;
import com.ss.fun2sh.utils.image.ImageUtils;
import com.ss.fun2sh.utils.listeners.ChatUIHelperListener;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import butterknife.Bind;

import static android.os.Environment.getExternalStorageDirectory;

public abstract class BaseDialogMessagesAdapter
        extends BaseRecyclerViewAdapter<CombinationMessage, BaseClickListenerViewHolder<CombinationMessage>> implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    protected static final int TYPE_REQUEST_MESSAGE = 0;
    protected static final int TYPE_OWN_MESSAGE = 1;
    protected static final int TYPE_OPPONENT_MESSAGE = 2;

    protected DataManager dataManager;
    protected ChatUIHelperListener chatUIHelperListener;
    protected ImageUtils imageUtils;
    protected Dialog dialog;
    protected ColorUtils colorUtils;
    protected QBUser currentUser;

    private FileUtils fileUtils;
    //notification builder


    public BaseDialogMessagesAdapter(BaseActivity baseActivity, List<CombinationMessage> objectsList) {
        super(baseActivity, objectsList);
        dataManager = DataManager.getInstance();
        imageUtils = new ImageUtils(baseActivity);
        colorUtils = new ColorUtils();
        fileUtils = new FileUtils();
        currentUser = AppSession.getSession().getUser();
    }

    @Override
    public long getHeaderId(int position) {
        CombinationMessage combinationMessage = getItem(position);
        return DateUtils.toShortDateLong(combinationMessage.getCreatedDate());
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View view = layoutInflater.inflate(R.layout.item_chat_sticky_header_date, parent, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {
        View view = holder.itemView;

        TextView headerTextView = (TextView) view.findViewById(R.id.header_date_textview);
        CombinationMessage combinationMessage = getItem(position);
        headerTextView.setText(DateUtils.toTodayYesterdayFullMonthDate(combinationMessage.getCreatedDate()));
    }

    protected void displayAttachImageById(String attachId, final ViewHolder viewHolder) {
       /* *//*String token;*/
        String privateUrl;
        try {
            //   token = QBAuth.getBaseService().getToken();
            privateUrl = attachId;//String.format("%s/blobs/%s?token=%s", BaseService.getServiceEndpointURL(), attachId, token);
            displayAttachImage(privateUrl, viewHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        TODO it is necessary to convert the following code after the update to version 2.5 SDK
//        String privateUrl = QBFile.getPrivateUrlForUID(attachId);
//        displayAttachImage(privateUrl, viewHolder);
    }


    protected void displayAttachImage(String attachUrl, final ViewHolder viewHolder) {
        ImageLoader.getInstance().displayImage(attachUrl, viewHolder.attachImageView,
                ImageLoaderUtils.UIL_DEFAULT_DISPLAY_OPTIONS, new ImageLoadingListener(viewHolder),
                new SimpleImageLoadingProgressListener(viewHolder));
    }

    protected int getMessageStatusIconId(boolean isDelivered, boolean isRead) {
        int iconResourceId = 0;

        if (isRead) {
            iconResourceId = R.drawable.ic_status_mes_sent_received;
        } else if (isDelivered) {
            iconResourceId = R.drawable.ic_status_mes_sent;
        }

        return iconResourceId;
    }


    protected void setMessageStatus(ImageView imageView, boolean messageDelivered, boolean messageRead) {
        imageView.setImageResource(getMessageStatusIconId(messageDelivered, messageRead));
    }

    protected int getItemViewType(CombinationMessage combinationMessage) {
        boolean ownMessage = !combinationMessage.isIncoming(currentUser.getId());
        if (combinationMessage.getNotificationType() == null) {
            if (ownMessage) {
                return TYPE_OWN_MESSAGE;
            } else {
                return TYPE_OPPONENT_MESSAGE;
            }
        } else {
            return TYPE_REQUEST_MESSAGE;
        }
    }

    protected void resetUI(ViewHolder viewHolder) {
        setViewVisibility(viewHolder.attachMessageRelativeLayout, View.GONE);
        setViewVisibility(viewHolder.progressRelativeLayout, View.GONE);
        setViewVisibility(viewHolder.textMessageView, View.GONE);
        setViewVisibility(viewHolder.attachOtherFileRelativeLayout, View.GONE);
    }

    public void setFullName(CombinationMessage combinationMessage, ViewHolder viewHolder) {
        String senderName = combinationMessage.getDialogOccupant().getUser().getFullName();
        if (combinationMessage.getAttachment() != null) {
            if (combinationMessage.getAttachment().getType().equals(Attachment.Type.PICTURE)) {
                setViewVisibility(viewHolder.nameTextViewAttach, View.VISIBLE);
                viewHolder.nameTextViewAttach.setTextColor(colorUtils.getRandomTextColorById(combinationMessage.getDialogOccupant().getUser().getUserId()));
                viewHolder.nameTextViewAttach.setText(senderName);
            } else {
                setViewVisibility(viewHolder.nameTextViewOtherAttach, View.VISIBLE);
                viewHolder.nameTextViewOtherAttach.setTextColor(colorUtils.getRandomTextColorById(combinationMessage.getDialogOccupant().getUser().getUserId()));
                viewHolder.nameTextViewOtherAttach.setText(senderName);
            }
        } else {
            setViewVisibility(viewHolder.nameTextView, View.VISIBLE);
            viewHolder.nameTextView.setTextColor(colorUtils.getRandomTextColorById(combinationMessage.getDialogOccupant().getUser().getUserId()));
            viewHolder.nameTextView.setText(senderName);
        }
    }


    //    @Override
    //    public void onAbsolutePathExtFileReceived(String absolutePath) {
    //        chatUIHelperListener.onScreenResetPossibilityPerformLogout(false);
    //        imageUtils.showFullImage((android.app.Activity) context, absolutePath);
    //    }

    protected void setViewVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    protected static class ViewHolder extends BaseViewHolder<CombinationMessage> {

        @Nullable
        @Bind(R.id.avatar_imageview)
        HexagonImageView avatarImageView;

        @Nullable
        @Bind(R.id.avatar_imageview_attach)
        HexagonImageView avatarAttachImageView;

        @Nullable
        @Bind(R.id.avatar_imageview_otherattach)
        HexagonImageView avatarOtherAttaheImageView;

        @Nullable
        @Bind(R.id.name_textview)
        TextView nameTextView;

        @Nullable
        @Bind(R.id.name_textview_attach)
        TextView nameTextViewAttach;

        @Nullable
        @Bind(R.id.name_textview_otherattach)
        TextView nameTextViewOtherAttach;

        @Nullable
        @Bind(R.id.text_message_view)
        View textMessageView;

        @Nullable
        @Bind(R.id.text_message_delivery_status_imageview)
        ImageView messageDeliveryStatusImageView;

        @Nullable
        @Bind(R.id.attach_message_delivery_status_imageview)
        ImageView attachDeliveryStatusImageView;

        @Nullable
        @Bind(R.id.otherattach_message_delivery_status_imageview)
        ImageView otherAttachDeliveryStatusImageView;

        @Nullable
        @Bind(R.id.progress_relativelayout)
        RelativeLayout progressRelativeLayout;

        @Nullable
        @Bind(R.id.attach_message_relativelayout)
        LinearLayout attachMessageRelativeLayout;

        @Nullable
        @Bind(R.id.attachotherfile_message_relativelayout)
        LinearLayout attachOtherFileRelativeLayout;

        @Nullable
        @Bind(R.id.message_textview)
        TextView messageTextView;

        @Nullable
        @Bind(R.id.attach_imageview)
        MaskedImageView attachImageView;

        @Bind(R.id.time_text_message_textview)
        TextView timeTextMessageTextView;

        @Nullable
        @Bind(R.id.time_attach_message_textview)
        TextView timeAttachMessageTextView;

        @Nullable
        @Bind(R.id.time_otherattach_message_textview)
        TextView timeOtherAttachMessageTextView;

        @Nullable
        @Bind(R.id.vertical_progressbar)
        ProgressBar verticalProgressBar;

        @Nullable
        @Bind(R.id.centered_progressbar)
        ProgressBar centeredProgressBar;

        @Nullable
        @Bind(R.id.accept_friend_imagebutton)
        ImageView acceptFriendImageView;

        @Nullable
        @Bind(R.id.divider_view)
        View dividerView;

        @Nullable
        @Bind(R.id.reject_friend_imagebutton)
        ImageView rejectFriendImageView;

        @Nullable
        @Bind(R.id.download_button)
        Button downloadButton;

        @Nullable
        @Bind(R.id.file_name)
        TextView fileName;

        @Nullable
        @Bind(R.id.file_type)
        TextView fileType;

        public ViewHolder(BaseDialogMessagesAdapter adapter, View view) {
            super(adapter, view);
        }

    }

    public class ImageLoadingListener extends SimpleImageLoadingListener {

        private ViewHolder viewHolder;
        private Bitmap loadedImageBitmap;
        private String imageUrl;

        public ImageLoadingListener(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }

        @Override
        public void onLoadingStarted(String imageUri, View view) {
            super.onLoadingStarted(imageUri, view);
            viewHolder.verticalProgressBar.setProgress(ConstsCore.ZERO_INT_VALUE);
            viewHolder.centeredProgressBar.setProgress(ConstsCore.ZERO_INT_VALUE);
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            updateUIAfterLoading();
            imageUrl = null;
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, final Bitmap loadedBitmap) {
            initMaskedImageView(loadedBitmap);
            //TODO karan save image
            fileUtils.checkExistsFile(imageUri, loadedBitmap);
            this.imageUrl = imageUri;
        }

        private void initMaskedImageView(Bitmap loadedBitmap) {
            loadedImageBitmap = loadedBitmap;
            viewHolder.attachImageView.setOnClickListener(receiveImageFileOnClickListener());
            viewHolder.attachImageView.setImageBitmap(loadedImageBitmap);
            setViewVisibility(viewHolder.attachMessageRelativeLayout, View.VISIBLE);
            setViewVisibility(viewHolder.attachImageView, View.VISIBLE);

            updateUIAfterLoading();
        }

        private void updateUIAfterLoading() {
            if (viewHolder.progressRelativeLayout != null) {
                setViewVisibility(viewHolder.progressRelativeLayout, View.GONE);
            }
        }

        private View.OnClickListener receiveImageFileOnClickListener() {
            return new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (imageUrl != null) {
                        Const.previewImage = ((BitmapDrawable) viewHolder.attachImageView.getDrawable()).getBitmap();
                        view.startAnimation(AnimationUtils.loadAnimation(baseActivity, R.anim.chat_attached_file_click));
                        PreviewImageActivity.start(baseActivity, imageUrl);
                    }
                }
            };
        }
    }

    public class SimpleImageLoadingProgressListener implements ImageLoadingProgressListener {

        private ViewHolder viewHolder;

        public SimpleImageLoadingProgressListener(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }

        @Override
        public void onProgressUpdate(String imageUri, View view, int current, int total) {
            viewHolder.verticalProgressBar.setProgress(Math.round(100.0f * current / total));
        }
    }

    //download file
    class DownloadFileAsync extends AsyncTask<String, Integer, String> {
        String foldername, fileName;
        ViewHolder viewHolder;
        ProgressDialog progress;

        public DownloadFileAsync(String folderName, ViewHolder viewHolder, String fileName) {
            this.foldername = folderName;
            this.viewHolder = viewHolder;
            this.fileName = fileName;
            progress = new ProgressDialog(baseActivity);
            progress.setMessage("Downloading :" + fileName);
            progress.setCanceledOnTouchOutside(false);
            progress.setCancelable(false);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setMax(100);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            new File(getExternalStorageDirectory().toString() + this.foldername).mkdirs();
            //baseActivity.showProgress();
            progress.show();

        }

        @Override
        protected String doInBackground(String... aurl) {
            int count;

            try {

                URL url = new URL(aurl[0]);
                String filename = fileName;
                File mypath = new File(getExternalStorageDirectory().toString(), foldername + filename);
                URLConnection conexion = url.openConnection();
                conexion.connect();
                int lenghtOfFile = conexion.getContentLength();
                //Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(mypath);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) ((total * 100) / lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
            }
            return null;

        }

        protected void onProgressUpdate(Integer... progress) {
            Log.d("ANDRO_ASYNC", "" + progress[0]);
            this.progress.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String unused) {
            // baseActivity.hideProgress();
            progress.dismiss();
            viewHolder.downloadButton.setText("OPEN");
            notifyDataSetChanged();
        }
    }

    protected void openFile(String token, File file) {
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String mimeType = myMime.getMimeTypeFromExtension(token);
        newIntent.setDataAndType(Uri.fromFile(file), mimeType);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            baseActivity.startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(baseActivity, "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
    }

}