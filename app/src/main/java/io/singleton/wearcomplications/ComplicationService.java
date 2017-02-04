package io.singleton.wearcomplications;

import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;

public class ComplicationService extends ComplicationProviderService {

    public static final String EXTRA_CT_TITLE = "ct_title";
    public static final String EXTRA_CT_BODY = "ct_body";
    public static final String EXTRA_CT_IMAGE = "ct_image";

    @Override
    public void onComplicationUpdate(int complicationId, int type,
                                     ComplicationManager manager) {


        throw new IllegalStateException("Unexpected complication type: " + type);

    }
}
