package io.singleton.wearcomplications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DailyTotalResult;

import java.util.Calendar;
import java.util.List;

public class EmojagotchiComplicationService extends ComplicationProviderService
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DailyTotalResult> {

    private static final String TAG = "EmoCS";

    private GoogleApiClient mGoogleApiClient;
    private Calendar mCalendar;

    private boolean mStepsRequested;
    private int mStepsTotal;
    private int mComplicationType;
    private ComplicationManager mComplicationManager;
    private int mComplicationId;

    @Override
    public void onCreate() {
        super.onCreate();

        mCalendar = Calendar.getInstance();

        mGoogleApiClient = new GoogleApiClient.Builder(EmojagotchiComplicationService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.RECORDING_API)
                .useDefaultAccount()
                .build();
    }

    public void onComplicationUpdate(int complicationId, int type,
                                     ComplicationManager manager) {

        mComplicationType = type;
        mComplicationManager = manager;
        mComplicationId = complicationId;

        mGoogleApiClient.connect();
    }

    public void completeComplicationUpdate() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        int minuteOfDay = mCalendar.get(Calendar.HOUR_OF_DAY) * 60 + mCalendar.get(Calendar.MINUTE);
        String emoji = Emoji.computeEmojiForTimeOfDayAndProgressToGoal(minuteOfDay, mStepsTotal, 10000);

        ComplicationData.Builder builder = new ComplicationData.Builder(mComplicationType);

        if (mComplicationType == ComplicationData.TYPE_LARGE_IMAGE) {
            Bitmap bitmap = textAsBitmap(emoji, 256, Color.BLACK);
            Icon icon = Icon.createWithBitmap(bitmap);
            builder.setLargeImage(icon);
        } else if (mComplicationType == ComplicationData.TYPE_ICON) {
            Bitmap test = textAsBitmap(emoji, 64, Color.BLACK);
            builder.setIcon(Icon.createWithBitmap(test));
        } else if (mComplicationType == ComplicationData.TYPE_RANGED_VALUE) {
            builder.setShortTitle(ComplicationText.plainText(emoji));
            builder.setMinValue(0);
            builder.setMaxValue(10000);
            builder.setValue(mStepsTotal);
        } else if (mComplicationType == ComplicationData.TYPE_SHORT_TEXT) {
            //builder.setShortTitle(ComplicationText.plainText("" + mStepsTotal));
            builder.setShortText(ComplicationText.plainText(emoji));
        }
        Intent intent = new Intent(this, EmojagotchiConfigActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setTapAction(pi);
        mComplicationManager.updateComplicationData(mComplicationId, builder.build());

        writeErrorReason(this, null);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private static void writeErrorReason(Context ctx, String error) {
        ctx.getSharedPreferences("emoja", 0).edit().putString("error", error).commit();
    }


    private void completeComplicationUpdateWithError() {

        ComplicationData.Builder builder = new ComplicationData.Builder(mComplicationType);

        builder.setIcon(Icon.createWithResource(this, android.R.drawable.ic_dialog_alert));
        Intent intent = new Intent(this, EmojagotchiConfigActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setTapAction(pi);
        mComplicationManager.updateComplicationData(mComplicationId, builder.build());

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public Bitmap textAsBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        Log.d(TAG, "mGoogleApiAndFitCallbacks.onConnected: " + connectionHint);
        mStepsRequested = false;

        // The subscribe step covers devices that do not have Google Fit App installed.
        subscribeToSteps();
        getTotalSteps();
    }

    private void subscribeToSteps() {
        Fitness.RecordingApi.subscribe(mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.i(TAG, "There was a problem subscribing.");
                        }
                    }
                });
    }

    private void getTotalSteps() {
        Log.d(TAG, "getTotalSteps()");
        if ((mGoogleApiClient != null)
                && (mGoogleApiClient.isConnected())
                && (!mStepsRequested)) {

            mStepsRequested = true;

            PendingResult<DailyTotalResult> stepsResult =
                    Fitness.HistoryApi.readDailyTotal(
                            mGoogleApiClient,
                            DataType.TYPE_STEP_COUNT_DELTA);

            stepsResult.setResultCallback(this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed " + connectionResult + " " + connectionResult.getErrorMessage());

        if (connectionResult.getErrorCode() == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            writeErrorReason(this, getResources().getString(R.string.play_services_upgrade));
        } else {
            writeErrorReason(this, getResources().getString(R.string.generic_error, connectionResult.toString()));
        }

        completeComplicationUpdateWithError();

    }

    @Override
    public void onResult(@NonNull DailyTotalResult dailyTotalResult) {
        Log.d(TAG, "mGoogleApiAndFitCallbacks.onResult(): " + dailyTotalResult);

        mStepsRequested = false;

        if (dailyTotalResult.getStatus().isSuccess()) {

            List<DataPoint> points = dailyTotalResult.getTotal().getDataPoints();;

            if (!points.isEmpty()) {
                mStepsTotal = points.get(0).getValue(Field.FIELD_STEPS).asInt();
                Log.d(TAG, "steps updated: " + mStepsTotal);

                completeComplicationUpdate();
            } else {
                mStepsTotal = 0;
                completeComplicationUpdate();
            }
        } else {
            Log.e(TAG, "onResult() failed! " + dailyTotalResult.getStatus().getStatusMessage());
            mStepsTotal = 0;
            writeErrorReason(this, dailyTotalResult.getStatus().getStatusMessage());
            completeComplicationUpdateWithError();
        }
    }
}
