package io.singleton.wearcomplications;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class RssComplicationService extends ComplicationProviderService {

    public static final String EXTRA_CT_TITLE = "ct_title";
    public static final String EXTRA_CT_BODY = "ct_body";
    public static final String EXTRA_CT_IMAGE = "ct_image";

    @Override
    public void onComplicationUpdate(int complicationId, int type,
                                     ComplicationManager manager) {

        if (type == ComplicationData.TYPE_LONG_TEXT) {

            ComplicationData.Builder builder = new ComplicationData.Builder(type);
            FeedDownloadingStore.Entry entry = FeedDownloadingStore.getNextEntry(this);
            String data = entry.title;
            if (data == null) {
                data = "-";
            } else {
                Intent intent = new Intent(this, ClickThruActivity.class);
                intent.putExtra(RssComplicationService.EXTRA_CT_TITLE, entry.title);
                intent.putExtra(RssComplicationService.EXTRA_CT_BODY, entry.description);
                PendingIntent pi = PendingIntent.getActivity(this, (int)Math.random(), intent, FLAG_ONE_SHOT);
                builder.setTapAction(pi);
            }
            builder.setLongText(ComplicationText.plainText(data));
            manager.updateComplicationData(complicationId, builder.build());
        } else {
            throw new IllegalStateException("Unexpected complication type: " + type);
        }

        FeedDownloadingStore.getInstance(this).updateFeedIfStale();
    }
}
