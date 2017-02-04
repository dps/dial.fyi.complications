package io.singleton.wearcomplications;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.support.wearable.complications.ComplicationProviderService;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class CounterIntentService extends IntentService {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
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
                    Toast.makeText(getApplicationContext(), "no cid: " + intent, Toast.LENGTH_LONG);
                }
                CounterSettings.getInstance(this).incrementCounter(complicationId);
                CounterComplicationService.requestUpdateComplication(this, complicationId);
            }
        }
    }

}
