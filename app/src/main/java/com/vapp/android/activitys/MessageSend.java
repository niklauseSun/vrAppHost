package com.vapp.android.activitys;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vapp.android.R;
import com.vapp.taketosee.AgoraMessage;

public class MessageSend extends AppCompatActivity {

    private EditText login_input = null;
    private Button login_button = null;
    private Button logout_button = null;

    private EditText channel_input = null;
    private Button join_channel = null;
    private Button leave_channel = null;

    private EditText group_message = null;
    private Button group_send = null;

    private EditText peer_message = null;
    private Button peer_send = null;

    private String login_text = null;
    private String channel_text = null;
    private String gourp_message_text = null;
    private String peer_message_text = null;
    private AgoraMessage agoraMessage = null;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.message_send);

        login_input = findViewById(R.id.login_input);
        login_button = findViewById(R.id.login_button);
        logout_button = findViewById(R.id.logout_button);

        channel_input = findViewById(R.id.channel_input);
        join_channel = findViewById(R.id.join_channel);
        leave_channel = findViewById(R.id.leave_channel);

        group_message = findViewById(R.id.group_message_input);
        group_send = findViewById(R.id.group_message_send);

        peer_message = findViewById(R.id.peer_message);
        peer_send = findViewById(R.id.peer_send_button);

        agoraMessage = new AgoraMessage();
        agoraMessage.initRTMMessageClient(this, getString(R.string.agora_app_id));


        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uid = login_input.getText().toString();
                login_text = uid;
                agoraMessage.loginRTMClientWithOutToken(uid);
            }
        });

        logout_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                agoraMessage.logoutRtm();
            }
        });

        join_channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MESSAGESEND", "test");
                String channel_name = channel_input.getText().toString();
                agoraMessage.createChannerListener();
                Log.i("MESSAGESEND", channel_name);
//                agoraMessage.joinChannel(channel_name);
            }
        });

        leave_channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                agoraMessage.leaveChannel();
            }
        });
        
        group_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String channel_text = group_message.getText().toString();
//                agoraMessage.sendChannelMessage(channel_text);
            }
        });

        peer_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String peer_text = peer_message.getText().toString();
//                agoraMessage.sendPeerMessage(peer_text, "userB");
            }
        });
    }


}
