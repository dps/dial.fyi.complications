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

public class NewestNoteComplicationService extends ComplicationProviderService {

    SharedPreferences mPrefs = null;

    @Override
    public void onComplicationUpdate(int complicationId, int type,
                                     ComplicationManager manager) {

        if (mPrefs == null) {
            mPrefs = getSharedPreferences("notes", 0);
        }
        
        final Intent intent = new Intent(this, ComplicationsIntentService.class);
        intent.setAction(ComplicationsIntentService.ACTION_NEW_NOTE);
        intent.putExtra(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, complicationId);
        final PendingIntent pi = PendingIntent.getService(this, complicationId, intent, 0);

        ComplicationData.Builder builder = new ComplicationData.Builder(type);
        builder.setTapAction(pi);
        builder.setIcon(Icon.createWithResource(this, R.drawable.ic_description_white_24dp));

        if (type == ComplicationData.TYPE_LONG_TEXT) {
            String note = mPrefs.getString("latest-note", "-");
            builder.setLongText(ComplicationText.plainText(note));
        } else {
            throw new IllegalStateException("Unexpected complication type: " + type);
        }
        manager.updateComplicationData(complicationId, builder.build());

    }

    static void requestUpdateComplication(Context ctx, int id) {
        ProviderUpdateRequester requester = new ProviderUpdateRequester(
                ctx, ComponentName.createRelative(
                "io.singleton.wearcomplications",
                ".NewestNoteComplicationService"));

        requester.requestUpdate(id);
    }

    static void requestUpdateAll(Context ctx) {
        ProviderUpdateRequester requester = new ProviderUpdateRequester(
                ctx, ComponentName.createRelative(
                "io.singleton.wearcomplications",
                ".NewestNoteComplicationService"));

        requester.requestUpdateAll();
    }


}
