package sg.edu.nyp.alexia.checkin;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import sg.edu.nyp.alexia.MainActivity;
import sg.edu.nyp.alexia.R;
import sg.edu.nyp.alexia.RoutingActivity;
import sg.edu.nyp.alexia.model.Appointments;
import sg.edu.nyp.alexia.model.MyNriceFile;
import sg.edu.nyp.alexia.model.Patients;
import sg.edu.nyp.alexia.receivers.GeofenceReceiver;
import sg.edu.nyp.alexia.services.SensorService;

import static android.R.attr.checked;
import static java.text.DateFormat.getDateTimeInstance;
import static java.util.Date.parse;

public class AppointmentChecker extends AppCompatActivity implements Serializable{
    private static final String TAG = "AppointmentChecker";

    static ProgressDialog progress;

    // For retrieving geofence receiver's data
    GeofenceReceiver geofenceReceiver = new GeofenceReceiver();

    // For appointment data from firebase
    private List<String> mAppointmentIds = new ArrayList<>();
    private List<Appointments> mAppointments = new ArrayList<>();
    private int mAppointmentIndex;
    private AppointmentAdapter mAdapter;

    // Firebase Database Reference
    private DatabaseReference patientDB;
    private DatabaseReference patientAppointDB;
    private ValueEventListener vPatientListener;

    // Define Views
    private TextView mPatientName;
    private TextView mPatientHeader;
    private RecyclerView mAppointmentRecycler;
    private Context mContext;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;

    // For NRIC
    MyNriceFile MyNricFile = new MyNriceFile();

    // For SensorService
    Intent mServiceIntent;
    private SensorService mSensorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appointment);

        progress = ProgressDialog.show(this, "Loading", "Please Wait A Moment", true);

        // Initialize SensorService
        mSensorService = new SensorService(this);
        mServiceIntent = new Intent(this, mSensorService.getClass());
        if (!isMyServiceRunning(mSensorService.getClass())) {
            startService(mServiceIntent);
        }

        // For appointment check-in through notification
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    String value = extras.getString("Appointment");
                    Log.e("IntentApoint", value);
                    geoTrackNoti(value);
                }
            }
        }, 3000);
    }

    //Check if SensorService is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
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
        super.onStart();

        //If Network Is Available
        if (isNetworkAvailable()) {
            Log.d(TAG, "Network is Available");
            //Initialize Firebase Database
            patientAppointDB = FirebaseDatabase.getInstance().getReference().child("Patients").child(MyNricFile.getNric(this)).child("Appointments");
            patientDB = FirebaseDatabase.getInstance().getReference().child("Patients").child(MyNricFile.getNric(this)).child("Details");
            //Initialize Views
            mPatientName = (TextView) findViewById(R.id.patient_name);
            mPatientHeader = (TextView) findViewById(R.id.patient_header);
            mAppointmentRecycler = (RecyclerView) findViewById(R.id.recycler_appointment);
            mAppointmentRecycler.setLayoutManager(new LinearLayoutManager(this));

            //Initialize ValueEventListener for patient
            ValueEventListener patientListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Get Post object and use the values to update the UI
                    Patients patients = dataSnapshot.getValue(Patients.class);
                    mPatientHeader.setText("Patient");
                    mPatientName.setText(patients.name);
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

            // For appointment adapter
            mAdapter = new AppointmentAdapter(this, patientAppointDB);
            mAppointmentRecycler.setAdapter(mAdapter);

            // If Network Is Not Available, Alert & Prompt User To Turn On Mobile Data
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("No Connection")
                    .setMessage("Please turn on internet")
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
        super.onStop();

        // Remove Firebase Database Listener
        if (vPatientListener != null) {
            patientDB.removeEventListener(vPatientListener);
        }
        finish();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume Called");
        super.onResume();
    }

    // Check-In Method
    public void geoTrack(View view) {

        Log.e(TAG, view.getTag().toString());
        Log.d(TAG, "It is " + String.valueOf(geofenceReceiver.getInGeoGeoFence()));

        final String firebaseButtonView;

        // Retrieve related appointmentId
        firebaseButtonView = view.getTag().toString();

        // Retrieve position of appointment with relevant Id
        for (int i = 0; i < mAppointmentIds.size(); i++) {
            Log.e("mAppointmentIds", String.valueOf(mAppointmentIds.get(i)));
            if (mAppointmentIds.get(i).equals(firebaseButtonView)) {
                mAppointmentIndex = mAppointmentIds.indexOf(mAppointmentIds.get(i));
            }
        }

        // Time
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        final String formattedDate = df.format(c.getTime());

        if (mAppointments.get(mAppointmentIndex).getDate().equals(formattedDate)) {
            if (mAppointments.get(mAppointmentIndex).getCheckin().equals("No")) {
                try {
                    // Time checker
                    DateFormat appointTime = new SimpleDateFormat("hh.mm aa", Locale.ENGLISH);
                    DateFormat sysTime = new SimpleDateFormat("hh.mm", Locale.ENGLISH);
                    Calendar co = Calendar.getInstance();
                    Date at = appointTime.parse(String.valueOf(mAppointments.get(mAppointmentIndex).getTime()));
                    Date st = sysTime.parse(String.valueOf(co.get(Calendar.HOUR_OF_DAY) + "." + co.get(Calendar.MINUTE)));
                    Log.e(TAG, "This" + String.valueOf((at.getTime() - st.getTime())/60/60/60/24));
                    if ((at.getTime() - st.getTime())/60/60/60/24 < 1) {
                        if (geofenceReceiver.getInGeoGeoFence()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Appointment")
                                    .setMessage("Would you like to check in your appointment?")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Update firebase check-in field
                                            patientAppointDB.child(firebaseButtonView).child("checkin").setValue("Yes");
                                            // Broadcast to close related appointment notification
                                            Intent inbroadcast = new Intent();
                                            inbroadcast.putExtra("Notification", mAppointmentIndex + 1000);
                                            inbroadcast.setAction("sg.edu.nyp.alexia.closeNotification");
                                            sendBroadcast(inbroadcast);
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
                                    .setMessage("You can only checkin when you're at NYP!")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            //Action
                                        }
                                    })
                                    .show();
                        }
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Appointment")
                                .setMessage("You can only checkin one hour before appointed time!")
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Action
                                    }
                                })
                                .show();
                    }
                } catch (ParseException error) {
                    error.printStackTrace();
                }
            } else {
                // If checkin equals yes, calls routing to
                // allow navigation to users designated
                // room for consultation
                Intent intent = new Intent(this, RoutingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("result", mAppointments.get(mAppointmentIndex).getRoom());
                startActivity(intent);
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Appointment")
                    .setMessage("This is scheduled on " + mAppointments.get(mAppointmentIndex).getDate())
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //Action
                        }
                    })
                    .show();
        }
    }

    // Check-in method from Notification
    public void geoTrackNoti(String index) {

        // Get notification index
        final int appIndex = Integer.parseInt(index);
        final String appointIndex = mAppointmentIds.get(appIndex);

        // Time
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        final String formattedDate = df.format(c.getTime());

        if (mAppointments.get(mAppointmentIndex).getCheckin().equals("No") && mAppointments.get(mAppointmentIndex).getDate().equals(formattedDate)) {
            if (geofenceReceiver.getInGeoGeoFence()) {
                new AlertDialog.Builder(this)
                        .setTitle("Appointment")
                        .setMessage("Would you like to check in your appointment?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Update firebase check-in field
                                patientAppointDB.child(appointIndex).child("checkin").setValue("Yes");
                                // Broadcast to close related appointment notification
                                Intent inbroadcast = new Intent();
                                inbroadcast.putExtra("Notification", appIndex + 1000);
                                inbroadcast.setAction("sg.edu.nyp.alexia.closeNotification");
                                sendBroadcast(inbroadcast);
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
                        .setMessage("You can only checkin when you're at NYP!")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Action
                            }
                        })
                        .show();
            }
        }
    }

    // Pressing back with bring user to MainActivity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(AppointmentChecker.this, MainActivity.class));
        finish();
    }

//////////////////////////////////////////////////////////////////////
//@@@                       RECYCLER VIEW ~!                     @@@//
//////////////////////////////////////////////////////////////////////

    private static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        // Appointment View Holder
        private TextView field1View;
        private TextView field2View;
        private TextView field3View;
        private TextView detail1View;
        private TextView detail2View;
        private TextView detail3View;
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

    private class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder> {

        public AppointmentAdapter(final Context context, DatabaseReference ref) {
            mContext = context;
            mDatabaseReference = ref;

            ChildEventListener childEventListener = new ChildEventListener() {

                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());

                    // A new Appointment has been added, add it to the displayed list
                    Appointments appointments = dataSnapshot.getValue(Appointments.class);

                    // Update RecyclerView
                    mAppointmentIds.add(dataSnapshot.getKey());
                    mAppointments.add(appointments);
                    notifyItemInserted(mAppointments.size() - 1);

                    Log.d(TAG, "DataSnapShot:" + mAppointmentIds);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());

                    // A appointment has changed, use the key to determine if we are displaying this
                    // appointment and if so displayed the changed appointment.
                    Appointments newAppointments = dataSnapshot.getValue(Appointments.class);
                    String AppointmentsKey = dataSnapshot.getKey();

                    int appointmentsIndex = mAppointmentIds.indexOf(AppointmentsKey);
                    if (appointmentsIndex > -1) {
                        mAppointments.set(appointmentsIndex, newAppointments);
                        notifyItemChanged(appointmentsIndex);
                    } else {
                        Log.w(TAG, "onChildChanged:unknown_child:" + AppointmentsKey);
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());

                    // A appointment has changed, use the key to determine if we are displaying this
                    // appointment and if so remove it.
                    String AppointmentsKey = dataSnapshot.getKey();

                    int appointmentsIndex = mAppointmentIds.indexOf(AppointmentsKey);
                    if (appointmentsIndex > -1) {
                        // Remove data from the list
                        mAppointmentIds.remove(appointmentsIndex);
                        mAppointments.remove(appointmentsIndex);
                        notifyItemRemoved(appointmentsIndex);
                    } else {
                        Log.w(TAG, "onChildRemoved:unknown_child:" + appointmentsIndex);
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                    Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());

                    // A appointment has changed position, use the key to determine if we are
                    // displaying this appointment and if so move it.
                    Appointments movedAppointments = dataSnapshot.getValue(Appointments.class);
                    String AppointmentsKey = dataSnapshot.getKey();
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

            // Time
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            final String formattedDate = df.format(c.getTime());

            Appointments appointments = mAppointments.get(position);
            holder.checker.setTag(mAppointmentIds.get(position));

            if (appointments.checkin.equals("No")) {
                if (appointments.date.equals(formattedDate)) {
                    holder.field1View.setText("\u26A0");
                    holder.field1View.setTextColor(Color.parseColor("#B33A3A"));
                    holder.field2View.setText(appointments.date);
                    holder.field3View.setText(appointments.time);
                    holder.detail1View.setText(appointments.type);
                    holder.detail2View.setText("Appt. Date:");
                    holder.detail3View.setText("Appt. Time:");
                } else {
                    holder.field2View.setText(appointments.date);
                    holder.field3View.setText(appointments.time);
                    holder.detail1View.setText(appointments.type);
                    holder.detail2View.setText("Appt. Date:");
                    holder.detail3View.setText("Appt. Time:");
                }
            } else {
                Log.e(TAG, "tell me" + appointments.checkin);
                holder.field1View.setText("\u2713");
                holder.field1View.setTextColor(Color.parseColor("#32CD32"));
                holder.field2View.setText(appointments.doctor);
                holder.field3View.setText(appointments.room);
                holder.detail1View.setText(appointments.type);
                holder.detail2View.setText("Appt. Doctor:");
                holder.detail3View.setText("Appt. Room:");
            }
        }

        @Override
        public int getItemCount() {
            return mAppointments.size();
        }
    }

    // Direction to NYP (How to get to place through Google Map for appointment)
    public void googMap(View view) {
        double destinationLatitude = 1.379268;
        double destinationLongitude = 103.849878;
        String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%f,%f (%s)", destinationLatitude, destinationLongitude, "Block L - School of Information Technology");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps" +
                        "" +
                        "&hl=en")));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps&hl=en")));
            }
        }
    }
}