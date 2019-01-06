package ee.zed.findme;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.content.IntentSender;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;


public class ActivityRecognitionService extends IntentService {
    public ActivityRecognitionService() {
        super(ActivityRecognitionService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            // Broadcast an intent containing the activity
            //Intent activityIntent = new Intent(BROADCAST_INTENT_ACTION);
            //activityIntent.putExtra(DETECTED_ACTIVITY_EXTRA_ID, mostProbableActivity);
            //sendBroadcast(activityIntent);
        }
    }


    // @Override
    // public void onResult(@NonNull Status status) {
    //     if (status.isSuccess()) {
    //         logger.d("Activity update request successful");
    //     } else if (status.hasResolution() && context instanceof Activity) {
    //         logger.w("Unable to register, but we can solve this - will startActivityForResult expecting result code " + RESULT_CODE + " (if received, please try again)");

    //         try {
    //             status.startResolutionForResult((Activity) context, RESULT_CODE);
    //         } catch (IntentSender.SendIntentException e) {
    //             logger.e(e, "problem with startResolutionForResult");
    //         }
    //     } else {
    //         // No recovery. Weep softly or inform the user.
    //         logger.e("Registering failed: " + status.getStatusMessage());
    //     }
    // }

}
