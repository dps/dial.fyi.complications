package io.singleton.wearcomplications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.ProgressSpinner;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.wearable.intent.RemoteIntent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static io.singleton.wearcomplications.CurlConfigActivity.EXTRA_WC_COMPLICATION_TYPE;


public class ServerConfigActivity extends WearableActivity implements Settings.Listener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    public static final int FEED_FETCH_TIMEOUT_MS = 10000;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private ProgressSpinner mProgressView;
    private Settings mSettings;
    private LocalBroadcastManager mLocalBroadcastManager;
    private int mComplicationId;
    private int mComplicationType;
    private String mMode;


    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(FeedDownloadingStore.ACTION_UPDATE_START)) {
                // Great
            } else if (intent.getAction().equals(FeedDownloadingStore.ACTION_UPDATE_COMPLETE)) {
                if (mReceiver != null) {
                    mLocalBroadcastManager.unregisterReceiver(mReceiver);
                    mReceiver = null;
                }
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rss_config);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
        mProgressView = (ProgressSpinner) findViewById(R.id.progress);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        findViewById(R.id.buttonProceed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                IntentFilter filter = new IntentFilter();
                filter.addAction(FeedDownloadingStore.ACTION_UPDATE_START);
                filter.addAction(FeedDownloadingStore.ACTION_UPDATE_COMPLETE);
                mLocalBroadcastManager.registerReceiver(mReceiver, filter);

                if (mMode.equals("rss")) {
                    FeedDownloadingStore.getInstance(getApplicationContext()).updateFeed();
                } else {
                    FeedDownloadingStore.getInstance(getApplicationContext()).updateCurlSettings();
                }
                mProgressView.setVisibility(View.VISIBLE);
                mProgressView.showWithAnimation();
                findViewById(R.id.buttonProceed).setVisibility(View.GONE);

                setResult(RESULT_OK);
                timeoutToFinish(FEED_FETCH_TIMEOUT_MS);
            }
        });
    }

    private void timeoutToFinish(int timeoutMs) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mReceiver != null) {
                    mLocalBroadcastManager.unregisterReceiver(mReceiver);
                    mReceiver = null;
                }
                finish();
            }
        }, timeoutMs);
    }

    @Override
    public void onResume() {
        super.onResume();
        mComplicationId = getIntent().getIntExtra(
                android.support.wearable.complications.ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, -1);
        mComplicationType = getIntent().getIntExtra(
                android.support.wearable.complications.ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_TYPE, -1);

        mMode = getIntent().getStringExtra(EXTRA_WC_COMPLICATION_TYPE);
        if (mMode == null) {
            mMode = "rss";
        }

        mSettings = Settings.getInstance(this);
        mSettings.addListener(this);
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettings.removeListener(this);
    }

    private void updateUi() {
        if (mSettings.getConfigToken() == null) {
            mTextView.setText(getString(R.string.registering));
            FeedDownloadingStore.getInstance(this).register();
        } else {
            String confUrl = Constants.USER_FRIENDLY_BASE_URL +
                    String.format(Constants.USER_CONFIG_PATH, mMode, mSettings.getConfigToken());
            String title = mMode.equals("rss") ?
                    getSharedPreferences("feed", 0).getString("feedTitle", "None") :
                    "curl";
            mTextView.setText(getString(R.string.config_instructions, title, confUrl));

            openConfUrlOnPhone("http://" + confUrl);
        }
    }

    private void openConfUrlOnPhone(String confUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse(confUrl));
        RemoteIntent.startRemoteActivity(this, intent, null);
    }


    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
        FeedDownloadingStore.getInstance(this).updateFeed();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);
            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackgroundColor(getResources().getColor(R.color.wl_background));
            mTextView.setTextColor(getResources().getColor(R.color.text_color));
            mClockView.setVisibility(View.GONE);
        }
    }


    @Override
    public void onSettingsChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });
    }
}
