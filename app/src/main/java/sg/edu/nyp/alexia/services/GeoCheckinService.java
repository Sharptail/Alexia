package sg.edu.nyp.alexia.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import sg.edu.nyp.alexia.R;


/**
 * Created by Spencer on 5/2/2017.
 */

public class GeoCheckinService extends Service {

    public static final String GEOFENCE_ID = "MyGeofenceAlexia";

    //Google / Geofence Service
    GoogleApiClient mGoogleApiClient;

    public GeoCheckinService(final Context applicationContext) {
        Log.i("GeoCheckinService", "Initialized");
    }

    public GeoCheckinService() {

    }

    @Override
    public void onCreate(){
        super.onCreate();
        BackGeoReceiver mBackGeoReceiver = new BackGeoReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("sg.edu.nyp.alexia.enter");
        filter.addAction("sg.edu.nyp.alexia.exit");
        this.registerReceiver(mBackGeoReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            Log.d("GeoCheckinService", "Connected to GoogleApiClient");
                            startLocationMonitoring();
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d("GeoCheckinService", "Suspended Connection to GoogleApiClient");
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d("GeoCheckinService", "Failed to connect to GoogleApiClient - " + result.getErrorMessage());
                        }
                    })
                    .build();
            mGoogleApiClient.connect();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Location Monitoring Method
    private void startLocationMonitoring() {
        Log.d("GeoCheckinService", "startLocationMonitoring Called");
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(100000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d("GeoCheckinService", "Location update lat/long " + location.getLatitude() + " " + location.getLongitude());
                }
            });

            startGeofenceMonitoring();

        } catch (SecurityException e) {
            Log.d("GeoCheckinService", "SecurityException - " + e.getMessage());
        }
    }

    // Start Geofencing Method, Create Geofence Area
    private void startGeofenceMonitoring() {
        Log.d("GeoCheckinService", "startGeofenceMonitoring Called");
        try {
            Geofence mGeofence = new Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(1.3787785, 103.8485165, 50)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(1000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            GeofencingRequest mGeofenceRequest = new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(mGeofence).build();

            // Call Geofence Service Class
            Intent intent = new Intent(this, GeofenceService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Check Google API
            if (!mGoogleApiClient.isConnected()) {
                Log.d("GeoCheckinService", "GoogleApiClient is not connected");
            } else {
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, mGeofenceRequest, pendingIntent)
                        .setResultCallback(new ResultCallback<Status>() {

                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.d("GeoCheckinService", "Successfully added geofence");
                                } else {
                                    Log.d("GeoCheckinService", "Failed to add geofence + " + status.getStatus());
                                }
                            }
                        });
            }
        } catch (SecurityException e) {
            Log.d("GeoCheckinService", "SecurityException - " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BackGeoReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (TextUtils.equals(intent.getAction(), "sg.edu.nyp.alexia.enter")) {
                Log.e("BackGeoReceiver", "WOoo Hoo Backgorund enter Geofence");

                NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);

                Notification noti = new Notification.Builder(ctx)
                        .setContentTitle("Medical Appointment at Alexandra Health")
                        .setContentText("Entering Geofence Area LE")
                        .setSmallIcon(R.drawable.notification_icon)
                        .build();

                notificationManager.notify(4321, noti);
            } else if (TextUtils.equals(intent.getAction(), "sg.edu.nyp.alexia.exit")) {
                Log.e("BackGeoReceiver", "Sob sob Backgorund exit Geofence");
                NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);

                Notification noti = new Notification.Builder(ctx)
                        .setContentTitle("Medical Appointment at Alexandra Health")
                        .setContentText("Exiting Geofence Area LE")
                        .setSmallIcon(R.drawable.notification_icon)
                        .build();

                notificationManager.notify(4312, noti);
            }
        }
    }
}



