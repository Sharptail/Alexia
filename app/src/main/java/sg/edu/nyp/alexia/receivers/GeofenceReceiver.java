package sg.edu.nyp.alexia.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by Spencer on 12/2/2017.
 */

public class GeofenceReceiver extends BroadcastReceiver{
    public static final String TAG = "GeofenceReceiver";

    // In Geofence Checker Variable
    private static boolean inGeoGeoFence = false;

    public GeofenceReceiver() {
        super();
    }

    // In Geofence Checker Getter
    public boolean getInGeoGeoFence() {
        return inGeoGeoFence;
    }

    // In Geofence Checker Setter
    public void setInGeoGeoFence(boolean vGeo) {
        this.inGeoGeoFence = vGeo;
    }

    public void onReceive(Context ctx, Intent intent) {

        Log.e(TAG, "Received!");
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event.hasError()) {
            Log.e("GEOFENCE SERVICE", "ERROR" + event.hasError());
        } else {
            int transition = event.getGeofenceTransition();
            List<Geofence> geofences = event.getTriggeringGeofences();
            Geofence geofence = geofences.get(0);
            String requestId = geofence.getRequestId();
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.d("GEOFENCE RECEIVER EVENT", "Received Enter");
                Log.d(TAG, "Entering geofence - " + requestId);
                setInGeoGeoFence(true);
                Log.d(TAG, "Tell me " + String.valueOf(inGeoGeoFence));
                // Broadcast to BackGeoReceiver in GeocheckinService
                Intent inbroadcast = new Intent();
                inbroadcast.setAction("sg.edu.nyp.alexia.enter");
                ctx.sendBroadcast(inbroadcast);
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.d("GEOFENCE RECEIVER EVENT", "Received EXIT");
                Log.d(TAG, "Exiting geofence - " + requestId);
                setInGeoGeoFence(false);
                Log.d(TAG, "Tell me " + String.valueOf(inGeoGeoFence));
                // Broadcast to BackGeoReceiver in GeocheckinService
                Intent exitbroadcast = new Intent();
                exitbroadcast.setAction("sg.edu.nyp.alexia.exit");
                ctx.sendBroadcast(exitbroadcast);
            }
        }
    }
}
