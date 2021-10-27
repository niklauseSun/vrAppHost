package com.vapp.android.activitys;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vapp.taketosee.TakeToSee;
import com.vapp.android.R;

public class CallActivity extends AppCompatActivity {

    private Button callButton = null;
    private Button cancelButton = null;
    private TakeToSee takeToSee;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.call_activity);

        callButton = findViewById(R.id.call_button);
        cancelButton = findViewById(R.id.close_button);

        initCall();

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinChannel();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leaveChannel();
            }
        });

    }


    private void initCall() {
        takeToSee = new TakeToSee();
        takeToSee.initAgoraEngine(this, getString(R.string.agora_app_id));
    }


    private void joinChannel() {
        takeToSee.joinChannel(getString(R.string.agora_access_token), getString(R.string.channel_title), 10, "");
    }

    private void leaveChannel() {
//        takeToSee.leaveChannle();
    }
}
