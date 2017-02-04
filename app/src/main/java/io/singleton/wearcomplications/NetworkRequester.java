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

    void broadcastListingUpdateComplete() {
        Intent intent = new Intent();
        intent.setAction(ACTION_LISTING_UPDATE_COMPLETE);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    void broadcastListingUpdateError() {
        Intent intent = new Intent();
        intent.setAction(ACTION_LISTING_UPDATE_ERROR);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    //private final static String BASE_URL = "http://netcomps.us.davidsingleton.org/";
    private final static String BASE_URL = "http://192.168.43.170:8080/";

    private void makeRequest(Request request) {
        mRequestQueue.add(request);
    }

    public static void makeListingRequest(final Context ctx) {
        Log.d(TAG, "makeListingRequest");
        final NetworkRequester requester = getInstance(ctx);

        JsonObjectRequest listingRequest = new JsonObjectRequest(Request.Method.GET,
                BASE_URL + "list", new JSONObject(), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "makeListingRequest onResponse");

                final SharedPreferences sharedPreferences = ctx.getSharedPreferences("list", 0);
                Set<String> providerIds = new TreeSet<>();
                SharedPreferences.Editor edit = sharedPreferences.edit();

                try {
                    JSONArray providers = response.getJSONArray("provider_list");

                    for (int i = 0; i < providers.length(); i++) {
                        JSONObject provider = providers.getJSONObject(i);
                        String pid = provider.getString("id");
                        Log.d(TAG, "provider: " + pid);

                        providerIds.add(pid);
                        edit.putString(pid + ":name", provider.getString("name"));
                        edit.putString(pid + ":url", provider.getString("url"));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                edit.putStringSet("providers", providerIds);
                long now = System.currentTimeMillis();
                edit.putLong("provider_list_updated", now);
                edit.commit();

                requester.broadcastListingUpdateComplete();

            }},
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, "makeListingRequest onErrorResponse " + error);

                    requester.broadcastListingUpdateError();
                }
            });
        requester.makeRequest(listingRequest);
    }

    public static void makeUpdateRequest(final Context ctx,
                                         final int complicationId, final int type, final ComplicationManager manager) {
        NetworkRequester requester = getInstance(ctx);

        final SharedPreferences sharedPreferences = ctx.getSharedPreferences("config", 0);
        final String name = sharedPreferences.getString(complicationId + "_selected_provider_name", "demo");
        final String id = sharedPreferences.getString(complicationId + "_selected_provider_id", "demo");
        final String url = sharedPreferences.getString(complicationId + "_selected_provider_url", "demourl");

        final Intent intent = new Intent(ctx, ConfigActivity.class);
        intent.putExtra(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, complicationId);
        final PendingIntent pi = PendingIntent.getActivity(ctx, complicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final String cachedData = sharedPreferences.getString(id + "_data", "");
        final long cachedAgeMillis = System.currentTimeMillis() - sharedPreferences.getLong(id + "_when", 0);
        if (cachedAgeMillis < ONE_HOUR_MILLIS) {
            ComplicationService.updateComplication(cachedData, id, complicationId, type, manager, pi, ctx);
            return;
        }
        // Show '-' while fetching...
        ComplicationService.updateComplication(null, id, complicationId, type, manager, pi, ctx);

        StringRequest request = new StringRequest(Request.Method.GET, url + "update", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Network Response: " + response);
                sharedPreferences.edit()
                        .putString(id + "_data", response)
                        .putLong(id + "_when", System.currentTimeMillis())
                        .apply();
                ComplicationService.updateComplication(response, id, complicationId, type, manager, pi, ctx);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error Response " + error);

                String data = cachedAgeMillis < ONE_DAY_MILLIS ? cachedData : null;

                ComplicationService.updateComplication(data, id, complicationId, type, manager, pi, ctx);
            }
        });
        requester.makeRequest(request);

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
