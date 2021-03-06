package com.ss.fun2sh.ui.activities.chats;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBAttachment;
import com.quickblox.chat.model.QBDialog;
import com.quickblox.chat.query.QueryDeleteMessages;
import com.quickblox.chat.request.QBMessageUpdateBuilder;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.q_municate_core.core.command.Command;
import com.quickblox.q_municate_core.core.loader.BaseLoader;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_core.models.CombinationMessage;
import com.quickblox.q_municate_core.qb.commands.QBLoadAttachFileCommand;
import com.quickblox.q_municate_core.qb.commands.chat.QBLoadDialogMessagesCommand;
import com.quickblox.q_municate_core.qb.helpers.QBBaseChatHelper;
import com.quickblox.q_municate_core.qb.helpers.QBGroupChatHelper;
import com.quickblox.q_municate_core.qb.helpers.QBPrivateChatHelper;
import com.quickblox.q_municate_core.service.QBService;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate_core.utils.ChatUtils;
import com.quickblox.q_municate_core.utils.ConstsCore;
import com.quickblox.q_municate_db.managers.DataManager;
import com.quickblox.q_municate_db.managers.DialogDataManager;
import com.quickblox.q_municate_db.managers.DialogNotificationDataManager;
import com.quickblox.q_municate_db.managers.MessageDataManager;
import com.quickblox.q_municate_db.models.Attachment;
import com.quickblox.q_municate_db.models.Dialog;
import com.quickblox.q_municate_db.models.DialogNotification;
import com.quickblox.q_municate_db.models.DialogOccupant;
import com.quickblox.q_municate_db.models.Message;
import com.quickblox.q_municate_db.models.State;
import com.quickblox.q_municate_db.models.User;
import com.quickblox.q_municate_db.utils.ErrorUtils;
import com.rockerhieu.emojicon.EmojiconGridFragment;
import com.rockerhieu.emojicon.EmojiconsFragment;
import com.rockerhieu.emojicon.emoji.Emojicon;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.ss.fun2sh.CRUD.Const;
import com.ss.fun2sh.CRUD.M;
import com.ss.fun2sh.CRUD.NetworkUtil;
import com.ss.fun2sh.CRUD.UserAccount;
import com.ss.fun2sh.CRUD.Utility;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.base.BaseLoggableActivity;
import com.ss.fun2sh.ui.activities.main.MainActivity;
import com.ss.fun2sh.ui.activities.others.PreviewImageActivity;
import com.ss.fun2sh.ui.adapters.base.BaseRecyclerViewAdapter;
import com.ss.fun2sh.utils.FileUtils;
import com.ss.fun2sh.utils.KeyboardUtils;
import com.ss.fun2sh.utils.helpers.ImagePickHelper;
import com.ss.fun2sh.utils.image.ImageLoaderUtils;
import com.ss.fun2sh.utils.image.ImageUtils;
import com.ss.fun2sh.utils.listeners.ChatUIHelperListener;
import com.ss.fun2sh.utils.listeners.OnImagePickedListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import cn.pedant.SweetAlert.SweetAlertDialog;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
import static com.ss.fun2sh.CRUD.Utility.getDirectoryName;

public abstract class BaseDialogActivity extends BaseLoggableActivity implements
        EmojiconGridFragment.OnEmojiconClickedListener, EmojiconsFragment.OnEmojiconBackspaceClickedListener,
        ChatUIHelperListener, OnImagePickedListener {

    private static final int TYPING_DELAY = 1000;
    private static final int DELAY_SCROLLING_LIST = 300;
    private static final int DELAY_SHOWING_SMILE_PANEL = 200;
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 555;
    private static final int ACTION_TAKE_VIDEO = 100;
    private static final int PICK_AUDIO_FILE = 7778;
    private static final int PICK_FILE = 52525;
    private static final int ACTION_RECORD_SOUND = 1436;
    protected Dialog dialog;
    protected Resources resources;
    protected DataManager dataManager;
    protected ImageUtils imageUtils;
    protected BaseRecyclerViewAdapter messagesAdapter;
    protected User opponentUser;
    protected QBBaseChatHelper baseChatHelper;
    protected List<CombinationMessage> combinationMessagesList;
    protected int chatHelperIdentifier;
    protected ImagePickHelper imagePickHelper;
    @Bind(R.id.messages_swiperefreshlayout)
    SwipeRefreshLayout messageSwipeRefreshLayout;
    @Bind(R.id.messages_recycleview)
    RecyclerView messagesRecyclerView;
    @Bind(R.id.message_edittext)
    EditText messageEditText;
    @Bind(R.id.attach_button)
    ImageButton attachButton;
    @Bind(R.id.send_button)
    ImageButton sendButton;
    @Bind(R.id.smile_panel_imagebutton)
    ImageButton smilePanelImageButton;
    @Bind(R.id.shimmer_tv)
    ShimmerTextView shimmer_tv;
    Shimmer shimmer;
    AnimatorSet set;
    private Handler mainThreadHandler;
    private View emojiconsFragment;
    private LoadAttachFileSuccessAction loadAttachFileSuccessAction;
    private LoadDialogMessagesSuccessAction loadDialogMessagesSuccessAction;
    private LoadDialogMessagesFailAction loadDialogMessagesFailAction;
    private Timer typingTimer;
    private boolean isTypingNow;
    private Observer dialogObserver;
    private Observer messageObserver;
    private Observer dialogNotificationObserver;
    private BroadcastReceiver typingMessageBroadcastReceiver;
    private BroadcastReceiver updatingDialogBroadcastReceiver;
    private boolean loadMore;
    //Audio Recarder
    private MediaRecorder recorder;
    boolean isRecarding;
    File sourceFile;

    //upload file
    private Uri fileUri;
    private String folderName;
    private File destFile;

    public static BaseDialogActivity baseDialogActivity;

    @Override
    protected int getContentResId() {
        return R.layout.activity_dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseDialogActivity = this;
        if (Const.FORWARD_MESSAGE != null) {
            M.E(Const.FORWARD_MESSAGE);
            sourceFile = new File(Const.FORWARD_MESSAGE);
            startLoadAttachFile(sourceFile);
        }
        initFields();
        shimmer = new Shimmer();
        shimmer.start(shimmer_tv);
        shimmer.setRepeatCount(99999999)
                .setDuration(2000)
                .setStartDelay(50)
                .setDirection(Shimmer.ANIMATION_DIRECTION_LTR)
                .setAnimatorListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
        shimmer_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shimmer_tv.setVisibility(View.GONE);
                messageEditText.setVisibility(View.VISIBLE);
                messageEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(messageEditText, InputMethodManager.SHOW_IMPLICIT);

                set = (AnimatorSet) AnimatorInflater.loadAnimator(BaseDialogActivity.this, R.animator.flipperanimation);
                set.setTarget(messageEditText);
                set.start();

            }
        });


        initCustomUI();
        initCustomListeners();
        addActions();
        addObservers();
        registerBroadcastReceivers();

        hideSmileLayout();

    }

    @OnTextChanged(R.id.message_edittext)
    void messageEditTextChanged(CharSequence charSequence) {
        setActionButtonVisibility(charSequence);

        // TODO: now it is possible only for Private chats
        if (dialog != null && Dialog.Type.PRIVATE.equals(dialog.getType())) {
            if (!isTypingNow) {
                isTypingNow = true;
                sendTypingStatus();
            }
            checkStopTyping();
        }
    }

    @OnTouch(R.id.message_edittext)
    boolean touchMessageEdit() {
        hideSmileLayout();
        scrollMessagesWithDelay();
        return false;
    }

    @OnClick(R.id.smile_panel_imagebutton)
    void smilePanelImageButtonClicked() {
        visibleOrHideSmilePanel();
        scrollMessagesWithDelay();
    }

    @OnClick(R.id.attach_button)
    void attachFile(View view) {
        if (Dialog.Type.PRIVATE.equals(dialog.getType())) {
            if (dataManager.getUserDataManager().isBlocked(opponentUser.getUserId())) {
                Utility.blockContactMessage(this, "Unblock " + opponentUser.getFullName() + " to send a attachement", opponentUser.getUserId());
                return;
            }
        }
        new MaterialDialog.Builder(this)
                .items(R.array.attach_file_option)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            //record video
                            Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                            // create a file to save the video
                            fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
                            // set the image file name
                            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                            // set the video image quality to high
                            cameraIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                            startActivityForResult(cameraIntent, ACTION_TAKE_VIDEO);
                        } else if (which == 1) {
                            //RECORD AUDIO
                            dialog.dismiss();
                            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View recardingView = inflater.inflate(R.layout.recoarding_layout, null);
                            final Button loginButton = (Button) recardingView.findViewById(R.id.loginButton);
                            ImageView close = (ImageView) recardingView.findViewById(R.id.close);
                            final Chronometer recardingTime = (Chronometer) recardingView.findViewById(R.id.chronometerRecarding);
                            final MaterialDialog recardingDialog = new MaterialDialog.Builder(BaseDialogActivity.this)
                                    .autoDismiss(false)
                                    .customView(recardingView, false)
                                    .show();
                            close.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (recorder != null) {
                                        recorder.stop();
                                        recorder.release();
                                        recardingTime.stop();
                                        isRecarding = false;
                                    }
                                    recardingDialog.dismiss();
                                }
                            });
                            loginButton.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (isRecarding) {
                                                recorder.stop();
                                                recorder.release();
                                                recardingTime.stop();
                                                addRecordingToMediaLibrary();
                                                isRecarding = false;
                                                recardingDialog.dismiss();
                                                startLoadAttachFile(sourceFile);
                                            } else {
                                                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                                                        .format(new java.util.Date().getTime());
                                                File dir = new File(Environment.getExternalStorageDirectory(), FileUtils.audioFolderName);
                                                if (!dir.exists()) {
                                                    if (!dir.mkdirs()) {
                                                        Toast.makeText(BaseDialogActivity.this, "Failed to create directory MyCameraVideo.",
                                                                Toast.LENGTH_LONG).show();
                                                        return;
                                                    }
                                                }

                                                try {
                                                    sourceFile = File.createTempFile("FUNCHAT_" + timeStamp + "", ".mp3", dir);
                                                    //Creating MediaRecorder and specifying audio source, output format, encoder & output format
                                                    recorder = new MediaRecorder();
                                                    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                                                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                                                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                                                    recorder.setOutputFile(sourceFile.getAbsolutePath());
                                                    recorder.prepare();
                                                    recorder.start();
                                                    recardingTime.start();
                                                    recardingTime.setFormat("Recordeing - %s");
                                                    isRecarding = true;
                                                    loginButton.setText("ATTACH");
                                                } catch (IOException e) {
                                                    Log.e("asa", "external storage access error");
                                                    return;
                                                }
                                            }
                                        }
                                    });
                        } else if (which == 2) {
                            //VIDEO FROM GALLERY
                            Intent intent = new Intent();
                            intent.setType("video/*");
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
                        } else if (which == 3) {
                            //AUDIO FROM SDCARD
                            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(i, PICK_AUDIO_FILE);
                        } else if (which == 4) {
                            //File FROM SDCARD
                            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                            i.setType("*/*");
                            startActivityForResult(i, PICK_FILE);
                        }
                    }
                })
                .show();

    }

    /**
     * Create a file Uri for saving an image or video
     */
    private Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        // Check that the SDCard is mounted
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), FileUtils.videoFolderName);
        // Create the storage directory(MyCameraVideo) if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Toast.makeText(BaseDialogActivity.this, "Failed to create directory MyCameraVideo.",
                        Toast.LENGTH_LONG).show();
                return null;
            }
        }
        // Create a media file name
        // For unique file name appending current timeStamp with file name
        java.util.Date date = new java.util.Date();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(date.getTime());
        File mediaFile;
        if (type == MEDIA_TYPE_VIDEO) {
            // For unique video file name appending current timeStamp with file name
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "FUNCHAT_" + timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }

    protected void addRecordingToMediaLibrary() {
        //creating content values of size 4
        ContentValues values = new ContentValues(4);
        long current = System.currentTimeMillis();
        values.put(MediaStore.Audio.Media.TITLE, "audio" + sourceFile.getName());
        values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
        values.put(MediaStore.Audio.Media.DATA, sourceFile.getAbsolutePath());

        //creating content resolver and storing it in the external content uri
        ContentResolver contentResolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri newUri = contentResolver.insert(base, values);

        //sending broadcast message to scan the media file so that it can be available
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
        //Toast.makeText(this, "Added File " + newUri, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // After camera screen this code will excuted

        if (requestCode == ACTION_TAKE_VIDEO) {
            if (resultCode == RESULT_OK) {
                sourceFile = new File(fileUri.getPath());
                startLoadAttachFile(sourceFile);
            }
        } else if (requestCode == REQUEST_TAKE_GALLERY_VIDEO && resultCode == RESULT_OK && data != null) {
            sendFile(data.getData());
        } else if (requestCode == PICK_AUDIO_FILE && resultCode == RESULT_OK && data != null) {
            fileUri = Uri.parse(getRealPathFromURI(data.getData(), 1));
            sourceFile = new File(fileUri.getPath());
            startLoadAttachFile(sourceFile);
        } else if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
//            fileUri = Uri.parse(getRealPathFromURI(data.getData(), 1));
            try {
                sourceFile = new File(Utility.getFilePath(BaseDialogActivity.this, data.getData()));
                startLoadAttachFile(sourceFile);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            //startLoadAttachFile(new File(getRealPathFromURI(data.getData(), 2)));
        }
    }

    private void sendFile(Uri selectedVideoUri) {
        try {
            if (Build.VERSION.SDK_INT > 19) {
                String wholeID = DocumentsContract.getDocumentId(selectedVideoUri);
                // Split at colon, use second item in the array
                String id = wholeID.split(":")[1];
                String[] column = {MediaStore.Video.Media.DATA};
                // where id is equal to
                String sel = MediaStore.Video.Media._ID + "=?";
                Cursor cursor = getContentResolver().
                        query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                column, sel, new String[]{id}, null);
                int columnIndex = cursor.getColumnIndex(column[0]);
                if (cursor.moveToFirst()) {
                    fileUri = Uri.parse(cursor.getString(columnIndex));
                }
                cursor.close();
                // OI FILE Manager
                //fileUri = getRealPathFromURI(selectedVideoUri);
            } else {
                fileUri = Uri.parse(getRealPathFromURI(selectedVideoUri, 0));
            }
            M.E(fileUri.toString());
            // MEDIA GALLERY
            sourceFile = new File(fileUri.getPath());
            startLoadAttachFile(sourceFile);
        } catch (Exception e) {
            M.E(e.getMessage());
        }
    }

    protected void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (destFile.exists()) {
            return;
        }
        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }
    }

    private String getRealPathFromURI(Uri contentUri, int type) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index;
        if (type == 0) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        } else if (type == 1) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        } else {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        }
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    @OnClick(R.id.camera_button)
    void attachCameraFile(View view) {
        if (Dialog.Type.PRIVATE.equals(dialog.getType())) {
            if (dataManager.getUserDataManager().isBlocked(opponentUser.getUserId())) {
                Utility.blockContactMessage(this, "Unblock " + opponentUser.getFullName() + " to send a attachment", opponentUser.getUserId());
                return;
            }
        }
        imagePickHelper.pickAnImage(BaseDialogActivity.this, ImageUtils.IMAGE_REQUEST_CODE);
    }

    @Override
    public void onBackPressed() {
        if (isSmilesLayoutShowing()) {
            hideSmileLayout();
        } else {
            super.onBackPressed();
            Const.FORWARD_MESSAGE = null;
            this.finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        createChatLocally();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSmileLayout();
        checkStartTyping();
    }

    @Override
    protected void onStop() {
        super.onStop();
        readAllMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeChatLocally();
        removeActions();
        deleteObservers();
        unregisterBroadcastReceivers();
    }

    @Override
    public void onConnectedToService(QBService service) {
        super.onConnectedToService(service);
        onConnectServiceLocally(service);
    }

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(messageEditText, emojicon);
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(messageEditText);
    }

    @Override
    public void onImagePicked(int requestCode, File file) {
        canPerformLogout.set(true);
        //TODO karan call for attache file
        startLoadAttachFile(file);
    }

    @Override
    public void onImagePickClosed(int requestCode) {
        canPerformLogout.set(true);
    }

    @Override
    public void onImagePickError(int requestCode, Exception e) {
        canPerformLogout.set(true);
        ErrorUtils.logError(e);
    }

    @Override
    public void onScreenResetPossibilityPerformLogout(boolean canPerformLogout) {
        this.canPerformLogout.set(canPerformLogout);
    }

    @Override
    protected void loadDialogs() {
        super.loadDialogs();
        createChatLocally();
        checkMessageSendingPossibility();
    }

    @Override
    protected void checkShowingConnectionError() {
        if (!isNetworkAvailable()) {
            setActionBarTitle(getString(R.string.dlg_internet_connection_is_missing));
            setActionBarIcon(null);
        } else {
            setActionBarTitle(title);
            updateActionBar();
        }
    }

    @Override
    protected void performLoginChatSuccessAction(Bundle bundle) {
        super.performLoginChatSuccessAction(bundle);
        if (groupChatHelper != null) {
            QBDialog qbDialog = ChatUtils.createQBDialogFromLocalDialog(dataManager, dialog);
            groupChatHelper.tryJoinRoomChat(qbDialog);
        }

        startLoadDialogMessages();
    }

    private void initFields() {
        mainThreadHandler = new Handler(Looper.getMainLooper());
        resources = getResources();
        dataManager = DataManager.getInstance();
        imageUtils = new ImageUtils(this);
        loadAttachFileSuccessAction = new LoadAttachFileSuccessAction();
        loadDialogMessagesSuccessAction = new LoadDialogMessagesSuccessAction();
        loadDialogMessagesFailAction = new LoadDialogMessagesFailAction();
        typingTimer = new Timer();
        dialogObserver = new DialogObserver();
        messageObserver = new MessageObserver();
        dialogNotificationObserver = new DialogNotificationObserver();
        typingMessageBroadcastReceiver = new TypingStatusBroadcastReceiver();
        updatingDialogBroadcastReceiver = new UpdatingDialogBroadcastReceiver();
        appSharedHelper.saveNeedToOpenDialog(false);
        imagePickHelper = new ImagePickHelper();
    }

    private void initCustomUI() {
        emojiconsFragment = _findViewById(R.id.emojicon_fragment);
    }

    private void initCustomListeners() {
        messageSwipeRefreshLayout.setOnRefreshListener(new RefreshLayoutListener());
    }

    protected void initMessagesRecyclerView() {
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    protected void addActions() {
        addAction(QBServiceConsts.LOAD_ATTACH_FILE_SUCCESS_ACTION, loadAttachFileSuccessAction);
        addAction(QBServiceConsts.LOAD_ATTACH_FILE_FAIL_ACTION, failAction);
        addAction(QBServiceConsts.LOAD_DIALOG_MESSAGES_SUCCESS_ACTION, loadDialogMessagesSuccessAction);
        addAction(QBServiceConsts.LOAD_DIALOG_MESSAGES_FAIL_ACTION, loadDialogMessagesFailAction);

        updateBroadcastActionList();
    }


    protected void removeActions() {
        removeAction(QBServiceConsts.LOAD_ATTACH_FILE_SUCCESS_ACTION);
        removeAction(QBServiceConsts.LOAD_ATTACH_FILE_FAIL_ACTION);

        removeAction(QBServiceConsts.LOAD_DIALOG_MESSAGES_SUCCESS_ACTION);
        removeAction(QBServiceConsts.LOAD_DIALOG_MESSAGES_FAIL_ACTION);

        removeAction(QBServiceConsts.ACCEPT_FRIEND_SUCCESS_ACTION);
        removeAction(QBServiceConsts.ACCEPT_FRIEND_FAIL_ACTION);

        removeAction(QBServiceConsts.REJECT_FRIEND_SUCCESS_ACTION);
        removeAction(QBServiceConsts.REJECT_FRIEND_FAIL_ACTION);

        updateBroadcastActionList();
    }

    private void registerBroadcastReceivers() {
        localBroadcastManager.registerReceiver(typingMessageBroadcastReceiver,
                new IntentFilter(QBServiceConsts.TYPING_MESSAGE));
        localBroadcastManager.registerReceiver(updatingDialogBroadcastReceiver,
                new IntentFilter(QBServiceConsts.UPDATE_DIALOG));
    }

    private void unregisterBroadcastReceivers() {
        localBroadcastManager.unregisterReceiver(updatingDialogBroadcastReceiver);
        localBroadcastManager.unregisterReceiver(typingMessageBroadcastReceiver);
    }

    private void addObservers() {
        dataManager.getDialogDataManager().addObserver(dialogObserver);
        dataManager.getMessageDataManager().addObserver(messageObserver);
        dataManager.getDialogNotificationDataManager().addObserver(dialogNotificationObserver);
    }

    private void deleteObservers() {
        dataManager.getDialogDataManager().deleteObserver(dialogObserver);
        dataManager.getMessageDataManager().deleteObserver(messageObserver);
        dataManager.getDialogNotificationDataManager().deleteObserver(dialogNotificationObserver);
    }

    protected void updateData() {
        dialog = dataManager.getDialogDataManager().getByDialogId(dialog.getDialogId());
        if (dialog != null) {
            updateActionBar();
        }
        updateMessagesList();
    }

    protected void onConnectServiceLocally() {
        createChatLocally();
    }

    private void showTypingStatus() {
        setActionBarSubtitle(R.string.dialog_now_typing);
    }

    private void hideTypingStatus() {
        setActionBarSubtitle(null);
    }

    protected void deleteTempMessages() {
        List<DialogOccupant> dialogOccupantsList = dataManager.getDialogOccupantDataManager()
                .getDialogOccupantsListByDialogId(dialog.getDialogId());
        List<Long> dialogOccupantsIdsList = ChatUtils.getIdsFromDialogOccupantsList(dialogOccupantsList);
        dataManager.getMessageDataManager().deleteTempMessages(dialogOccupantsIdsList);
    }

    private void hideSmileLayout() {
        emojiconsFragment.setVisibility(View.GONE);
        setSmilePanelIcon(R.drawable.ic_insert_emoticon_black_24dp);
    }

    private void showSmileLayout() {
        mainThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                emojiconsFragment.setVisibility(View.VISIBLE);
                setSmilePanelIcon(R.drawable.ic_keyboard_dark);
            }
        }, DELAY_SHOWING_SMILE_PANEL);
    }

    protected void checkActionBarLogo(String url, int resId) {
        if (!TextUtils.isEmpty(url)) {
            loadActionBarLogo(url);
        } else {
            setDefaultActionBarLogo(resId);
        }
    }

    protected void loadActionBarLogo(String logoUrl) {
        ImageLoader.getInstance().loadImage(logoUrl, ImageLoaderUtils.UIL_USER_AVATAR_DISPLAY_OPTIONS,
                new SimpleImageLoadingListener() {

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedBitmap) {
                        setActionBarIcon(
                                ImageUtils.getRoundIconDrawable(BaseDialogActivity.this, loadedBitmap));
                    }
                });
    }

    protected void setDefaultActionBarLogo(int drawableResId) {
        setActionBarIcon(ImageUtils
                .getRoundIconDrawable(this, BitmapFactory.decodeResource(getResources(), drawableResId)));
    }

    //TODO Karan send attachement
    protected void startLoadAttachFile(final File file) {
        SweetAlertDialog sweetAlertDialog = M.dConfirem(this, "FunChat", getString(R.string.dialog_confirm_sending_attach), "ATTACH", "NO");
        sweetAlertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                sweetAlertDialog.dismiss();
                showProgress();
                QBLoadAttachFileCommand.start(BaseDialogActivity.this, file);
            }
        });
        sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                Const.FORWARD_MESSAGE = null;
                sweetAlertDialog.dismiss();
            }
        });
    }

    protected void startLoadDialogMessages(Dialog dialog, long lastDateLoad) {
        QBLoadDialogMessagesCommand.start(this, ChatUtils.createQBDialogFromLocalDialog(dataManager, dialog),
                lastDateLoad, loadMore);
    }

    private void setActionButtonVisibility(CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence) || TextUtils.isEmpty(charSequence.toString().trim())) {
            sendButton.setVisibility(View.GONE);
            attachButton.setVisibility(View.VISIBLE);
        } else {
            sendButton.setVisibility(View.VISIBLE);
            attachButton.setVisibility(View.GONE);
        }
    }

    private void checkStopTyping() {
        typingTimer.cancel();
        typingTimer = new Timer();
        typingTimer.schedule(new TypingTimerTask(), TYPING_DELAY);
    }

    private void checkStartTyping() {
        // TODO: now it is possible only for Private chats
        if (dialog != null && Dialog.Type.PRIVATE.equals(dialog.getType())) {
            if (isTypingNow) {
                isTypingNow = false;
                sendTypingStatus();
            }
        }
    }

    private void sendTypingStatus() {
        baseChatHelper.sendTypingStatusToServer(opponentUser.getUserId(), isTypingNow);
    }

    private void setSmilePanelIcon(int resourceId) {
        smilePanelImageButton.setImageResource(resourceId);
    }

    private boolean isSmilesLayoutShowing() {
        return emojiconsFragment.getVisibility() == View.VISIBLE;
    }

    protected void checkForScrolling(int oldMessagesCount) {
        if (oldMessagesCount != messagesAdapter.getAllItems().size()) {
            scrollMessagesToBottom();
        }
    }

    protected void scrollMessagesToBottom() {
        if (!loadMore) {
            scrollMessagesWithDelay();
        }
    }

    private void scrollMessagesWithDelay() {
        mainThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                messagesRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1);
            }
        }, DELAY_SCROLLING_LIST);
    }

    protected void sendMessage(boolean privateMessage) {
        boolean error = false;
        try {
            if (privateMessage) {
                if (!dataManager.getUserDataManager().isBlocked(opponentUser.getUserId())) {
                    ((QBPrivateChatHelper) baseChatHelper).sendPrivateMessage(
                            messageEditText.getText().toString(), opponentUser.getUserId());
                } else {
                    Utility.blockContactMessage(this, "Unblock " + opponentUser.getFullName() + " to send a message", opponentUser.getUserId());
                }
            } else {
                ((QBGroupChatHelper) baseChatHelper).sendGroupMessage(dialog.getRoomJid(),
                        messageEditText.getText().toString());
            }
        } catch (QBResponseException e) {
            ErrorUtils.showError(this, e);
            error = true;
        } catch (IllegalStateException e) {
            ErrorUtils.showError(this, this.getString(
                    com.quickblox.q_municate_core.R.string.dlg_not_joined_room));
            error = true;
        } catch (Exception e) {
            ErrorUtils.showError(this, e);
            error = true;
        }

        if (!error) {
            messageEditText.setText(ConstsCore.EMPTY_STRING);
            scrollMessagesToBottom();
        }
    }

    protected void startLoadDialogMessages() {
        if (dialog == null) {
            return;
        }


        List<DialogOccupant> dialogOccupantsList = dataManager.getDialogOccupantDataManager().getDialogOccupantsListByDialogId(dialog.getDialogId());
        List<Long> dialogOccupantsIdsList = ChatUtils.getIdsFromDialogOccupantsList(dialogOccupantsList);

        Message message;
        DialogNotification dialogNotification;

        long messageDateSent = 0;

        if (loadMore) {
            message = dataManager.getMessageDataManager().getMessageByDialogId(true, dialogOccupantsIdsList);
            dialogNotification = dataManager.getDialogNotificationDataManager().getDialogNotificationByDialogId(true, dialogOccupantsIdsList);
            messageDateSent = ChatUtils.getDialogMessageCreatedDate(false, message, dialogNotification);
        } else {
            message = dataManager.getMessageDataManager().getMessageByDialogId(false, dialogOccupantsIdsList);
            dialogNotification = dataManager.getDialogNotificationDataManager().getDialogNotificationByDialogId(
                    false, dialogOccupantsIdsList);
            messageDateSent = ChatUtils.getDialogMessageCreatedDate(true, message, dialogNotification);
        }

        startLoadDialogMessages(dialog, messageDateSent);
    }

    private void readAllMessages() {
        if (dialog != null) {
            List<Message> messagesList = dataManager.getMessageDataManager()
                    .getMessagesByDialogId(dialog.getDialogId());
            dataManager.getMessageDataManager().createOrUpdateAll(ChatUtils.readAllMessages(messagesList, AppSession.getSession().getUser()));

            List<DialogNotification> dialogNotificationsList = dataManager.getDialogNotificationDataManager()
                    .getDialogNotificationsByDialogId(dialog.getDialogId());
            dataManager.getDialogNotificationDataManager().createOrUpdateAll(ChatUtils
                    .readAllDialogNotification(dialogNotificationsList, AppSession.getSession().getUser()));
        }
    }

    private void createChatLocally() {
        if (isNetworkAvailable()) {
            if (service != null) {
                baseChatHelper = (QBBaseChatHelper) service.getHelper(chatHelperIdentifier);
                Log.d("Fix double message", "baseChatHelper = " + baseChatHelper + "\n dialog = " + dialog);
                if (baseChatHelper != null && dialog != null) {
                    try {
                        baseChatHelper.createChatLocally(ChatUtils.createQBDialogFromLocalDialog(dataManager, dialog),
                                generateBundleToInitDialog());
                    } catch (QBResponseException e) {
                        ErrorUtils.showError(this, e.getMessage());
                        finish();
                    }
                }
            }
        }
    }

    private void closeChatLocally() {
        if (baseChatHelper != null && dialog != null) {
            baseChatHelper.closeChat(ChatUtils.createQBDialogFromLocalDialog(dataManager, dialog),
                    generateBundleToInitDialog());
        }
        dialog = null;
    }

    protected List<CombinationMessage> createCombinationMessagesList() {
        if (dialog == null) {
            Log.d("PrivateDialogActivity", "dialog = " + dialog);
            return null;
        }

        List<Message> messagesList = dataManager.getMessageDataManager().getMessagesByDialogId(dialog.getDialogId());
        List<DialogNotification> dialogNotificationsList = dataManager.getDialogNotificationDataManager()
                .getDialogNotificationsByDialogId(dialog.getDialogId());
        return ChatUtils.createCombinationMessagesList(messagesList, dialogNotificationsList);
    }

    private void visibleOrHideSmilePanel() {
        if (isSmilesLayoutShowing()) {
            hideSmileLayout();
            KeyboardUtils.showKeyboard(BaseDialogActivity.this);
        } else {
            KeyboardUtils.hideKeyboard(BaseDialogActivity.this);
            showSmileLayout();
        }
        shimmer_tv.setVisibility(View.GONE);
        messageEditText.setVisibility(View.VISIBLE);
        messageEditText.requestFocus();
    }

    protected void checkMessageSendingPossibility(boolean enable) {
        messageEditText.setEnabled(enable);
        smilePanelImageButton.setEnabled(enable);
        attachButton.setEnabled(enable);
    }

    protected abstract void updateActionBar();

    protected abstract void onConnectServiceLocally(QBService service);

    protected abstract Bundle generateBundleToInitDialog();

    protected abstract void updateMessagesList();

    protected abstract void onFileLoaded(QBFile file);

    protected abstract void checkMessageSendingPossibility();

    public static class CombinationMessageLoader extends BaseLoader<List<CombinationMessage>> {

        private String dialogId;

        public CombinationMessageLoader(Context context, DataManager dataManager, String dialogId) {
            super(context, dataManager);
            this.dialogId = dialogId;
        }

        @Override
        protected List<CombinationMessage> getItems() {
            return createCombinationMessagesList();
        }

        private List<CombinationMessage> createCombinationMessagesList() {
            List<Message> messagesList = dataManager.getMessageDataManager().getMessagesByDialogId(dialogId);
            List<DialogNotification> dialogNotificationsList = dataManager.getDialogNotificationDataManager()
                    .getDialogNotificationsByDialogId(dialogId);
            return ChatUtils.createCombinationMessagesList(messagesList, dialogNotificationsList);
        }
    }

    private class MessageObserver implements Observer {

        @Override
        public void update(Observable observable, Object data) {
            Log.d("Fix double message", "MessageObserver update(Observable observable, Object data) from " + BaseDialogActivity.class.getSimpleName());
            if (data != null && data.equals(MessageDataManager.OBSERVE_KEY)) {
                updateMessagesList();
            }
        }
    }

    private class DialogNotificationObserver implements Observer {

        @Override
        public void update(Observable observable, Object data) {
            Log.d("Fix double message", "DialogNotificationObserver update(Observable observable, Object data) from " + BaseDialogActivity.class.getSimpleName());
            if (data != null && data.equals(DialogNotificationDataManager.OBSERVE_KEY)) {
                updateMessagesList();
            }
        }
    }

    private class DialogObserver implements Observer {

        @Override
        public void update(Observable observable, Object data) {
            Log.d("Fix double message", "DialogObserver update(Observable observable, Object data) from " + BaseDialogActivity.class.getSimpleName());
            if (data != null && data.equals(DialogDataManager.OBSERVE_KEY) && dialog != null) {
                dialog = dataManager.getDialogDataManager().getByDialogId(dialog.getDialogId());
                updateActionBar();
            }
        }
    }

    private class TypingTimerTask extends TimerTask {

        @Override
        public void run() {
            isTypingNow = false;
            sendTypingStatus();
        }
    }

    public class LoadAttachFileSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            QBFile file = (QBFile) bundle.getSerializable(QBServiceConsts.EXTRA_ATTACH_FILE);
            Const.FORWARD_MESSAGE = null;
            onFileLoaded(file);
            hideProgress();
            String attachmentType = file.getContentType();
            if (attachmentType.contains("audio")) {
                folderName = FileUtils.audioFolderName;
            } else if (attachmentType.contains("video")) {
                folderName = FileUtils.videoFolderName;
            } else if (attachmentType.contains("image")) {
                folderName = FileUtils.folderName + "/";
            } else if (attachmentType.contains("pdf") || attachmentType.contains("msword") || attachmentType.contains("wordprocessingml")) {
                folderName = FileUtils.docFolderName;
            } else {
                folderName = FileUtils.otherFolderName;
            }
            destFile = new File(Environment.getExternalStorageDirectory().toString() + folderName, sourceFile.getName());
            try {
                copyFile(sourceFile, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class LoadDialogMessagesSuccessAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            messageSwipeRefreshLayout.setRefreshing(false);
            int totalEntries = bundle.getInt(QBServiceConsts.EXTRA_TOTAL_ENTRIES, ConstsCore.ZERO_INT_VALUE);


            if (messagesAdapter != null && !messagesAdapter.isEmpty() && totalEntries != ConstsCore.ZERO_INT_VALUE) {
                scrollMessagesToBottom();
            }

            loadMore = false;

        }
    }

    public class LoadDialogMessagesFailAction implements Command {

        @Override
        public void execute(Bundle bundle) {
            messageSwipeRefreshLayout.setRefreshing(false);

            loadMore = false;

        }
    }

    private class TypingStatusBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            int userId = extras.getInt(QBServiceConsts.EXTRA_USER_ID);
            // TODO: now it is possible only for Private chats
            if (dialog != null && opponentUser != null && userId == opponentUser.getUserId()) {
                if (Dialog.Type.PRIVATE.equals(dialog.getType())) {
                    boolean isTyping = extras.getBoolean(QBServiceConsts.EXTRA_IS_TYPING);
                    if (isTyping) {
                        showTypingStatus();
                    } else {
                        hideTypingStatus();
                    }
                }
            }
        }
    }

    private class UpdatingDialogBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(QBServiceConsts.UPDATE_DIALOG)) {
                updateData();
            }
        }
    }

    private class RefreshLayoutListener implements SwipeRefreshLayout.OnRefreshListener {

        @Override
        public void onRefresh() {
            if (!isNetworkAvailable()) {
                messageSwipeRefreshLayout.setRefreshing(false);
                return;
            }

            if (!loadMore) {
                loadMore = true;
                startLoadDialogMessages();
            }
        }
    }

    //for long click

    public void ownMessage(final CombinationMessage combinationMessage) {
        final String opration[];
        if (combinationMessage.getAttachment() == null) {
            opration = resources.getStringArray(R.array.new_messages_option);
            if (dataManager.getMessageDataManager().isFav(combinationMessage.getMessageId())) {
                opration[0] = "Remove from favourite";
            } else {
                opration[0] = "Add this message to Favourite";
            }
            new MaterialDialog.Builder(BaseDialogActivity.this)
                    .items(opration)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            if (which == 0) {
                                int update = (opration[0].equals("Remove from favourite")) ? 0 : 1;
                                String message = (opration[0].equals("Remove from favourite")) ? "Remove from favourite" : "Add this message to Favourite";
                                if (dataManager.getMessageDataManager().updateFav(combinationMessage.getMessageId(), update) > 0) {
                                    M.T(BaseDialogActivity.this, message);
                                } else {
                                    M.E("Error in add to favourite");
                                }
                            } else if (which == 1) {
                                //Edit
                                editMessage(combinationMessage);
                            } else if (which == 2) {
                                //Remove
                                removeMessage(combinationMessage);
                            } else if (which == 3) {
                                //Copy
                                Utility.msgInClipBoard(BaseDialogActivity.this, combinationMessage.getBody());
                            }
                        }
                    })
                    .show();
        } else {
            opration = resources.getStringArray(R.array.new_messages_own_attachment_option);
            if (dataManager.getMessageDataManager().isFav(combinationMessage.getMessageId())) {
                opration[0] = "Remove from favourite";
            } else {
                opration[0] = "Add this message to Favourite";
            }
            new MaterialDialog.Builder(BaseDialogActivity.this)
                    .items(opration)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            if (which == 0) {
                                int update = (opration[0].equals("Remove from favourite")) ? 0 : 1;
                                String message = (opration[0].equals("Remove from favourite")) ? "Remove from favourite" : "Add this message to Favourite";
                                if (dataManager.getMessageDataManager().updateFav(combinationMessage.getMessageId(), update) > 0) {
                                    M.T(BaseDialogActivity.this, message);
                                } else {
                                    M.E("Error in add to favourite");
                                }
                                messagesAdapter.notifyDataSetChanged();
                            } else if (which == 1) {
                                //Remove
                                removeMessage(combinationMessage);
                            } else if (which == 2) {
                                //Copy
                                if (combinationMessage.getAttachment() != null) {
                                    //forward images or file ka code
                                    final File file;
                                    String fileName;
                                    if (combinationMessage.getAttachment().getType().equals(Attachment.Type.PICTURE)) {
                                        Uri fileUri = Uri.parse(combinationMessage.getAttachment().getRemoteUrl());
                                        fileName = fileUri.getLastPathSegment() + ".jpg";
                                        file = new File(Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage), fileName);
                                    } else {
                                        file = new File(Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage), combinationMessage.getAttachment().getName());
                                        fileName = combinationMessage.getAttachment().getName();
                                    }

                                    final boolean check = file.exists();
                                    if (check) {
                                        Const.FORWARD_MESSAGE = null;
                                        Const.FORWARD_MESSAGE = Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage) + fileName;
                                        M.E(Const.FORWARD_MESSAGE);
                                        MainActivity.start(BaseDialogActivity.this);
                                        BaseDialogActivity.this.finish();
                                    } else {
                                        M.T(BaseDialogActivity.this, "Message forwarding failed, media is missing, download first.");
                                    }
                                }
                            }
                        }
                    })
                    .show();
        }
        messagesAdapter.notifyDataSetChanged();
    }

    public void opponentMessage(final CombinationMessage combinationMessage) {
        final String opration[] = resources.getStringArray(R.array.new_messages_option_opponent);
        if (dataManager.getMessageDataManager().isFav(combinationMessage.getMessageId())) {
            opration[0] = "Remove from favourite";
        }
        if (combinationMessage.getAttachment() != null) {
            opration[1] = "Forward this message";
        }
        new MaterialDialog.Builder(BaseDialogActivity.this)
                .items(opration)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        if (which == 0) {
                            int update = (opration[0].equals("Remove from favourite")) ? 0 : 1;
                            String message = (opration[0].equals("Remove from favourite")) ? "Remove from favourite" : "Added to favourite";
                            if (dataManager.getMessageDataManager().updateFav(combinationMessage.getMessageId(), update) > 0) {
                                M.T(BaseDialogActivity.this, message);
                            } else {
                                M.E("Error in add to favourite");
                            }
                        } else if (which == 1) {
                            //Copy
                            if (combinationMessage.getAttachment() != null) {
                                //forward images or file ka code
                                if (combinationMessage.getAttachment() != null) {
                                    //forward images or file ka code
                                    final File file;
                                    String fileName;
                                    if (combinationMessage.getAttachment().getType().equals(Attachment.Type.PICTURE)) {
                                        Uri fileUri = Uri.parse(combinationMessage.getAttachment().getRemoteUrl());
                                        fileName = fileUri.getLastPathSegment() + ".jpg";
                                        file = new File(Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage), fileName);
                                    } else {
                                        file = new File(Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage), combinationMessage.getAttachment().getName());
                                        fileName = combinationMessage.getAttachment().getName();
                                    }

                                    final boolean check = file.exists();
                                    if (check) {
                                        Const.FORWARD_MESSAGE = null;
                                        Const.FORWARD_MESSAGE = Environment.getExternalStorageDirectory().toString() + getDirectoryName(combinationMessage) + fileName;
                                        M.E(Const.FORWARD_MESSAGE);
                                        MainActivity.start(BaseDialogActivity.this);
                                        BaseDialogActivity.this.finish();
                                    } else {
                                        M.T(BaseDialogActivity.this, "Message forwarding failed, media is missing, download first.");
                                    }
                                }
                            } else {
                                Utility.msgInClipBoard(BaseDialogActivity.this, combinationMessage.getBody());
                            }
                        }
                    }
                })
                .show();
        messagesAdapter.notifyDataSetChanged();
    }

    private void removeMessage(final CombinationMessage combinationMessage) {
        SweetAlertDialog sweetAlertDialog = M.dConfirem(BaseDialogActivity.this, "Delete message?", "DELETE", "CANCEL");
        sweetAlertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(final SweetAlertDialog sweetAlertDialog) {
                if (NetworkUtil.getConnectivityStatus(BaseDialogActivity.this)) {
                    new DeleteMessage(combinationMessage).execute();
                    sweetAlertDialog.dismiss();
                } else {
                    M.dError(BaseDialogActivity.this, getString(R.string.dlg_internet_connection_is_missing));
                }
            }
        });
        sweetAlertDialog.setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
            @Override
            public void onClick(SweetAlertDialog sweetAlertDialog) {
                sweetAlertDialog.dismiss();
            }
        });

        messagesAdapter.notifyDataSetChanged();
    }

    private class DeleteMessage extends AsyncTask<String, Void, Boolean> {
        CombinationMessage combinationMessage;

        DeleteMessage(CombinationMessage combinationMessage) {
            this.combinationMessage = combinationMessage;
        }

        @Override
        protected Boolean doInBackground(String[] params) {
            // do above Server call here
            try {
                Set<String> messagesIds = new HashSet<String>() {{
                    add(combinationMessage.getMessageId());
                }};
                QBChatService.deleteMessages(messagesIds, true);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean message) {
            //process message
            if (message) {
                dataManager.getMessageDataManager().deleteMessageById(combinationMessage.getMessageId());
            } else {
                M.T(BaseDialogActivity.this, "Unable to delete message");
            }

        }
    }

    public void editMessage(final CombinationMessage msg) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_editmessage, null);
        Button loginButton = (Button) view.findViewById(R.id.loginButton);
        ImageView close = (ImageView) view.findViewById(R.id.close);
        final EditText emailEditText = (EditText) view.findViewById(R.id.dialog_message);
        emailEditText.setText(msg.getBody());
        final MaterialDialog dialog = new MaterialDialog.Builder(BaseDialogActivity.this)
                .autoDismiss(false)
                .customView(view, false)
                .show();
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        loginButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (UserAccount.isEmpty(emailEditText)) {
                            QBMessageUpdateBuilder messageUpdateBuilder = new QBMessageUpdateBuilder();
                            messageUpdateBuilder.updateText(emailEditText.getText().toString());
                            QBChatService.updateMessage(msg.getMessageId(), msg.getDialogOccupant().getDialog().getDialogId(), messageUpdateBuilder, new QBEntityCallback<Void>() {
                                @Override
                                public void onSuccess(Void aVoid, Bundle bundle) {
                                    dataManager.getMessageDataManager().updateMessage(msg.getMessageId(), emailEditText.getText().toString());
                                    M.T(BaseDialogActivity.this, "Message is updated");
                                    updateData();
                                    dialog.dismiss();
                                }

                                @Override
                                public void onError(QBResponseException errors) {

                                }
                            });

                        } else {
                            UserAccount.EditTextPointer.requestFocus();
                            UserAccount.EditTextPointer.setError("This can't be empty !");
                        }
                    }
                });
        messagesAdapter.notifyDataSetChanged();
    }

}