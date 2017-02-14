package sg.edu.nyp.alexia.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import sg.edu.nyp.alexia.services.SensorService;

/**
 * Created by Spencer on 6/2/2017.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Start SensorService on application boot
        context.startService(new Intent(context, SensorService.class));
    }
}
