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
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import sg.edu.nyp.alexia.R;
import sg.edu.nyp.alexia.checkin.AppointmentChecker;
import sg.edu.nyp.alexia.model.Appointments;

import static android.R.string.yes;


/**
 * Created by Spencer on 5/2/2017.
 */

public class GeoCheckinService extends Service implements com.google.android.gms.location.LocationListener{

    public static final String GEOFENCE_ID = "MyGeofenceAlexia";

    //Google / Geofence Service
    GoogleApiClient mGoogleApiClient;
    ArrayList<Appointments> mAppointments;
    BackGeoReceiver mBackGeoReceiver = new BackGeoReceiver();

    public GeoCheckinService(final Context applicationContext) {
        Log.i("GeoCheckinService", "Initialized");
    }

    public GeoCheckinService() {

    }

    @Override
    public void onCreate(){
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction("sg.edu.nyp.alexia.enter");
        filter.addAction("sg.edu.nyp.alexia.exit");
        this.registerReceiver(mBackGeoReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Bundle bundle = intent.getExtras();
        mAppointments = (ArrayList<Appointments>) bundle.getSerializable("Appointment_List");

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
        Log.e("GeoCheckinService", "Unregister mBackGeoreceiver");
        unregisterReceiver(mBackGeoReceiver);
        stopGeofenceMonitoring();
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }

    // Location Monitoring Method
    public void startLocationMonitoring() {
        Log.d("GeoCheckinService", "startLocationMonitoring Called");
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(30000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

            startGeofenceMonitoring();

        } catch (SecurityException e) {
            Log.d("GeoCheckinService", "SecurityException - " + e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("GeoCheckinService", "Location update lat/long " + location.getLatitude() + " " + location.getLongitude());
    }

    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    // Start Geofencing Method, Create Geofence Area
    private void startGeofenceMonitoring() {
        Log.d("GeoCheckinService", "startGeofenceMonitoring Called");
        try {
            Geofence mGeofence = new Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(1.3787785, 103.8485165, 100)
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

    // Stop Geofence Monitoring
    private void stopGeofenceMonitoring() {
        ArrayList<String> geofenceIds = new ArrayList<String>();
        geofenceIds.add(GEOFENCE_ID);
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofenceIds);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BackGeoReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context ctx, Intent intent) {

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            final String formattedDate = df.format(c.getTime());
            Log.e("Testing Serializable", mAppointments.get(1).getRoom());
            if (TextUtils.equals(intent.getAction(), "sg.edu.nyp.alexia.enter")) {
                Log.e("BackGeoReceiver", "WOoo Hoo Backgorund enter Geofence");
                for (int i = 0; i < mAppointments.size(); i++) {
                    Log.e("OnDataChange", String.valueOf(mAppointments.get(i).getDate()));
                    if (mAppointments.get(i).getDate().equals(formattedDate) && mAppointments.get(i).getCheckin().equals("No")) {
                        Log.e("INSIDE DATASNAP", formattedDate);

                        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
                        Intent googMapIntent = new Intent("sg.edu.nyp.alexia.GoogleMap");
                        PendingIntent resultGoogMapIntent = PendingIntent.getBroadcast(ctx, (int) System.currentTimeMillis(), googMapIntent, 0);

                        Intent checkinIntent = new Intent("sg.edu.nyp.alexia.AppointmentCheckin");
                        Log.e("TESTING", "mAppointments: " + i);
                        checkinIntent.putExtra("Appointment",String.valueOf(i));
                        PendingIntent pendingCheckinIntent = PendingIntent.getBroadcast(ctx, (int) System.currentTimeMillis(), checkinIntent, 0);

                        NotificationCompat.Builder noti = new NotificationCompat.Builder(ctx);
                        noti.setContentTitle(String.valueOf(mAppointments.get(i).getType()) + " Appointment");
                        noti.setContentText("Time: " + String.valueOf(mAppointments.get(i).getTime()));
                        noti.setSmallIcon(R.drawable.notification_icon);
                        noti.addAction(new NotificationCompat.Action(0, "Get Direction", resultGoogMapIntent));
                        noti.addAction(new NotificationCompat.Action(0, "Check-in", pendingCheckinIntent));
                        noti.setPriority(NotificationCompat.PRIORITY_MAX);
                        noti.setWhen(0);
                        noti.setOngoing(true);
                        noti.setOnlyAlertOnce(true);
                        noti.setAutoCancel(false);
                        notificationManager.notify(i + 1000, noti.build());
                    }
                }
            } else if (TextUtils.equals(intent.getAction(), "sg.edu.nyp.alexia.exit")) {
                Log.e("BackGeoReceiver", "Sob sob Backgorund exit Geofence");
                for (int i = 0; i < mAppointments.size(); i++) {
                    Log.e("OnDataChange", String.valueOf(mAppointments.get(i).getDate()));
                    if (mAppointments.get(i).getDate().equals(formattedDate) && mAppointments.get(i).getCheckin().equals("No")) {
                        Log.e("INSIDE DATASNAP", formattedDate);

                        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
                        Intent googMapIntent = new Intent("sg.edu.nyp.alexia.GoogleMap");
                        PendingIntent resultGoogMapIntent = PendingIntent.getBroadcast(ctx, (int) System.currentTimeMillis(), googMapIntent, 0);

                        NotificationCompat.Builder noti = new NotificationCompat.Builder(ctx);
                        noti.setContentTitle(String.valueOf(mAppointments.get(i).getType()) + " Appointment");
                        noti.setContentText("Time: " + String.valueOf(mAppointments.get(i).getTime()));
                        noti.setSmallIcon(R.drawable.notification_icon);
                        noti.addAction(new NotificationCompat.Action(0, "Get Direction", resultGoogMapIntent));
                        noti.setPriority(NotificationCompat.PRIORITY_MAX);
                        noti.setWhen(0);
                        noti.setOngoing(true);
                        noti.setOnlyAlertOnce(true);
                        noti.setAutoCancel(false);
                        notificationManager.notify(i + 1000, noti.build());
                    }
                }
            }
        }
    }
}