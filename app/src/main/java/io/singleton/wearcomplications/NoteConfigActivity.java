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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static android.support.wearable.input.RemoteInputIntent.ACTION_REMOTE_INPUT;
import static android.support.wearable.input.RemoteInputIntent.EXTRA_REMOTE_INPUTS;

public class NoteConfigActivity extends WearableActivity {

    public static final String ID_NEW = "new";
    private static final String KEY_NOTES = "notes";

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

        mSharedPrefs = getSharedPreferences("notes", 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        showListFromSharedPrefs();
    }


    private void showListFromSharedPrefs() {

        Map<String, ?> notes = mSharedPrefs.getAll();
        if (notes != null) {
            mElements = new String[notes.size()];  // We will always skip latest-...
            mIds = new String[notes.size()];
            mValues = new Integer[notes.size()];

            mElements[0] = getString(R.string.new_note);
            mIds[0] = ID_NEW;
            mValues[0] = -1;

            int i = 1;
            for (String key: notes.keySet()) {
                if (key.startsWith("latest")) {
                    continue;
                }
                mValues[i] = 0;
                mElements[i] = (String) notes.get(key);
                mIds[i] = key;
                i++;
            }
        } else {
            mElements = new String[1];
            mIds = new String[1];
            mValues = new Integer[1];

            mElements[0] = getString(R.string.new_note);
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
            createNewNote();
            return;
        } else {
            String note = mSharedPrefs.getString(mIds[pos], "");
            Intent intent = new Intent(this, ClickThruActivity.class);
            intent.putExtra(RssComplicationService.EXTRA_CT_TITLE, "Note");
            intent.putExtra(RssComplicationService.EXTRA_CT_BODY, note);
            startActivity(intent);
        }

        setResult(RESULT_OK);
        CounterComplicationService.requestUpdateComplication(this, mComplicationId);
        finish();
    }

    private void createNewNote() {
        startActivity(new Intent(this, NewNoteActivity.class));
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
            return new ItemViewHolder(mInflater.inflate(R.layout.note_list_item, null));
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
                        deleteNoteAt(position);
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

    private void deleteNoteAt(int pos) {
        final String id = mIds[pos];
        AlertDialog.Builder ad = new AlertDialog.Builder(NoteConfigActivity.this);
        ad.setTitle(getString(R.string.delete_confirm));
        ad.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteNoteWithId(id);
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
    }

    private void deleteNoteWithId(String id) {
        mSharedPrefs.edit().remove(id).commit();
        showListFromSharedPrefs();
    }



}
