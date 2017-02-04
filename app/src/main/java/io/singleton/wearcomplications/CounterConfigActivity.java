package io.singleton.wearcomplications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.ProgressSpinner;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static android.support.wearable.input.RemoteInputIntent.ACTION_REMOTE_INPUT;
import static android.support.wearable.input.RemoteInputIntent.EXTRA_REMOTE_INPUTS;

public class CounterConfigActivity extends WearableActivity implements WearableListView.ClickListener {

    private static final String TAG = "CA";

    private static final String KEY_COUNTER_NAME_TEXT = "counter";

    private String[] mElements;
    private String[] mIds;
    private Integer[] mValues;
    private int mComplicationId;

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

        // Get the list component from the layout of the activity
        mListView =
                (WearableListView) findViewById(R.id.wearable_list);
        mSpinner = (ProgressSpinner) findViewById(R.id.load_spinner);

        mFrameLayout = (FrameLayout) findViewById(R.id.frame_layout);

        mComplicationId = getIntent().getIntExtra(
                android.support.wearable.complications.ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, -1);

        mSharedPrefs = getSharedPreferences("config", 0);
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

        Set<String> counters = mSharedPrefs.getStringSet("counters", null);
        if (counters != null) {
            mElements = new String[counters.size() + 1];
            mIds = new String[counters.size() + 1];
            mValues = new Integer[counters.size() + 1];

            mElements[0] = getString(R.string.new_counter_title);
            mIds[0] = "new";
            mValues[0] = -1;

            int i = 1;
            for (String id: counters) {
                mValues[i] = mSharedPrefs.getInt(id + ":val", 0);
                mElements[i] = mSharedPrefs.getString(id + ":name", "") + "  " + mValues[i];
                mIds[i] = id;
                Log.d(TAG, "UI provider " + id + " " + mElements[i]);

                i++;
            }
        } else {
            Log.d(TAG, "mSharedPrefs.getStringSet counters = null");

            mElements = new String[1];
            mIds = new String[1];
            mValues = new Integer[1];

            mElements[0] = getString(R.string.new_counter_title);
            mIds[0] = "new";
            mValues[0] = -1;

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

        if (mIds[pos].equals("new")) {
            createNewCounter();
            return;

        } else {
            Set<String> cids = mSharedPrefs.getStringSet("cids_for_" + mIds[pos],
                    new TreeSet<String>());
            cids.add(Integer.toString(mComplicationId));
            mSharedPrefs.edit()
                    .putString(mComplicationId + "_selected_counter_id", mIds[pos])
                    .putStringSet("cids_for_" + mIds[pos], cids)
                    .commit();
        }

        setResult(RESULT_OK);
        CounterComplicationService.requestUpdateComplication(this, mComplicationId);
        finish();
    }

    private void createNewCounter() {
        Set<String> counters = mSharedPrefs.getStringSet("counters", null);
        String defaultNamePrefix = getString(R.string.counter_name_prefix);
        String newName = defaultNamePrefix + "1";
        if (counters == null) {
            counters = new TreeSet<String>();
        } else {
            int currentMaxCount = 0;
            for (String cid : counters) {
                String name = mSharedPrefs.getString(cid + ":name", "");
                if (name.startsWith(defaultNamePrefix)) {
                    int val = Integer.parseInt(name.split("#")[1]);
                    if (val > currentMaxCount) {
                        currentMaxCount = val;
                    }
                }
            }
            newName = defaultNamePrefix + (currentMaxCount + 1);
        }

        RemoteInput[] remoteInputs = new RemoteInput[] {
                new RemoteInput.Builder(KEY_COUNTER_NAME_TEXT)
                        .setLabel("Counter Name")
                        .setChoices(new String[] {newName})
                        .build()
        };
        Intent intent = new Intent(ACTION_REMOTE_INPUT);
        intent.putExtra(EXTRA_REMOTE_INPUTS, remoteInputs);
        startActivityForResult(intent, 0);

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle results = RemoteInput.getResultsFromIntent(data);

        if (results != null) {
            String newCounterTitle = results.getCharSequence(KEY_COUNTER_NAME_TEXT).toString();

            Set<String> counters = mSharedPrefs.getStringSet("counters", null);
            if (counters == null) {
                counters = new TreeSet<String>();
            }
            String newId = generateCounterId();

            counters.add(newId);

            mSharedPrefs.edit()
                    .putString(newId + ":name", newCounterTitle)
                    .putInt(newId + ":val", 0)
                    .putStringSet("counters", counters).commit();

            showListFromSharedPrefs();
        }
    }

    private String generateCounterId() {
        SecureRandom rnd = new SecureRandom();
        return Long.toHexString(rnd.nextLong()) + Long.toHexString(rnd.nextLong());
    }

    @Override
    public void onTopEmptyRegionClick() {

    }

    private void resetCounterAt(int pos) {
        resetCounterWithId(mIds[pos]);
    }

    private void resetCounterWithId(String cid) {
        mSharedPrefs.edit().putInt(cid + ":val", 0).commit();

        Set<String> complicationIds = mSharedPrefs.getStringSet("cids_for_" + cid, null);
        if (complicationIds != null) {
            for (String compId : complicationIds) {
                CounterComplicationService.requestUpdateComplication(this, Integer.parseInt(compId));
            }
        }

        showListFromSharedPrefs();
    }

    private final class Adapter extends WearableListView.Adapter {
        private String[] mDataset;
        private final LayoutInflater mInflater;

        public Adapter(Context context, String[] dataset) {
            mInflater = LayoutInflater.from(context);
            mDataset = dataset;
        }

        public class ItemViewHolder extends WearableListView.ViewHolder {
            private TextView textView;
            private ImageView imageView;
            public ItemViewHolder(View itemView) {
                super(itemView);
                // find the text view within the custom item's layout
                textView = (TextView) itemView.findViewById(R.id.name);
                imageView = (ImageView) itemView.findViewById(R.id.circle);
            }
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder,
                                     final int position) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            view.setText(mDataset[position]);

            if (position == 0) {
                itemHolder.imageView.setVisibility(View.GONE);
            } else {
                itemHolder.imageView.setVisibility(View.VISIBLE);

                itemHolder.imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetCounterAt(position);
                    }
                });
            }

            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mDataset != null ? mDataset.length : 0;
        }
    }

}
