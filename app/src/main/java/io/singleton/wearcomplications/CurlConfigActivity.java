package io.singleton.wearcomplications;

import android.app.Activity;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

public class CurlConfigActivity extends WearableActivity {

    static final String EXTRA_WC_COMPLICATION_TYPE = "wc_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        intent.putExtra(EXTRA_WC_COMPLICATION_TYPE, "curl");
        intent.setClass(this, ServerConfigActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setResult(resultCode);
        finish();
    }
}
