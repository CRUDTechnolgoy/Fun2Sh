package com.ss.fun2sh.ui.activities.groupcall.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.quickblox.chat.QBChatService;
import com.quickblox.q_municate_core.service.QBServiceConsts;
import com.quickblox.q_municate_core.utils.helpers.CoreSharedHelper;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCSessionDescription;
import com.quickblox.videochat.webrtc.QBRTCTypes;
import com.ss.fun2sh.CRUD.M;
import com.ss.fun2sh.R;
import com.ss.fun2sh.oldutils.Constants;
import com.ss.fun2sh.ui.activities.groupcall.activities.GroupCallActivity;
import com.ss.fun2sh.ui.activities.groupcall.utils.RingtonePlayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * QuickBlox team
 */
public class IncomeCallFragment extends Fragment implements Serializable, View.OnClickListener {

    private static final String TAG = IncomeCallFragment.class.getSimpleName();
    private static final long CLICK_DELAY = TimeUnit.SECONDS.toMillis(2);
    private TextView incVideoCall;
    private TextView incAudioCall;
    private TextView callerName;
    private TextView otherIncUsers;
    private ImageButton rejectBtn;
    private ImageButton takeBtn;

    private ArrayList<Integer> opponents;
    private List<QBUser> opponentsFromCall = new ArrayList<>();
    private QBRTCSessionDescription sessionDescription;
    private Vibrator vibrator;
    private QBRTCTypes.QBConferenceType conferenceType;
    private int qbConferenceType;
    private View view;
    private long lastCliclTime = 0l;
    private RingtonePlayer ringtonePlayer;
    boolean missedCall;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (getArguments() != null) {
            opponents = getArguments().getIntegerArrayList(QBServiceConsts.EXTRA_OPPONENTS);
            sessionDescription = (QBRTCSessionDescription) getArguments().getSerializable(QBServiceConsts.EXTRA_SESSION_DESCRIPTION);
            qbConferenceType = getArguments().getInt(QBServiceConsts.EXTRA_CONFERENCE_TYPE);


            conferenceType = qbConferenceType == 1 ? QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO :
                    QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO;

            Log.d(TAG, conferenceType.toString() + "From onCreateView()");
        }

        if (savedInstanceState == null) {

            view = inflater.inflate(R.layout.group_fragment_income_call, container, false);
            initUI(view);
            setDisplayedTypeCall(conferenceType);
            initButtonsListener();

        }
        ringtonePlayer = new RingtonePlayer(getActivity());

        //  getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        return view;
    }

    private void stopCallNotification() {
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
        }
        if (missedCall) {
            //save for missed call
            ((GroupCallActivity) getActivity()).setMissedCallValue();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);

        Log.d(TAG, "onCreate() from IncomeCallFragment");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        startCallNotification();
    }

    private void initButtonsListener() {
        rejectBtn.setOnClickListener(this);
        takeBtn.setOnClickListener(this);
    }

    private void initUI(View view) {

        incAudioCall = (TextView) view.findViewById(R.id.incAudioCall);
        incVideoCall = (TextView) view.findViewById(R.id.incVideoCall);

        callerName = (TextView) view.findViewById(R.id.callerName);
        callerName.setText(getCallerName(((GroupCallActivity) getActivity()).getCurrentSession()));
        // ((GroupCallActivity) getActivity()).initGroupActionBar(getCallerName(((GroupCallActivity) getActivity()).getCurrentSession()) + " is calling");
        ((GroupCallActivity) getActivity()).initGroupActionBar("  ");
        otherIncUsers = (TextView) view.findViewById(R.id.otherIncUsers);
        otherIncUsers.setText(getOtherIncUsersNames(opponents));

        rejectBtn = (ImageButton) view.findViewById(R.id.rejectBtn);
        takeBtn = (ImageButton) view.findViewById(R.id.takeBtn);
    }

    private void enableButtons(boolean enable) {
        takeBtn.setEnabled(enable);
        rejectBtn.setEnabled(enable);
    }

    public void startCallNotification() {

        ringtonePlayer.play(false);

        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        long[] vibrationCycle = {0, 1000, 1000};
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(vibrationCycle, 1);
        }
        missedCall = true;
    }


    private String getOtherIncUsersNames(ArrayList<Integer> opponents) {
        M.E("onlysize  size" + opponents);
        StringBuffer s = new StringBuffer("");
        opponents.remove(QBChatService.getInstance().getUser().getId());

        for (Integer i : opponents) {
            for (QBUser usr : opponentsFromCall) {
                if (usr.getId().equals(i)) {
                    s.append(usr.getFullName() + ", ");
                }
            }
        }
        if (s.toString().endsWith(", ")) {
            return s.toString().substring(0, s.toString().length() - 2);
        } else {
            return s.toString();
        }
    }

    private String getCallerName(QBRTCSession session) {
        String s = new String();
        int i = session.getCallerID();

        opponentsFromCall.addAll(((GroupCallActivity) getActivity()).getOpponentsList());

        for (QBUser usr : opponentsFromCall) {
            if (usr.getId().equals(i)) {
                s = usr.getFullName();
            }
        }
        return s;
    }

    private void setDisplayedTypeCall(QBRTCTypes.QBConferenceType conferenceType) {
        if (conferenceType == QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO) {
            incVideoCall.setVisibility(View.VISIBLE);
            incAudioCall.setVisibility(View.INVISIBLE);
        } else if (conferenceType == QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO) {
            incVideoCall.setVisibility(View.INVISIBLE);
            incAudioCall.setVisibility(View.VISIBLE);
        }
    }

    public void onStop() {
        stopCallNotification();
        super.onDestroy();
        Log.d(TAG, "onDestroy() from IncomeCallFragment");
    }

    @Override
    public void onClick(View v) {

        if ((SystemClock.uptimeMillis() - lastCliclTime) < CLICK_DELAY) {
            return;
        }
        lastCliclTime = SystemClock.uptimeMillis();
        switch (v.getId()) {
            case R.id.rejectBtn:
                reject();
                CoreSharedHelper.getInstance().savePref(Constants.FUULSCREENPREF, "false");
                CoreSharedHelper.getInstance().savePref(Constants.USERPICAUDIOPREF, "false");
                CoreSharedHelper.getInstance().savePref(Constants.USERORIENTATIONAUDIOPREF, "false");
                break;
            case R.id.takeBtn:
                accept();
                if (opponents.size() == 0) {
                    CoreSharedHelper.getInstance().savePref(Constants.FUULSCREENPREF, "true");
                    CoreSharedHelper.getInstance().savePref(Constants.USERPICAUDIOPREF, "true");
                    CoreSharedHelper.getInstance().savePref(Constants.AUDIOONETOONECALL, "true");
                } else {
                    CoreSharedHelper.getInstance().savePref(Constants.USERPICAUDIOPREF, "true");
                    CoreSharedHelper.getInstance().savePref(Constants.USERORIENTATIONAUDIOPREF, "true");
                }
                break;
            default:
                break;
        }
    }

    private void accept() {
        takeBtn.setClickable(false);
        missedCall = false;
        stopCallNotification();

        ((GroupCallActivity) getActivity())
                .addConversationFragmentReceiveCall();

        Log.d(TAG, "Call is started");
    }

    private void reject() {
        rejectBtn.setClickable(false);
        Log.d(TAG, "Call is rejected");
        missedCall = false;
        stopCallNotification();

        ((GroupCallActivity) getActivity()).rejectCurrentSession(" I don't want to talk");
        ((GroupCallActivity) getActivity()).removeIncomeCallFragment();
        getActivity().finish();
    }
}
