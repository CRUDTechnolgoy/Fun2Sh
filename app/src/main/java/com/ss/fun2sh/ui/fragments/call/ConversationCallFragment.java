package com.ss.fun2sh.ui.fragments.call;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.quickblox.q_municate_core.models.AppSession;
import com.quickblox.q_municate_core.models.StartConversationReason;
import com.quickblox.q_municate_core.qb.commands.push.QBSendPushCommand;
import com.quickblox.q_municate_core.qb.helpers.QBFriendListHelper;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate_core.utils.call.CameraUtils;
import com.quickblox.q_municate_db.models.User;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBMediaStreamManager;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientVideoTracksCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCSessionConnectionCallbacks;
import com.quickblox.videochat.webrtc.exception.QBRTCException;
import com.quickblox.videochat.webrtc.view.QBRTCVideoTrack;
import com.ss.fun2sh.R;
import com.ss.fun2sh.ui.activities.call.CallActivity;
import com.ss.fun2sh.ui.views.RTCGLVideoView;
import com.ss.fun2sh.utils.image.ImageLoaderUtils;

import org.webrtc.VideoRenderer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ConversationCallFragment extends Fragment implements Serializable, QBRTCClientVideoTracksCallbacks,
        QBRTCSessionConnectionCallbacks, CallActivity.QBRTCSessionUserCallback/*, OpponentsFromCallAdapter.OnAdapterEventListener*/ {

    private static final int DEFAULT_ROWS_COUNT = 2;
    private static final int DEFAULT_COLS_COUNT = 3;
    private static final long TOGGLE_CAMERA_DELAY = 1000;
    private static final long LOCAL_TRACk_INITIALIZE_DELAY = 500;

    private String TAG = ConversationCallFragment.class.getSimpleName();
    private ArrayList<QBUser> opponents;
    private QBRTCTypes.QBConferenceType qbConferenceType;
    private StartConversationReason startConversationReason;
    private String sessionID;

    private ToggleButton cameraToggle;
    private ToggleButton micToggleVideoCall;
    private ImageButton handUpVideoCall;
    private View view;
    private Map<String, String> userInfo;
    private boolean isVideoCall = false;
    private boolean isAudioEnabled = true;
    private List<QBUser> allUsers = new ArrayList<>();
    private LinearLayout actionVideoButtonsLayout;
    private String callerName;
    private boolean isMessageProcessed;
    private RTCGLVideoView remoteVideoView;
    private CameraState cameraState = CameraState.NONE;
    private boolean isPeerToPeerCall;
    private QBRTCVideoTrack localVideoTrack;
    private QBRTCVideoTrack remoteVideoTrack;
    private Handler mainHandler;
    private ImageView avatarImageview;
    private TextView callingToTextView;
    private FrameLayout avatarAndNameView;
    private boolean isFullScreen;
    private View elementSetVideoButtons;

    public static ConversationCallFragment newInstance(List<QBUser> opponents, String callerName,
                                                       QBRTCTypes.QBConferenceType qbConferenceType,
                                                       StartConversationReason reason,
                                                       String sessionId) {

        ConversationCallFragment fragment = new ConversationCallFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(QBServiceConsts.EXTRA_CONFERENCE_TYPE, qbConferenceType);
        bundle.putString(QBServiceConsts.EXTRA_CALLER_NAME, callerName);
        bundle.putSerializable(QBServiceConsts.EXTRA_OPPONENTS, (Serializable) opponents);

        bundle.putSerializable(QBServiceConsts.EXTRA_START_CONVERSATION_REASON_TYPE, reason);
        if (sessionId != null) {
            bundle.putString(QBServiceConsts.EXTRA_SESSION_ID, sessionId);
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_conversation, container, false);
        Log.d(TAG, "Fragment. Thread id: " + Thread.currentThread().getId());

        if (getArguments() != null) {
            opponents = (ArrayList<QBUser>) getArguments().getSerializable(QBServiceConsts.EXTRA_OPPONENTS);
            qbConferenceType = (QBRTCTypes.QBConferenceType) getArguments().getSerializable(QBServiceConsts.EXTRA_CONFERENCE_TYPE);
            startConversationReason = (StartConversationReason) getArguments().getSerializable(QBServiceConsts.EXTRA_START_CONVERSATION_REASON_TYPE);
            sessionID = getArguments().getString(QBServiceConsts.EXTRA_SESSION_ID);
            callerName = getArguments().getString(QBServiceConsts.EXTRA_CALLER_NAME);

            isPeerToPeerCall = opponents.size() == 1;
            isVideoCall = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO.equals(qbConferenceType);
            Log.d(TAG, "CALLER_NAME: " + callerName);
            Log.d(TAG, "opponents: " + opponents.toString());
        }

        initViews(view);
        ((CallActivity)getActivity()).initActionBar();
        ((CallActivity)getActivity()).setCallActionBarTitle(StartConversationReason.INCOME_CALL_FOR_ACCEPTION
                .equals(startConversationReason) ? callerName : opponents.get(0).getFullName());
        initButtonsListener();
        initSessionListener();
        setUpUiByCallType();

        displayOpponentAvatar();

        mainHandler = new FragmentLifeCycleHandler();
        return view;

    }

    private void initSessionListener() {
        ((CallActivity) getActivity()).addVideoTrackCallbacksListener(this);
    }

    private void setUpUiByCallType() {
        if (!isVideoCall) {
            remoteVideoView.setVisibility(View.INVISIBLE);
            cameraToggle.setVisibility(View.GONE);
            cameraToggle.setEnabled(false);
            cameraToggle.setChecked(false);
        }
    }

    private void displayOpponentAvatar() {
        User opponent = ((CallActivity) getActivity()).getOpponentAsUserFromDB(opponents.get(0).getId());
        if (StartConversationReason.INCOME_CALL_FOR_ACCEPTION.equals(startConversationReason) && !isVideoCall){
            avatarAndNameView.setVisibility(View.VISIBLE);
            ImageLoader.getInstance().displayImage(opponent.getAvatar(), avatarImageview, ImageLoaderUtils.UIL_USER_AVATAR_DISPLAY_OPTIONS);
        } else if (StartConversationReason.OUTCOME_CALL_MADE.equals(startConversationReason)) {
            avatarAndNameView.setVisibility(View.VISIBLE);
            ImageLoader.getInstance().displayImage(opponent.getAvatar(), avatarImageview, ImageLoaderUtils.UIL_USER_AVATAR_DISPLAY_OPTIONS);
            callingToTextView.setText(getString(R.string.calling_to, opponent.getFullName()));
        }
    }


    private void actionsByConnectedToUser(){
        actionButtonsEnabled(true);
        ((CallActivity) getActivity()).startTimer();
        getActivity().invalidateOptionsMenu();
        hideAvatarIfNeed();
    }

    private void hideAvatarIfNeed() {
        callingToTextView.setVisibility(View.INVISIBLE);

        if (isVideoCall) {
            avatarAndNameView.setVisibility(View.GONE);
        }
    }

    public void actionButtonsEnabled(boolean enability) {

        if (isVideoCall){
            cameraToggle.setEnabled(enability);
        }
        micToggleVideoCall.setEnabled(enability);

        // inactivate toggle buttons
        if (isVideoCall) {
            cameraToggle.setActivated(enability);
        }
        micToggleVideoCall.setActivated(enability);
    }


    @Override
    public void onStart() {
        super.onStart();
        QBRTCSession session = ((CallActivity) getActivity()).getCurrentSession();
        if (!isMessageProcessed) {
            if (startConversationReason == StartConversationReason.INCOME_CALL_FOR_ACCEPTION) {
                session.acceptCall(session.getUserInfo());
            } else {
                sendPushAboutCall();
                session.startCall(session.getUserInfo());
            }
            isMessageProcessed = true;
        }
        ((CallActivity) getActivity()).addTCClientConnectionCallback(this);
        ((CallActivity) getActivity()).addRTCSessionUserCallback(this);
    }

    private void sendPushAboutCall() {
        if (isPeerToPeerCall) {
            QBFriendListHelper qbFriendListHelper = new QBFriendListHelper(getActivity());
            QBUser qbUser = opponents.get(0);

            if (!qbFriendListHelper.isUserOnline(qbUser.getId())) {
                String callMsg = getString(R.string.dlg_offline_call,
                        AppSession.getSession().getUser().getFullName());
                QBSendPushCommand.start(getActivity(), callMsg, qbUser.getId());
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() from " + TAG);
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void initViews(View view) {
        remoteVideoView = (RTCGLVideoView) ((ViewStub) view.findViewById(R.id.remoteViewStub)).inflate();

        cameraToggle = (ToggleButton) view.findViewById(R.id.cameraToggle);

        micToggleVideoCall = (ToggleButton) view.findViewById(R.id.micToggleVideoCall);

        actionVideoButtonsLayout = (LinearLayout) view.findViewById(R.id.element_set_video_buttons);

        handUpVideoCall = (ImageButton) view.findViewById(R.id.handUpVideoCall);

        avatarAndNameView = (FrameLayout) view.findViewById(R.id.avatar_and_name);

        avatarAndNameView.addView(getActivity().getLayoutInflater().inflate(isVideoCall ?
                        R.layout.view_avatar_and_name_horizontal : R.layout.view_avatar_and_name_vertical,
                avatarAndNameView, false));

        elementSetVideoButtons = view.findViewById(R.id.element_set_video_buttons);

        avatarImageview = (ImageView) avatarAndNameView.findViewById(R.id.avatar_imageview);

        callingToTextView = (TextView) avatarAndNameView.findViewById(R.id.calling_to_text_view);

        actionButtonsEnabled(false);

     /*   new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLocalVideoViewVisible(true,getResources().getIntArray(R.array.local_view_coordinates_full_screen_aoutomatichide));
                        elementSetVideoButtons.setVisibility(View.INVISIBLE);
                    }
                });

            }
        }, 6000);*/
    }

    @Override
    public void onResume() {
        super.onResume();

        // If user changed camera state few times and last state was CameraState.ENABLED_FROM_USER // Жень, глянь здесь, смысл в том, что мы здесь включаем камеру, если юзер ее не выключал
        // than we turn on cam, else we nothing change
        if (cameraState != CameraState.DISABLED_FROM_USER
                && isVideoCall) {
            toggleCamera(true);
        }
    }

    @Override
    public void onPause() {
        // If camera state is CameraState.ENABLED_FROM_USER or CameraState.NONE
        // than we turn off cam
        if (cameraState != CameraState.DISABLED_FROM_USER && isVideoCall) {
            toggleCamera(false);
        }

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((CallActivity) getActivity()).removeRTCClientConnectionCallback(this);
        ((CallActivity) getActivity()).removeRTCSessionUserCallback();
    }

    private void initButtonsListener() {
        cameraToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                cameraState = isChecked ? CameraState.ENABLED_FROM_USER : CameraState.DISABLED_FROM_USER;
                toggleCamera(isChecked);
            }
        });

        micToggleVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((CallActivity) getActivity()).getCurrentSession() != null) {
                    if (isAudioEnabled) {
                        Log.d(TAG, "Mic is off!");
                        ((CallActivity) getActivity()).getCurrentSession().setAudioEnabled(false);
                        isAudioEnabled = false;
                    } else {
                        Log.d(TAG, "Mic is on!");
                        ((CallActivity) getActivity()).getCurrentSession().setAudioEnabled(true);
                        isAudioEnabled = true;
                    }
                }
            }
        });

        handUpVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionButtonsEnabled(false);
                handUpVideoCall.setEnabled(false);
                Log.d(TAG, "Call is stopped");

                ((CallActivity) getActivity()).hangUpCurrentSession();
                handUpVideoCall.setEnabled(false);
                handUpVideoCall.setActivated(false);

            }
        });

        remoteVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFullScreen();
                elementSetVideoButtons.setVisibility(View.VISIBLE);
                setLocalVideoViewVisible(true,getResources().getIntArray(R.array.local_view_coordinates_full_screen));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("log1","handler value1-");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setLocalVideoViewVisible(true,getResources().getIntArray(R.array.local_view_coordinates_full_screen_aoutomatichide));
                                elementSetVideoButtons.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                }, 6000);
            }
        });
    }

    void toggleFullScreen(){
        if(isFullScreen){
           // setLocalVideoViewVisible(false);
            showSystemUI();
            ((CallActivity)getActivity()).showCallActionBar();
            elementSetVideoButtons.setVisibility(View.INVISIBLE);
            isFullScreen = false;
        } else {
         //   setLocalVideoViewVisible(true);
            hideSystemUI();
            ((CallActivity) getActivity()).hideCallActionBar();
            elementSetVideoButtons.setVisibility(View.VISIBLE);
            isFullScreen = true;
        }
    }


    private void setLocalVideoViewVisible(boolean visible,int s[]){
        if (remoteVideoView != null && localVideoTrack != null){
            if (visible) {
                QBMediaStreamManager mediaStreamManager = ((CallActivity) getActivity()).getCurrentSession().getMediaStreamManager();
                int currentCameraId = mediaStreamManager.getCurrentCameraId();

                RTCGLVideoView.RendererConfig config = new RTCGLVideoView.RendererConfig();
                config.mirror = CameraUtils.isCameraFront(currentCameraId);
                config.coordinates = s;

                localVideoTrack.addRenderer(new VideoRenderer(remoteVideoView.obtainVideoRenderer(RTCGLVideoView.RendererSurface.SECOND)));

                remoteVideoView.updateRenderer(RTCGLVideoView.RendererSurface.SECOND, config);
                Log.d(TAG, "fullscreen enabled");
            } else {
                localVideoTrack.removeRenderer(localVideoTrack.getRenderer());
                remoteVideoView.removeLocalRendererCallback();
                Log.d(TAG, "fullscreen disabled");
            }
        }
    }


    private void hideSystemUI() {
        getActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    private void showSystemUI() {
        getActivity().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        QBRTCSession currentSession = ((CallActivity) getActivity()).getCurrentSession();
        if (currentSession == null) {
            return false;
        }

        final QBMediaStreamManager mediaStreamManager = currentSession.getMediaStreamManager();
        if (mediaStreamManager == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.switch_camera_toggle:
                boolean cameraSwitched = mediaStreamManager.switchCameraInput(new Runnable() {
                    @Override
                    public void run() {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                toggleCamerainternal(mediaStreamManager);
                            }
                        });
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleCamerainternal(QBMediaStreamManager mediaStreamManager){
        toggleCameraOnUiThread(false);
        int currentCameraId = mediaStreamManager.getCurrentCameraId();
        Log.d(TAG, "Camera was switched!");
        RTCGLVideoView.RendererConfig config = new RTCGLVideoView.RendererConfig();
        config.mirror = CameraUtils.isCameraFront(currentCameraId);
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toggleCameraOnUiThread(true);
            }
        }, TOGGLE_CAMERA_DELAY);
    }

    private void toggleCameraOnUiThread(final boolean toggle){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggleCamera(toggle);
            }
        });
    }

    private void runOnUiThread(Runnable runnable){
        mainHandler.post(runnable);
    }

    private void toggleCamera(boolean isNeedEnableCam) {
        QBRTCSession currentSession = ((CallActivity) getActivity()).getCurrentSession();
        if (currentSession != null && currentSession.getMediaStreamManager() != null){
            currentSession.getMediaStreamManager().setVideoEnabled(isNeedEnableCam);
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onLocalVideoTrackReceive(QBRTCSession qbrtcSession, final QBRTCVideoTrack videoTrack) {
        Log.d(TAG, "onLocalVideoTrackReceive() run");
        localVideoTrack = videoTrack;

        if (remoteVideoView != null && remoteVideoTrack == null) {
            fillVideoView(remoteVideoView, videoTrack, false);

            RTCGLVideoView.RendererConfig config = new RTCGLVideoView.RendererConfig();
            config.mirror = true;
            config.coordinates = getResources().getIntArray(R.array.local_view_coordinates);
            remoteVideoView.updateRenderer(RTCGLVideoView.RendererSurface.SECOND, config);
        }
    }

    @Override
    public void onRemoteVideoTrackReceive(QBRTCSession session, QBRTCVideoTrack videoTrack, Integer userID) {
        Log.d(TAG, "onRemoteVideoTrackReceive for opponent= " + userID);
        remoteVideoTrack = videoTrack;

        if (remoteVideoView != null) {
            fillVideoView(remoteVideoView, videoTrack, true);

            RTCGLVideoView.RendererConfig config = new RTCGLVideoView.RendererConfig();
            config.mirror = false;
            remoteVideoView.updateRenderer(RTCGLVideoView.RendererSurface.MAIN, config);

            if (localVideoTrack != null) {
                localVideoTrack.removeRenderer(localVideoTrack.getRenderer());
                remoteVideoView.removeLocalRendererCallback();
            }
        }
    }

    private void fillVideoView(RTCGLVideoView videoView, QBRTCVideoTrack videoTrack, boolean remoteRenderer) {
        videoTrack.addRenderer(new VideoRenderer(remoteRenderer ?
                videoView.obtainVideoRenderer(RTCGLVideoView.RendererSurface.MAIN) :
                videoView.obtainVideoRenderer(RTCGLVideoView.RendererSurface.SECOND)));
        Log.d(TAG, (remoteRenderer ? "remote" : "local") + " Track is rendering");
    }

    private void fillVideoView(RTCGLVideoView videoView, QBRTCVideoTrack videoTrack) {
        fillVideoView(videoView, videoTrack, true);
    }

    @Override
    public void onStartConnectToUser(QBRTCSession qbrtcSession, Integer userId) {
//        setStatusForOpponent(userId, getString(R.string.checking));
    }

    @Override
    public void onConnectedToUser(QBRTCSession qbrtcSession, final Integer userId) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                actionsByConnectedToUser();
            }
        });
//        setStatusForOpponent(userId, getString(R.string.connected));
    }


    @Override
    public void onConnectionClosedForUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.closed));
    }

    @Override
    public void onDisconnectedFromUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.disconnected));
    }

    @Override
    public void onDisconnectedTimeoutFromUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.time_out));
    }

    @Override
    public void onConnectionFailedWithUser(QBRTCSession qbrtcSession, Integer integer) {
//        setStatusForOpponent(integer, getString(R.string.failed));
    }

    @Override
    public void onError(QBRTCSession qbrtcSession, QBRTCException e) {

    }

    @Override
    public void onUserNotAnswer(QBRTCSession session, Integer userId) {
//        setStatusForOpponent(userId, getString(R.string.call_no_answer));
    }

    @Override
    public void onCallRejectByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
//        setStatusForOpponent(userId, getString(R.string.call_rejected));
    }

    @Override
    public void onCallAcceptByUser(QBRTCSession session, Integer userId, Map<String, String> userInfo) {
//        setStatusForOpponent(userId, getString(R.string.call_accepted));
    }

    @Override
    public void onReceiveHangUpFromUser(QBRTCSession session, Integer userId) {
//        setStatusForOpponent(userId, getString(R.string.call_hung_up));
    }



    private enum CameraState {
        NONE,
        DISABLED_FROM_USER,
        ENABLED_FROM_USER
    }

    class FragmentLifeCycleHandler extends Handler{

        @Override
        public void dispatchMessage(Message msg) {
            if (isAdded() && getActivity() != null) {
                super.dispatchMessage(msg);
            } else {
                Log.d(TAG, "Fragment under destroying");
            }
        }
    }

    class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private int space;

        public DividerItemDecoration(@NonNull Context context, @DimenRes int dimensionDivider) {
            this.space = context.getResources().getDimensionPixelSize(dimensionDivider);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(space, space, space, space);
        }
    }
}