package io.singleton.wearcomplications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.ProgressSpinner;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Set;

public class CounterConfigActivity extends WearableActivity implements WearableListView.ClickListener {

    private static final String TAG = "CA";
    private String[] mElements;
    private String[] mIds;
    private String[] mUrls;
    private int mComplicationId;
    private LocalBroadcastManager mLocalBroadcastManager;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

//            if (intent.getAction().equals(FxNetworkRequester.ACTION_LISTING_UPDATE_COMPLETE)) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        showListFromSharedPrefs();
//                    }
//                });
//            }

        }
    };

    WearableListView mListView;
    ProgressSpinner mSpinner;
    private SharedPreferences mSharedPrefs;
    private static final long UPDATE_INTERVAL_MILLIS = 60 * 60 * 1000;
    private FrameLayout mFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);



        // Get the list component from the layout of the activity
        mListView =
                (WearableListView) findViewById(R.id.wearable_list);
        mSpinner = (ProgressSpinner) findViewById(R.id.load_spinner);

        mFrameLayout = (FrameLayout) findViewById(R.id.frame_layout);

        mComplicationId = getIntent().getIntExtra(
                android.support.wearable.complications.ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, -1);


//        int selected = getSharedPreferences("config", 0).getInt(mComplicationId + "_selected_position", 0);
//        mListView.scrollToPosition(selected);

        IntentFilter intf = new IntentFilter();
//        intf.addAction(FxNetworkRequester.ACTION_LISTING_UPDATE_COMPLETE);
//        intf.addAction(FxNetworkRequester.ACTION_LISTING_UPDATE_ERROR);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intf);
        
        mSharedPrefs = getSharedPreferences("list", 0);
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (System.currentTimeMillis() - mSharedPrefs.getLong("provider_list_updated", 0l)
//                > UPDATE_INTERVAL_MILLIS) {
//            FxNetworkRequester.makeListingRequest(this);
//        } else {
            showListFromSharedPrefs();
//        }
    }

    private void showListFromSharedPrefs() {

        Set<String> providers = mSharedPrefs.getStringSet("providers", null);
        if (providers != null) {
            mElements = new String[providers.size()];
            mIds = new String[providers.size()];
            mUrls = new String[providers.size()];
            int i = 0;
            for (String id: providers) {
                mElements[i] = mSharedPrefs.getString(id + ":name", "");
                mIds[i] = id;
                mUrls[i] = mSharedPrefs.getString(id + ":url", "");
                Log.d(TAG, "UI provider " + id + " " + mElements[i]);

                i++;
            }
        } else {
            Log.d(TAG, "mSharedPrefs.getStringSet providers = null");
        }
        mSpinner.setVisibility(View.GONE);
        mListView.setVisibility(View.VISIBLE);
        mFrameLayout.setVisibility(View.VISIBLE);

        // Assign an adapter to the list
        mListView.setAdapter(new Adapter(this, mElements));

        // Set a click listener
        mListView.setClickListener(this);

        mListView.invalidate();

    }

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        int pos = viewHolder.getAdapterPosition();
        getSharedPreferences("config", 0).edit()
                .putInt(mComplicationId + "_selected_position", pos)
                .putString(mComplicationId + "_selected_provider_name", mElements[pos])
                .putString(mComplicationId + "_selected_provider_id", mIds[pos])
                .putString(mComplicationId + "_selected_provider_url", mUrls[pos])
                .apply();

        setResult(RESULT_OK);
        ComplicationService.requestUpdateComplication(this, mComplicationId);
        finish();
    }

    @Override
    public void onTopEmptyRegionClick() {

    }

    private static final class Adapter extends WearableListView.Adapter {
        private String[] mDataset;
        private final LayoutInflater mInflater;

        public Adapter(Context context, String[] dataset) {
            mInflater = LayoutInflater.from(context);
            mDataset = dataset;
        }

        public static class ItemViewHolder extends WearableListView.ViewHolder {
            private TextView textView;
            public ItemViewHolder(View itemView) {
                super(itemView);
                // find the text view within the custom item's layout
                textView = (TextView) itemView.findViewById(R.id.name);
            }
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder,
                                     int position) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            view.setText(mDataset[position]);
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mDataset.length;
        }
    }

}
