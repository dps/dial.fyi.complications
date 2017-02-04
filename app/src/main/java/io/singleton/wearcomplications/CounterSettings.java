package io.singleton.wearcomplications;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class CounterSettings {

    public static final String PREF_COUNTER = "counter";
    public static final String PREF_SET_CONFIG = "config";


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
        String cid = mPrefs.getString(complicationId + "_selected_counter_id", null);
        if (cid != null) {
            return mPrefs.getInt(cid + ":val", 0);
        }
        return -1;
    }

    public String getShortTitle(int complicationId) {
        String cid = mPrefs.getString(complicationId + "_selected_counter_id", null);
        String name = mPrefs.getString(cid + ":name", null);

        if (name == null || name.startsWith(mContext.getString(R.string.counter_name_prefix))) {
            return "count";
        } else {
            return name;
        }
    }

    public int incrementCounter(int complicationId) {
        String cid = mPrefs.getString(complicationId + "_selected_counter_id", null);
        int current = mPrefs.getInt(cid + ":val", 0);
        mPrefs.edit().putInt(cid + ":val", current + 1).commit();
        return current + 1;
    }

}
