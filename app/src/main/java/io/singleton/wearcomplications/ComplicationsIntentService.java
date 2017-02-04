package io.singleton.wearcomplications;

import android.app.IntentService;
import android.app.RemoteInput;
import android.content.Intent;
import android.content.Context;
import android.support.wearable.complications.ComplicationProviderService;
import android.widget.Toast;

import static android.support.wearable.input.RemoteInputIntent.ACTION_REMOTE_INPUT;
import static android.support.wearable.input.RemoteInputIntent.EXTRA_REMOTE_INPUTS;

public class ComplicationsIntentService extends IntentService {
    static final String ACTION_INCR = "io.singleton.wearcomplications.action.INCR";
    static final String ACTION_NEW_NOTE = "io.singleton.wearcomplications.action.NEW_NOTE";

    public ComplicationsIntentService() {
        super("ComplicationsIntentService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_INCR.equals(action)) {
                incrementCounter(intent);
            } else if (ACTION_NEW_NOTE.equals(action)) {
                newNote(intent);
            }
        }
    }

    private void newNote(Intent intent) {
        startActivity(new Intent(this, NewNoteActivity.class));
    }

    private void incrementCounter(Intent intent) {
        int complicationId = intent.getIntExtra(ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID, -1);
        if (complicationId == -1) {
            return;
        }
        CounterSettings.getInstance(this).incrementCounter(complicationId);
    }

}
