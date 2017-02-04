package io.singleton.wearcomplications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class NetworkRequester {

    private static final String TAG = "FXNR";
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;
    private static final long ONE_HOUR_MILLIS = 60 * 60 * 1000;
    public static final String ACTION_LISTING_UPDATE_COMPLETE = "listing_update_complete";
    public static final String ACTION_LISTING_UPDATE_ERROR = "listing_update_error";

    private static NetworkRequester sInstance;

    private final RequestQueue mRequestQueue;
    private final LocalBroadcastManager mLocalBroadcastManager;

    public NetworkRequester(Context ctx) {
        mRequestQueue = Volley.newRequestQueue(ctx);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(ctx);
    }

    public static synchronized NetworkRequester getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new NetworkRequester(ctx);
        }
        return sInstance;
    }


    private void makeRequest(Request request) {
        mRequestQueue.add(request);
    }


    public ImageLoader getImageLoader() {
        return new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {

            HashMap<String, Bitmap> mHashMap = new HashMap<>();
            @Override
            public Bitmap getBitmap(String url) {
                return mHashMap.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                mHashMap.put(url, bitmap);
            }
        });
    }
}
