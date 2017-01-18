package sg.edu.nyp.alexia;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AppointmentChecker extends AppCompatActivity {

    public static final String GEOFENCE_ID = "MyGeofenceAlexia";
    private static final String TAG = "AppointmentChecker";

    //Google / Geofence Service
    GoogleApiClient mGoogleApiClient;
    GeofenceService geofenceService = new GeofenceService();

    //Firebase Database Reference
    private DatabaseReference patientDB;
    private DatabaseReference patientAppointDB;
    private ValueEventListener vPatientListener;
    private AppointmentAdapter mAdapter;
    //Define Views
    private TextView mPatientName;
    private TextView mPatientGender;
    private TextView mPatientBirthdate;
    private TextView mPatientAge;
    private RecyclerView mAppointmentRecycler;

    static ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appointment);

        progress = ProgressDialog.show(this, "Loading",
                "Please Wait A Moment", true);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            Log.d(TAG, "Connected to GoogleApiClient");
                            startLocationMonitoring();
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d(TAG, "Suspended Connection to GoogleApiClient");
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d(TAG, "Failed to connect to GoogleApiClient - " + result.getErrorMessage());
                        }
                    })
                    .build();
        }
    }

    //Network checker Method
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    protected void onStart() {
    Log.d(TAG, "onStart Called");
    mGoogleApiClient.reconnect();
    super.onStart();

    //If Network Is Available
    if (isNetworkAvailable()) {
        Log.d(TAG, "Network is Available");
        //Initialize Firebase Database
        patientAppointDB = FirebaseDatabase.getInstance().getReference().child("Patients").child("S9609231H").child("Appointments");
        patientDB = FirebaseDatabase.getInstance().getReference().child("Patients").child("S9609231H").child("Details");
        //Initialize Views
        mPatientName = (TextView) findViewById(R.id.patient_name);
//        mPatientGender = (TextView) findViewById(R.id.patient_gender);
//        mPatientBirthdate = (TextView) findViewById(R.id.patient_birthdate);
//        mPatientAge = (TextView) findViewById(R.id.patient_age);
        mAppointmentRecycler = (RecyclerView) findViewById(R.id.recycler_appointment);
        mAppointmentRecycler.setLayoutManager(new LinearLayoutManager(this));

        //Initialize ValueEventListener
        ValueEventListener patientListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                Patients patients = dataSnapshot.getValue(Patients.class);
                mPatientName.setText(patients.name);
//                mPatientGender.setText(patients.gender);
//                mPatientBirthdate.setText(patients.birthdate);
//                mPatientAge.setText(patients.age);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadAppointment:onCancelled", databaseError.toException());
                // [START_EXCLUDE]
                Toast.makeText(AppointmentChecker.this, "Failed to load appointment.",
                        Toast.LENGTH_SHORT).show();
                // [END_EXCLUDE]
            }
        };

        patientDB.addValueEventListener(patientListener);

        vPatientListener = patientListener;

        mAdapter = new AppointmentAdapter(this, patientAppointDB);
        mAppointmentRecycler.setAdapter(mAdapter);

        // If Network Is Not Available, Alert & Prompt User To Turn On Mobile Data
    } else {
        new AlertDialog.Builder(this)
                .setTitle("No INTERNET CONNECTION")
                .setMessage("WALEO WHAT ERA LIEO NO INTERNET???")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS), 0);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop Called");
        mGoogleApiClient.disconnect();
        super.onStop();

        // Remove Firebase Database Listener
        if (vPatientListener != null) {
            patientDB.removeEventListener(vPatientListener);
        }

        mAdapter.cleanupListener();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume Called");
        super.onResume();

        // Check Google Play Services Availability
        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (response != ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Services not available - show dialog to ask user to download it");
            GoogleApiAvailability.getInstance().getErrorDialog(this, response, 1).show();
        } else {
            Log.d(TAG, "Google Play Services is available - no action is required");
        }
    }

    // Location Monitoring Method
    private void startLocationMonitoring() {
        Log.d(TAG, "startLocationMonitoring Called");
        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(100000)
                    .setFastestInterval(5000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Location update lat/long " + location.getLatitude() + " " + location.getLongitude());
                }
            });

            startGeofenceMonitoring();

        } catch (SecurityException e) {
            Log.d(TAG, "SecurityException - " + e.getMessage());
        }
    }

    // Start Geofencing Method, Create Geofence Area
    private void startGeofenceMonitoring() {
        Log.d(TAG, "startGeofenceMonitoring Called");
        try {
            Geofence mGeofence = new Geofence.Builder()
                    .setRequestId(GEOFENCE_ID)
                    .setCircularRegion(1.3787785, 103.8485165, 200)
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
                Log.d(TAG, "GoogleApiClient is not connected");
            } else {
                LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, mGeofenceRequest, pendingIntent)
                        .setResultCallback(new ResultCallback<Status>() {

                            @Override
                            public void onResult(Status status) {
                                if (status.isSuccess()) {
                                    Log.d(TAG, "Successfully added geofence");
                                } else {
                                    Log.d(TAG, "Failed to add geofence + " + status.getStatus());
                                }
                            }
                        });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "SecurityException - " + e.getMessage());
        }
    }

    // Check-In Method
    public void geoTrack(View view) {

        Log.e(TAG, view.getTag().toString());
        Log.d(TAG, "It is " + String.valueOf(geofenceService.getInGeoGeoFence()));

        final String firebaseButtonView;
        firebaseButtonView = view.getTag().toString();

        if (geofenceService.getInGeoGeoFence() == true) {
            new AlertDialog.Builder(this)
                    .setTitle("Appointment")
                    .setMessage("Would you like to check in your appointment?")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Action
                            patientAppointDB.child(firebaseButtonView).child("checkin").setValue("Yes");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Action
                        }
                    })
                    .show();

        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Appointment")
                    .setMessage("Please Get Closer!")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Action
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        startActivity(new Intent(AppointmentChecker.this, MainActivity.class));
        finish();
    }

//////////////////////////////////////////////////////////////////////
//@@@                       RECYCLER VIEW ~!                     @@@//
//////////////////////////////////////////////////////////////////////

private static class AppointmentViewHolder extends RecyclerView.ViewHolder {

    private TextView field1View;
    private TextView field2View;
    private TextView field3View;
    private TextView detail1View;
    private TextView detail2View;
    private TextView detail3View;

    //        private TextView roomView;
//        private TextView timeView;
//        private TextView typeView;
    private CardView checker;

    public AppointmentViewHolder(View itemView) {
        super(itemView);

        field1View = (TextView) itemView.findViewById(R.id.appointment_field1);
        field2View = (TextView) itemView.findViewById(R.id.appointment_field2);
        field3View = (TextView) itemView.findViewById(R.id.appointment_field3);
        detail1View = (TextView) itemView.findViewById(R.id.appointment_detail1);
        detail2View = (TextView) itemView.findViewById(R.id.appointment_detail2);
        detail3View = (TextView) itemView.findViewById(R.id.appointment_detail3);
        checker = (CardView) itemView.findViewById(R.id.card_view);
        progress.dismiss();
    }
}

private static class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder> {

    public List<String> mAppointmentIds = new ArrayList<>();
    public List<Appointments> mAppointments = new ArrayList<>();
    private Context mContext;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    public AppointmentAdapter(final Context context, DatabaseReference ref) {
        mContext = context;
        mDatabaseReference = ref;

        ChildEventListener childEventListener = new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

                // A new Appointment has been added, add it to the displayed list
                Appointments appointments = dataSnapshot.getValue(Appointments.class);

                // [START_EXCLUDE]
                // Update RecyclerView
                mAppointmentIds.add(dataSnapshot.getKey());
                mAppointments.add(appointments);
                notifyItemInserted(mAppointments.size() - 1);
                // [END_EXCLUDE]

                Log.d(TAG, "DataSnapShot:" + mAppointmentIds);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                // A appointment has changed, use the key to determine if we are displaying this
                // appointment and if so displayed the changed appointment.
                Appointments newAppointments = dataSnapshot.getValue(Appointments.class);
                String AppointmentsKey = dataSnapshot.getKey();

                // [START_EXCLUDE]
                int appointmentsIndex = mAppointmentIds.indexOf(AppointmentsKey);
                if (appointmentsIndex > -1) {
                    // Replace with the new data
                    mAppointments.set(appointmentsIndex, newAppointments);

                    // Update the RecyclerView
                    notifyItemChanged(appointmentsIndex);
                } else {
                    Log.w(TAG, "onChildChanged:unknown_child:" + AppointmentsKey);
                }
                // [END_EXCLUDE]
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                // A appointment has changed, use the key to determine if we are displaying this
                // appointment and if so remove it.
                String AppointmentsKey = dataSnapshot.getKey();

                // [START_EXCLUDE]
                int appointmentsIndex = mAppointmentIds.indexOf(AppointmentsKey);
                if (appointmentsIndex > -1) {
                    // Remove data from the list
                    mAppointmentIds.remove(appointmentsIndex);
                    mAppointments.remove(appointmentsIndex);

                    // Update the RecyclerView
                    notifyItemRemoved(appointmentsIndex);
                } else {
                    Log.w(TAG, "onChildRemoved:unknown_child:" + appointmentsIndex);
                }
                // [END_EXCLUDE]
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                // A appointment has changed position, use the key to determine if we are
                // displaying this appointment and if so move it.
                Appointments movedAppointments = dataSnapshot.getValue(Appointments.class);
                String AppointmentsKey = dataSnapshot.getKey();

                // ...
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "postAppointments:onCancelled", databaseError.toException());
                Toast.makeText(mContext, "Failed to load Appointments.",
                        Toast.LENGTH_SHORT).show();
            }
        };

        ref.addChildEventListener(childEventListener);

        mChildEventListener = childEventListener;
    }

    @Override
    public AppointmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_appointments, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AppointmentViewHolder holder, int position) {
        Appointments appointments = mAppointments.get(position);
        holder.checker.setTag(mAppointmentIds.get(position));

        if (appointments.checkin.equals("No")) {
            Log.e(TAG, "tell me" + appointments.checkin);
            holder.field1View.setText(appointments.type);
            holder.field2View.setText(appointments.date);
            holder.field3View.setText(appointments.time);
            holder.detail1View.setText("Appt. Type:");
            holder.detail2View.setText("Appt. Date:");
            holder.detail3View.setText("Appt. Time:");
        } else {
            Log.e(TAG, "tell me" + appointments.checkin);
            holder.field1View.setText(appointments.type);
            holder.field2View.setText(appointments.doctor);
            holder.field3View.setText(appointments.room);
            holder.detail1View.setText("Appt. Type:");
            holder.detail2View.setText("Appt. Doctor:");
            holder.detail3View.setText("Appt. Room:");
            holder.checker.setClickable(false);
            holder.checker.setFocusable(false);
        }
    }

    @Override
    public int getItemCount() {
        return mAppointments.size();
    }

    public void cleanupListener() {
        if (mChildEventListener != null) {
            mDatabaseReference.removeEventListener(mChildEventListener);
        }
    }
}

}
