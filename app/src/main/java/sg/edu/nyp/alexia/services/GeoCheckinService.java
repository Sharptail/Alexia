package sg.edu.nyp.alexia.services;

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
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import sg.edu.nyp.alexia.R;
import sg.edu.nyp.alexia.model.Appointments;

import static com.mapbox.mapboxsdk.constants.MapboxConstants.TAG;

public class GeoCheckinService extends Service implements com.google.android.gms.location.LocationListener{

    public static final String GEOFENCE_ID = "MyGeofenceAlexia";
    GoogleApiClient mGoogleApiClient;
    ArrayList<Appointments> mAppointments;
    BackGeoReceiver mBackGeoReceiver = new BackGeoReceiver();

    public GeoCheckinService(final Context applicationContext) {}

    public GeoCheckinService() {}

    @Override
    public void onCreate(){
        super.onCreate();

        // Register Geofence broadcaster for enter / exit
        IntentFilter filter = new IntentFilter();
        filter.addAction("sg.edu.nyp.alexia.enter");
        filter.addAction("sg.edu.nyp.alexia.exit");
        this.registerReceiver(mBackGeoReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // Retrieve appointment data from AppRemindReceiver
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
        // Force service to stick around even if system tries to shut it
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("GeoCheckinService", "Unregister mBackGeoreceiver");
        // Unregister / End all task
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

            // Call GeofenceReceiver
            Intent intent = new Intent("sg.edu.nyp.alexia.GeofenceReceiver.ACTION_RECEIVE_GEOFENCE");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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

    // Geofence notification receiver
    // for updating notification when user enter geofence
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
                        try {
                            // Time checker
                            // Show checkin one hour before appointed time
                            DateFormat appointTime = new SimpleDateFormat("hh.mm aa", Locale.ENGLISH);
                            DateFormat sysTime = new SimpleDateFormat("hh.mm", Locale.ENGLISH);
                            Calendar co = Calendar.getInstance();
                            Date at = appointTime.parse(String.valueOf(mAppointments.get(i).getTime()));
                            Date st = sysTime.parse(String.valueOf(co.get(Calendar.HOUR_OF_DAY) + "." + co.get(Calendar.MINUTE)));
                            if ((at.getTime() - st.getTime()) / 60 / 60 / 60 / 24 < 1) {
                                Log.e("INSIDE DATASNAP", formattedDate);

                                // For getting direction to hospital
                                NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
                                Intent googMapIntent = new Intent("sg.edu.nyp.alexia.GoogleMap");
                                PendingIntent resultGoogMapIntent = PendingIntent.getBroadcast(ctx, (int) System.currentTimeMillis(), googMapIntent, 0);

                                // For check-in through notification
                                // Calls a broadcast method to start activity / method
                                Intent checkinIntent = new Intent("sg.edu.nyp.alexia.AppointmentCheckin");
                                Log.e("TESTING", "mAppointments: " + i);
                                checkinIntent.putExtra("Appointment", String.valueOf(i));
                                PendingIntent pendingCheckinIntent = PendingIntent.getBroadcast(ctx, (int) System.currentTimeMillis(), checkinIntent, 0);

                                // Building notification
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
                        } catch (ParseException error) {
                            error.printStackTrace();
                        }
                    }
                }
            } else if (TextUtils.equals(intent.getAction(), "sg.edu.nyp.alexia.exit")) {
                Log.e("BackGeoReceiver", "Sob sob Backgorund exit Geofence");
                for (int i = 0; i < mAppointments.size(); i++) {
                    Log.e("OnDataChange", String.valueOf(mAppointments.get(i).getDate()));
                    if (mAppointments.get(i).getDate().equals(formattedDate) && mAppointments.get(i).getCheckin().equals("No")) {
                        Log.e("INSIDE DATASNAP", formattedDate);

                        // For getting direction to hospital
                        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
                        Intent googMapIntent = new Intent("sg.edu.nyp.alexia.GoogleMap");
                        PendingIntent resultGoogMapIntent = PendingIntent.getBroadcast(ctx, (int) System.currentTimeMillis(), googMapIntent, 0);

                        // Building notification
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