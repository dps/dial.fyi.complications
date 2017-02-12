package io.singleton.wearcomplications;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.android.volley.VolleyError;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class CurlComplicationService extends ComplicationProviderService {

    private static final String TAG = "CCS";

    @Override
    public void onComplicationUpdate(int complicationId, int type,
                                     ComplicationManager manager) {
        Log.d(TAG, "CurlComplicationService onComplicationUpdate");

        final Intent intent = new Intent(this, ComplicationsIntentService.class);
        intent.setAction(ComplicationsIntentService.ACTION_CURL);
        intent.putExtra(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, complicationId);
        final PendingIntent pi = PendingIntent.getService(this, complicationId, intent, 0);

        ComplicationData.Builder builder = new ComplicationData.Builder(type);
        builder.setTapAction(pi);

        SharedPreferences prefs = getSharedPreferences("feed", 0);
        String icon = prefs.getString("curl-icon", "-");
        if (prefs.contains("curl-inflight")) {
            icon = "replay";
        }
        if (type == ComplicationData.TYPE_SHORT_TEXT) {
            builder.setIcon(Icon.createWithResource(this, IconLookup.resIdForIconName(icon)));
            builder.setShortText(ComplicationText.plainText(icon));
            Log.d(TAG, "CurlComplicationService TYPE_SHORT_TEXT");
            manager.updateComplicationData(complicationId, builder.build());
        } else if (type == ComplicationData.TYPE_ICON) {
            builder.setIcon(Icon.createWithResource(this, IconLookup.resIdForIconName(icon)));
            Log.d(TAG, "CurlComplicationService TYPE_ICON");
            manager.updateComplicationData(complicationId, builder.build());

        } else {
            throw new IllegalStateException("Unexpected complication type: " + type);
        }
    }

    static void requestUpdateAll(Context ctx) {
        ProviderUpdateRequester requester = new ProviderUpdateRequester(
                ctx, ComponentName.createRelative(
                "io.singleton.wearcomplications",
                ".CurlComplicationService"));

        requester.requestUpdateAll();
    }

    public static void onHttpRequestCompleted(String response, Context ctx) {

        SharedPreferences.Editor edit = ctx.getSharedPreferences("feed", 0).edit();
        edit.remove("curl-inflight");
        if (response.startsWith("dial.fyi")) {
            String[] parts = response.split(" ");
            edit.putString("curl-icon", parts[1]).putString("curl-text", parts[2]);
        }
        edit.commit();
        requestUpdateAll(ctx);
    }

    public static void onHttpRequestError(VolleyError error, Context ctx) {
        SharedPreferences.Editor edit = ctx.getSharedPreferences("feed", 0).edit();
        edit.remove("curl-inflight");
        edit.putString("curl-error", error.getMessage());
        edit.commit();
        requestUpdateAll(ctx);
    }
}
