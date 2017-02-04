package io.singleton.wearcomplications;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.ProviderUpdateRequester;

public class CounterComplicationService extends ComplicationProviderService {

    @Override
    public void onComplicationUpdate(int complicationId, int type,
                                     ComplicationManager manager) {

        CounterSettings counterSettings = CounterSettings.getInstance(this);

        if (type == ComplicationData.TYPE_SHORT_TEXT) {
            ComplicationData.Builder builder = new ComplicationData.Builder(type);
            builder.setShortText(ComplicationText.plainText(
                    String.format("%d", counterSettings.getCounter(complicationId))));
            builder.setShortTitle(ComplicationText.plainText(counterSettings.getShortTitle(complicationId)));
            final Intent intent = new Intent(this, CounterIntentService.class);
            intent.setAction(CounterIntentService.ACTION_INCR);
            intent.putExtra(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, complicationId);
            final PendingIntent pi = PendingIntent.getService(this, complicationId, intent, 0);
            builder.setTapAction(pi);
            manager.updateComplicationData(complicationId, builder.build());
        } else {
            throw new IllegalStateException("Unexpected complication type: " + type);
        }
    }

    static void requestUpdateComplication(Context ctx, int id) {
        ProviderUpdateRequester requester = new ProviderUpdateRequester(
                ctx, ComponentName.createRelative(
                "io.singleton.wearcomplications",
                ".CounterComplicationService"));

        requester.requestUpdate(id);
    }
}
