package io.singleton.wearcomplications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static android.R.attr.type;


public class FeedDownloadingStore {

    private static final String TAG = "ACIDS";
    public static final String ACTION_UPDATE_START = "io.singleton.wearcomplications.UPDATE_START";
    public static final String ACTION_UPDATE_COMPLETE = "io.singleton.wearcomplications.UPDATE_COMPLETE";
    public static final int MAX_NUM_RETRIES = 2;
    public static final float BACKOFF_MULTIPLIER = 1f;
    public static final String FILE_PREFIX = "ac-";
    public static final int REGISTER_INITIAL_TIMEOUT_MS = 60000;
    private static final long TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000;
    public static final int FILE_READ_BUFFER_SIZE_BYTES = 8096;

    private final File mCacheDir;
    private Context mContext;
    private MessageDigest mDigester;
    private Map<String, String> mUrlHashes = new HashMap<String, String>();


    private static FeedDownloadingStore sInstance;

    public static synchronized FeedDownloadingStore getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FeedDownloadingStore(context);
        }
        return sInstance;
    }

    private RequestQueue mRequestQueue;
    private Settings mSettings;
    private LocalBroadcastManager mLocalBroadcastManager;

    public FeedDownloadingStore(Context context) {
        mContext = context;
        mCacheDir = mContext.getCacheDir();
        mRequestQueue = Volley.newRequestQueue(context);
        mSettings = Settings.getInstance(context);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, "onErrorResponse " + error);
        }
    };

    private RetryPolicy mRetryPolicy =
            new DefaultRetryPolicy(REGISTER_INITIAL_TIMEOUT_MS,
                    MAX_NUM_RETRIES, BACKOFF_MULTIPLIER);


    public void register() {
        String when = Long.toString(System.currentTimeMillis());
        JSONObject json = new JSONObject();
        try {
            json.put("id", mSettings.getLocalId());
            json.put("when", when);
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing request ", e);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                Constants.getRegisterUrl(),
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String token = null;
                        try {
                            token = response.getString("token");
                            mSettings.setConfigToken(token);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response", e);
                        }

                    }
                },
                mErrorListener
        );
        request.setRetryPolicy(mRetryPolicy);
        mRequestQueue.add(request);
    }

    public void updateFeedIfStale() {
        long lastUpdate = mSettings.getLastUpdateTimeMs();
        if (System.currentTimeMillis() - lastUpdate > TWO_HOURS_MILLIS) {
            updateFeed();
        }
    }

    public void onFeedUpdated() {
        mSettings.setLastUpdateTimeToNow();
        RssComplicationService.requestUpdateAll(mContext);
        broadcastUpdateComplete();
    }

    public void onCurlUpdated() {
        CurlComplicationService.requestUpdateAll(mContext);
        broadcastUpdateComplete();
    }

    private void broadcastUpdateComplete() {
        Intent broadcast = new Intent();
        broadcast.setAction(ACTION_UPDATE_COMPLETE);
        mLocalBroadcastManager.sendBroadcast(broadcast);
    }

    private synchronized String urlHash(String url) {
        try {
            if (mDigester == null) {
                mDigester = MessageDigest.getInstance("MD5");
            }
            mDigester.reset();
            mDigester.update(url.getBytes());
            return FILE_PREFIX + (new BigInteger(1, mDigester.digest())).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Digest algorithm not found", e);
        }
        return null;
    }


    private static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read;
        byte[] data = new byte[FILE_READ_BUFFER_SIZE_BYTES];
        while ((read = is.read(data, 0, data.length)) >= 0) {
            buffer.write(data, 0, read);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    public void updateFeed() {
        String when = Long.toString(System.currentTimeMillis());
        JSONObject body = new JSONObject();
        try {
            body.put("id", mSettings.getLocalId());
            body.put("token", mSettings.getConfigToken());
            body.put("when", when);
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing request ", e);
        }

        JSONObject json = body;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                Constants.getFeedUrl(mSettings.getConfigToken()),
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // TODO parse response
                        Log.d(TAG, response.toString());
                        processFeedResponse(response);
                    }
                },
                mErrorListener
        );
        request.setRetryPolicy(mRetryPolicy);
        mRequestQueue.add(request);
    }

    public void updateCurlSettings() {
        String when = Long.toString(System.currentTimeMillis());
        JSONObject body = new JSONObject();
        try {
            body.put("id", mSettings.getLocalId());
            body.put("token", mSettings.getConfigToken());
            body.put("when", when);
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing request ", e);
        }

        JSONObject json = body;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                Constants.getCurlUrl(mSettings.getConfigToken()),
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // TODO parse response
                        Log.d(TAG, response.toString());
                        processCurlResponse(response);
                    }
                },
                mErrorListener
        );
        request.setRetryPolicy(mRetryPolicy);
        mRequestQueue.add(request);
    }

    private void processCurlResponse(JSONObject response) {
        SharedPreferences prefs = mContext.getSharedPreferences("feed", 0);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            String iconName = response.getString("icon");
            String curlUrl = response.getString("url");
            String method = response.getString("method");

            editor.putString("curl-icon", iconName)
                    .putString("curl-url", curlUrl)
                    .putString("curl-method", method).commit();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        onCurlUpdated();
    }

    private void processFeedResponse(JSONObject response) {
        SharedPreferences prefs = mContext.getSharedPreferences("feed", 0);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            String feedTitle = response.getString("title");
            editor.putString("feedTitle", feedTitle);
            JSONArray entries = response.getJSONArray("entries");
            editor.putInt("entriesLength", entries.length());
            editor.putInt("currentEntry", 0);
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                editor.putString("entry-" + i + "-title", entry.getString("title"));

                if (entry.has("description")) {
                    editor.putString("entry-" + i + "-description", entry.getString("description"));
                } else {
                    editor.remove("entry-" + i + "-description");
                }

                if (entry.has("link")) {
                    editor.putString("entry-" + i + "-link", entry.getString("link"));
                } else {
                    editor.remove("entry-" + i + "-link");
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.commit();
        onFeedUpdated();
    }

    public void makeGenericCurlRequest(int complicationId) {

        SharedPreferences prefs = mContext.getSharedPreferences("feed", 0);
        String url = prefs.getString("curl-url", null);
        String method = prefs.getString("curl-method", null);
        if (url == null || method == null) {
            return;
        }


        int requestMethod = Request.Method.POST;
        if (method.equals("GET")) {
            requestMethod = Request.Method.GET;
        }

        StringRequest request = new StringRequest(requestMethod, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                CurlComplicationService.onHttpRequestCompleted(response, mContext);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error Response " + error);
                CurlComplicationService.onHttpRequestError(error, mContext);
            }
        });

        request.setRetryPolicy(mRetryPolicy);
        mRequestQueue.add(request);
    }

    static class Entry {
        String title;
        String link;
        String description;
    }

    static Entry getNextEntry(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("feed", 0);

        int len = prefs.getInt("entriesLength", -1);
        int current = prefs.getInt("currentEntry", -1);
        if (len < 0 || current < 0) {
            return null;
        }

        current = (current + 1) % len;
        prefs.edit().putInt("currentEntry", current).commit();
        Log.d(TAG, "currentEntry " + current);
        Entry result = new Entry();
        result.title = prefs.getString("entry-" + current + "-title", "");
        result.link = prefs.getString("entry-" + current + "-link", "");
        result.description = prefs.getString("entry-" + current + "-description", "");
        return result;
    }
}
