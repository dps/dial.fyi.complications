package io.singleton.wearcomplications;

import android.app.AlertDialog;
import android.app.RemoteInput;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.Set;
import java.util.TreeSet;

import static android.support.wearable.input.RemoteInputIntent.ACTION_REMOTE_INPUT;
import static android.support.wearable.input.RemoteInputIntent.EXTRA_REMOTE_INPUTS;

public class CounterConfigActivity extends WearableActivity {

    private static final String KEY_COUNTER_NAME_TEXT = "counter";
    public static final String KEY_COUNTERS = "counters";
    public static final String ID_NEW = "new";
    public static final String PREFS_KEY_PREFIX_CIDS_FOR = "cids_for_";

    private String[] mElements;
    private String[] mIds;
    private Integer[] mValues;
    private int mComplicationId;

    WearableListView mListView;
    private SharedPreferences mSharedPrefs;
    private FrameLayout mFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        // Get the list component from the layout of the activity
        mListView = (WearableListView) findViewById(R.id.wearable_list);

        mFrameLayout = (FrameLayout) findViewById(R.id.frame_layout);

        mComplicationId = getIntent().getIntExtra(
                android.support.wearable.complications.ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, -1);

        mSharedPrefs = getSharedPreferences("config", 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        showListFromSharedPrefs();
    }


    private void showListFromSharedPrefs() {

        Set<String> counters = mSharedPrefs.getStringSet(KEY_COUNTERS, null);
        if (counters != null) {
            mElements = new String[counters.size() + 1];
            mIds = new String[counters.size() + 1];
            mValues = new Integer[counters.size() + 1];

            mElements[0] = getString(R.string.new_counter_title);
            mIds[0] = ID_NEW;
            mValues[0] = -1;

            int i = 1;
            for (String id: counters) {
                mValues[i] = mSharedPrefs.getInt(id + CounterSettings.KEY_SUFFIX_VAL, 0);
                mElements[i] = mSharedPrefs.getString(id + CounterSettings.KEY_SUFFIX_NAME, "") + "  " + mValues[i];
                mIds[i] = id;
                i++;
            }
        } else {
            mElements = new String[1];
            mIds = new String[1];
            mValues = new Integer[1];

            mElements[0] = getString(R.string.new_counter_title);
            mIds[0] = ID_NEW;
            mValues[0] = -1;
        }
        mListView.setVisibility(View.VISIBLE);
        mFrameLayout.setVisibility(View.VISIBLE);

        // Assign an adapter to the list
        mListView.setAdapter(new Adapter(this, mElements));

        mListView.invalidate();

    }


    private void handleClickAtPos(int pos) {
        if (mIds[pos].equals(ID_NEW)) {
            createNewCounter();
            return;
        } else {
            Set<String> cids = mSharedPrefs.getStringSet(PREFS_KEY_PREFIX_CIDS_FOR + mIds[pos],
                    new TreeSet<String>());
            cids.add(Integer.toString(mComplicationId));
            mSharedPrefs.edit()
                    .putString(mComplicationId + CounterSettings.PREFS_KEY_SUFFIX_SELECTED_COUNTER_ID, mIds[pos])
                    .putStringSet(PREFS_KEY_PREFIX_CIDS_FOR + mIds[pos], cids)
                    .commit();
        }

        setResult(RESULT_OK);
        CounterComplicationService.requestUpdateComplication(this, mComplicationId);
        finish();
    }

    private void createNewCounter() {
        Set<String> counters = mSharedPrefs.getStringSet(KEY_COUNTERS, null);
        String defaultNamePrefix = getString(R.string.counter_name_prefix);
        String newName = defaultNamePrefix + "1";
        if (counters == null) {
            counters = new TreeSet<String>();
        } else {
            int currentMaxCount = 0;
            for (String cid : counters) {
                String name = mSharedPrefs.getString(cid + CounterSettings.KEY_SUFFIX_NAME, "");
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
                        .setLabel(getString(R.string.counter_name))
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

            Set<String> counters = mSharedPrefs.getStringSet(KEY_COUNTERS, null);
            if (counters == null) {
                counters = new TreeSet<String>();
            }
            String newId = generateCounterId();

            counters.add(newId);

            mSharedPrefs.edit()
                    .putString(newId + CounterSettings.KEY_SUFFIX_NAME, newCounterTitle)
                    .putInt(newId + CounterSettings.KEY_SUFFIX_VAL, 0)
                    .putStringSet(KEY_COUNTERS, counters).commit();

            showListFromSharedPrefs();
        }
    }

    private String generateCounterId() {
        SecureRandom rnd = new SecureRandom();
        return Long.toHexString(rnd.nextLong()) + Long.toHexString(rnd.nextLong());
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
            final ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            view.setText(mDataset[position]);

            itemHolder.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleClickAtPos(position);
                }
            });

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

                itemHolder.textView.setOnLongClickListener(new View.OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        AlertDialog.Builder ad = new AlertDialog.Builder(CounterConfigActivity.this);
                        ad.setTitle(getString(R.string.delete_confirm));
                        ad.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteCounterAt(position);
                            }
                        });
                        ad.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        ad.setIcon(android.R.drawable.ic_delete);
                        ad.setCancelable(true);
                        ad.show();

                        return true;
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

    private void deleteCounterAt(int pos) {
        deleteCounterWithId(mIds[pos]);
    }

    private void deleteCounterWithId(String cid) {
        Set<String> counters = mSharedPrefs.getStringSet(KEY_COUNTERS, null);
        Set<String> complicationIds = mSharedPrefs.getStringSet(CounterConfigActivity.PREFS_KEY_PREFIX_CIDS_FOR + cid, null);

        if (counters != null) {
            counters.remove(cid);
            mSharedPrefs.edit()
                    .putStringSet(KEY_COUNTERS, counters)
                    .remove(cid + CounterSettings.KEY_SUFFIX_NAME)
                    .remove(cid + CounterSettings.KEY_SUFFIX_VAL)
                    .remove(PREFS_KEY_PREFIX_CIDS_FOR + cid)
                    .commit();
        }

        SharedPreferences.Editor deleter = mSharedPrefs.edit();
        if (complicationIds != null) {
            for (String compId : complicationIds) {
                CounterComplicationService.requestUpdateComplication(this, Integer.parseInt(compId));
                deleter.remove(compId + CounterSettings.PREFS_KEY_SUFFIX_SELECTED_COUNTER_ID);
            }
        }
        deleter.commit();

        showListFromSharedPrefs();
    }

    private void resetCounterAt(int pos) {
        resetCounterWithId(mIds[pos]);
    }

    private void resetCounterWithId(String cid) {
        mSharedPrefs.edit().putInt(cid + CounterSettings.KEY_SUFFIX_VAL, 0).commit();

        Set<String> complicationIds =
                mSharedPrefs.getStringSet(PREFS_KEY_PREFIX_CIDS_FOR + cid, null);
        if (complicationIds != null) {
            for (String compId : complicationIds) {
                CounterComplicationService.requestUpdateComplication(this, Integer.parseInt(compId));
            }
        }

        showListFromSharedPrefs();
    }
}
