package sg.edu.nyp.alexia.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by Spencer on 12/12/2016.
 */

public class GeofenceService extends IntentService {

    public static final String TAG = "GeofenceService";
    // In Geofence Checker Variable
    private static boolean inGeoGeoFence = false;

    public GeofenceService() {
        super(TAG);
    }

    // In Geofence Checker Getter
    public boolean getInGeoGeoFence() {
        return inGeoGeoFence;
    }

    // In Geofence Checker Setter
    public void setInGeoGeoFence(boolean vGeo) {
        this.inGeoGeoFence = vGeo;
    }

    @Override
    public void onHandleIntent(Intent intent) {

        //Geofencing Event
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event.hasError()) {

        } else {

            // Get Geofence Transition
            int transition = event.getGeofenceTransition();
            List<Geofence> geofences = event.getTriggeringGeofences();
            Geofence geofence = geofences.get(0);
            String requestId = geofence.getRequestId();

            // If Geofence Transition = Enter, Set Geofence Checker To True. Else Geofence Checker is False
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.d(TAG, "Entering geofence - " + requestId);
                setInGeoGeoFence(true);
                Log.d(TAG, "Tell me " + String.valueOf(inGeoGeoFence));
            } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.d(TAG, "Exiting geofence - " + requestId);
                setInGeoGeoFence(false);
                Log.d(TAG, "Tell me " + String.valueOf(inGeoGeoFence));
            }
        }
    }
}
