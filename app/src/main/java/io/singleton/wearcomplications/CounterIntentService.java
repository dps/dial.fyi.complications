package io.singleton.wearcomplications;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.wearable.complications.ComplicationProviderService;
import android.widget.Toast;

public class CounterIntentService extends IntentService {
    static final String ACTION_INCR = "io.singleton.wearcomplications.action.INCR";

    public CounterIntentService() {
        super("CounterIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_INCR.equals(action)) {
                int complicationId = intent.getIntExtra(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, -1);
                if (complicationId == -1) {
                    return;
                }
                CounterSettings.getInstance(this).incrementCounter(complicationId);
            }
        }
    }

}
