package io.singleton.wearcomplications;

import android.util.Log;

/**
 * Created by davidsingleton on 5/28/16.
 */
public class Emoji {

    private static final String TAG = "Em";

    private static String EMOJI_FACE_SCREAMING_IN_FEAR = "\uD83D\uDE31";
    private static String EMOJI_FACE_WITH_OPEN_MOUTH_AND_COLD_SWEAT = "\uD83D\uDE30";
    private static String EMOJI_FEARFUL_FACE = "\uD83D\uDE28";
    private static String EMOJI_LOUDLY_CRYING_FACE = "\uD83D\uDE2D";
    private static String EMOJI_CRYING_FACE = "\uD83D\uDE22";
    private static String EMOJI_ANGRY_FACE = "\uD83D\uDE20";
    private static String EMOJI_CONFOUNDED_FACE = "\uD83D\uDE16";
    private static String EMOJI_WHITE_FROWNING_FACE = "☹️";
    private static String EMOJI_SLIGHTLY_FROWINING_FACE = "\uD83D\uDE41";
    private static String EMOJIFACEWITHROLLINGEYES = "\uD83D\uDE44";
    private static String EMOJI_SLIGHTLY_SMILING_FACE = "\uD83D\uDE42";
    private static String EMOJI_WHITE_SMILING_FACE = "☺";
    private static String EMOJI_GRINNING_FACE = "\uD83D\uDE00";
    private static String EMOJI_GRINNING_FACE_WITH_SMILING_EYES = "\uD83D\uDE01";
    private static String EMOJI_FACE_WITH_TEARS_OF_JOY = "\uD83D\uDE02";
    private static String EMOJI_SMILING_FACE_WITH_SUNGLASSES = "\uD83D\uDE0E";
    private static String EMOJI_SMILING_FACE_WITH_HALO = "\uD83D\uDE07";

    private static String[] EMOJI_SPECTRUM = {
            EMOJI_FACE_SCREAMING_IN_FEAR,
            EMOJI_FACE_WITH_OPEN_MOUTH_AND_COLD_SWEAT,
            EMOJI_FEARFUL_FACE,
            EMOJI_LOUDLY_CRYING_FACE,
            EMOJI_CRYING_FACE,
            EMOJI_ANGRY_FACE,
            EMOJI_CONFOUNDED_FACE,
            EMOJI_WHITE_FROWNING_FACE,
            EMOJI_SLIGHTLY_FROWINING_FACE,
            EMOJIFACEWITHROLLINGEYES,  //9
            EMOJI_SLIGHTLY_SMILING_FACE,
            EMOJI_WHITE_SMILING_FACE,
            EMOJI_GRINNING_FACE,  // goal
            EMOJI_GRINNING_FACE_WITH_SMILING_EYES,
            EMOJI_FACE_WITH_TEARS_OF_JOY,
            EMOJI_SMILING_FACE_WITH_SUNGLASSES,
            EMOJI_SMILING_FACE_WITH_HALO};

    private static int SPECTRUM_INDEX_MID_POINT = 9;
    private static int SPECTRUM_INDEX_GOAL = 12;

    private static float DAY_SCORE = 9;
    private static int SPECTRUM_NATURAL_MIN = 9;
    private static float SPECTRUM_NATURAL_WIDTH = 4;

    private static int STEPS_STARTER_GOAL = 300;
    private static int WAKE_UP_MIN_OF_DAY = 8 * 60;
    private static int SLEEP_MIN_OF_DAY = 23 * 60;
    /*
steps   ^
expected|                                 XX----+ goal
        |                               XXX
        |                             XXX
        |                           XXX
        |                         XX
        |                     XXXX
        |                  XXX
        |               XXX
        |             XX
starter +-----------XX
goal    |
        +--------------------------------------->
       12am          8am                  11pm
                                             time of day

     Linearly interpolate how far behind the goal line user is and map to emoji MIN thru GOAL.

     */

    static int expectedStepsForTimeOfDay(int minuteOfDay, int goal) {
        float goalGradient = (((float)goal) - STEPS_STARTER_GOAL) / (SLEEP_MIN_OF_DAY - WAKE_UP_MIN_OF_DAY);
        if (minuteOfDay <= WAKE_UP_MIN_OF_DAY) {
            return STEPS_STARTER_GOAL;
        }
        if (minuteOfDay > SLEEP_MIN_OF_DAY) {
            return goal;
        }
        return STEPS_STARTER_GOAL + (int)((minuteOfDay - WAKE_UP_MIN_OF_DAY) * goalGradient);
    }

    static String computeEmojiForTimeOfDayAndProgressToGoal(int minuteOfDay, int progress, int goal) {
        int timeOfDayGoal = expectedStepsForTimeOfDay(minuteOfDay, goal);
        Log.d(TAG, "computeEmojiForTimeOfDayAndProgressToGoal " + minuteOfDay + " " + progress + " " + goal + " " + timeOfDayGoal);

        if (progress > timeOfDayGoal * 2) {
            return EMOJI_SPECTRUM[EMOJI_SPECTRUM.length - 1];
        }
        if (progress > timeOfDayGoal * 1.5) {
            return EMOJI_SPECTRUM[EMOJI_SPECTRUM.length - 2];
        }
        if (progress >= timeOfDayGoal) {
            return EMOJI_SPECTRUM[SPECTRUM_INDEX_GOAL];
        }

        float fracProgressToTimeOfDayGoal = (float)progress / (float)timeOfDayGoal;

        int index = (int)(fracProgressToTimeOfDayGoal * SPECTRUM_INDEX_GOAL);

        Log.d(TAG, "computeEmojiForTimeOfDayAndProgressToGoal " + index + " " + fracProgressToTimeOfDayGoal + " " + index);

        return EMOJI_SPECTRUM[index];
    }

}
