package io.singleton.wearcomplications;

import android.app.Activity;
import android.app.RemoteInput;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

import static android.support.wearable.input.RemoteInputIntent.ACTION_REMOTE_INPUT;
import static android.support.wearable.input.RemoteInputIntent.EXTRA_REMOTE_INPUTS;

public class NewNoteActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RemoteInput[] remoteInputs = new RemoteInput[] {
                new RemoteInput.Builder("note")
                        .setLabel(getString(R.string.new_note_provider_name))
                        .build()
        };
        Intent intent = new Intent(ACTION_REMOTE_INPUT);
        intent.putExtra(EXTRA_REMOTE_INPUTS, remoteInputs);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        Bundle results = RemoteInput.getResultsFromIntent(data);

        if (results != null) {
            String note = results.getCharSequence("note").toString();

            getSharedPreferences("notes", 0).edit()
                    .putString("note-" + System.currentTimeMillis(), note)
                    .putString("latest-note", note).commit();

            NewestNoteComplicationService.requestUpdateAll(this);
        }
        finish();
    }
}
