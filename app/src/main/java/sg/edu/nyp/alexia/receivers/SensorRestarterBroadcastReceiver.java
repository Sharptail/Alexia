package sg.edu.nyp.alexia.receivers;

/**
 * Created by Spencer on 3/2/2017.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import sg.edu.nyp.alexia.services.GeoCheckinService;
import sg.edu.nyp.alexia.services.SensorService;

public class SensorRestarterBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(SensorRestarterBroadcastReceiver.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
        // Shutdown GeoCheckinService through broadcast to AppRemindReceiver
        Intent inbroadcast = new Intent();
        inbroadcast.setAction("sg.edu.nyp.alexia.ShutGeoCheckin");
        context.sendBroadcast(inbroadcast);
        // Restart SensorService
        context.startService(new Intent(context, SensorService.class));
    }
}
