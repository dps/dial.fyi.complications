package io.singleton.wearcomplications;

import android.app.Activity;
import android.app.RemoteInput;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

import static android.support.wearable.input.RemoteInputIntent.ACTION_REMOTE_INPUT;
import static android.support.wearable.input.RemoteInputIntent.EXTRA_REMOTE_INPUTS;

public class EmojagotchiConfigActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emojagotchi_config);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setResult(Activity.RESULT_OK);
    }

}