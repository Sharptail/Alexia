package sg.edu.nyp.alexia.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Spencer on 9/2/2017.
 */

public class CheckinReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Close notification on check-in
        int appointIndex = intent.getIntExtra("Notification", -1);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(appointIndex);
    }
}
