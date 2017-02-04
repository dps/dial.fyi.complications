package io.singleton.wearcomplications;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CounterSettings {

    public static final String PREF_SET_CONFIG = "config";
    public static final String KEY_SUFFIX_VAL = ":val";
    public static final String KEY_SUFFIX_NAME = ":name";
    public static final String PREFS_KEY_SUFFIX_SELECTED_COUNTER_ID = "_selected_counter_id";


    private static CounterSettings mInstance;

    static synchronized CounterSettings getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new CounterSettings(ctx);
        }
        return mInstance;
    }

    private final Context mContext;
    private SharedPreferences mPrefs;

    private CounterSettings(Context ctx) {
        mContext = ctx;
        mPrefs = ctx.getSharedPreferences(PREF_SET_CONFIG, 0);
    }


    public int getCounter(int complicationId) {
        String cid = mPrefs.getString(complicationId + PREFS_KEY_SUFFIX_SELECTED_COUNTER_ID, null);
        if (cid != null) {
            return mPrefs.getInt(cid + KEY_SUFFIX_VAL, 0);
        }
        return -1;
    }

    public String getShortTitle(int complicationId) {
        String cid = mPrefs.getString(complicationId + PREFS_KEY_SUFFIX_SELECTED_COUNTER_ID, null);
        String name = mPrefs.getString(cid + KEY_SUFFIX_NAME, null);

        if (name == null || name.startsWith(mContext.getString(R.string.counter_name_prefix))) {
            return mContext.getString(R.string.counter);
        } else {
            return name;
        }
    }

    public int incrementCounter(int complicationId) {
        String cid = mPrefs.getString(complicationId + PREFS_KEY_SUFFIX_SELECTED_COUNTER_ID, null);
        int current = mPrefs.getInt(cid + KEY_SUFFIX_VAL, 0);
        mPrefs.edit().putInt(cid + KEY_SUFFIX_VAL, current + 1).commit();

        Set<String> complicationIds = mPrefs.getStringSet(
                CounterConfigActivity.PREFS_KEY_PREFIX_CIDS_FOR + cid, null);
        if (complicationIds != null) {
            for (String compId : complicationIds) {
                CounterComplicationService.requestUpdateComplication(mContext, Integer.parseInt(compId));
            }
        }

        return current + 1;
    }

}
