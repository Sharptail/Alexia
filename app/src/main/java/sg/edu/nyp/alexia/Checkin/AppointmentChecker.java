package sg.edu.nyp.alexia.checkin;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
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
import sg.edu.nyp.alexia.model.HelpInfo;
import sg.edu.nyp.alexia.model.MyNriceFile;
import sg.edu.nyp.alexia.model.Patients;
import sg.edu.nyp.alexia.model.Timeslot;
import sg.edu.nyp.alexia.receivers.GeofenceReceiver;
import sg.edu.nyp.alexia.services.SensorService;

public class AppointmentChecker extends AppCompatActivity implements Serializable {
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
    private DatabaseReference timeslotDB;
    private DatabaseReference patientDB;
    private DatabaseReference patientAppointDB;
    private ValueEventListener vPatientListener;
    private ChildEventListener vTimeslotListener;

    // Define Views
    private TextView mPatientName;
    private TextView mPatientHeader;
    private RecyclerView mAppointmentRecycler;
    private Context mContext;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;
    private ShowcaseView sv;

    // For NRIC
    MyNriceFile MyNricFile = new MyNriceFile();

    // For Help Info
    HelpInfo myHelpInfo = new HelpInfo();

    // For SensorService
    Intent mServiceIntent;
    private SensorService mSensorService;

    // For Appointment Booking
    private Spinner dateSpinner, timeSpinner, typeSpinner;
    private List<Timeslot> mTimeslot = new ArrayList<>();
    private List<String> mTimeslotID = new ArrayList<>();
    private List<String> mTimeslotTime = new ArrayList<>();
    private List<String> mTimeslotDate = new ArrayList<>();
    ArrayAdapter<String> dataAdapter;
    ArrayAdapter<String> timeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appointment);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

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

        // Launch help info on app's first launch
        if (myHelpInfo.getHelp(this) == null) {
            helpInfo();
        }

        Handler viewHandler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                initSwipe();
            }
        }, 3000);

        progress = ProgressDialog.show(this, "Loading", "Please Wait A Moment", true);
    }

    // Inflate app bar with appoint menu (Contains help button)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_appoint, menu);
        return true;
    }

    // Listen for menu's item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                helpInfo();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Generate help info
    private void helpInfo() {
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        lps.setMargins(0, 0, 24, 24);
        sv = new ShowcaseView.Builder(this)
                .setContentTitle("Appointment")
                .setContentText("\nTap the + sign to book appointment\n" +
                        "Swipe right on appointment to reschedule\n" +
                        "Swipe left on appointment to cancel\n\n\n" +
                        "Tap on your appointment to check-in\n" +
                        "You will see a \u2713 upon successful check-in\n" +
                        "You will see a \u2691 for today's appointment\n\n" +
                        "TAKE NOTE\nCheck-in can only be done:\n\u00B7 When you have arrived at the Hospital\n\u00B7 At most one hour before your appointment")
                .setStyle(R.style.CustomShowcaseTheme2)
                .hideOnTouchOutside()
                .replaceEndButton(R.layout.gotit_custom_button)
                .build();
        sv.setButtonPosition(lps);
        myHelpInfo.setHelp("Yes", this);
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
            // Initialize Firebase Database
            timeslotDB = FirebaseDatabase.getInstance().getReference().child("Timeslot");
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
                    mPatientHeader.setText("Hello!");
                    mPatientName.setText(patients.name);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Getting appointment failed, log a message
                    Log.w(TAG, "loadAppointment:onCancelled", databaseError.toException());
                    Toast.makeText(AppointmentChecker.this, "Failed to load appointment.",
                            Toast.LENGTH_SHORT).show();
                }
            };

            // Attach listener to database reference
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

        // Remove firebase database listener
        if (vPatientListener != null) {
            patientDB.removeEventListener(vPatientListener);
        }
        if (vTimeslotListener != null) {
            timeslotDB.removeEventListener(vTimeslotListener);
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

        // Get today's date
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        final String formattedDate = df.format(c.getTime());

        // Check if selected appointment is scheduled for today
        if (mAppointments.get(mAppointmentIndex).getDate().equals(formattedDate)) {
            // Then check if selected appointment has been checked-in
            if (mAppointments.get(mAppointmentIndex).getCheckin().equals("No")) {
                try {
                    // Get appointment time and current time for difference comparison
                    DateFormat appointTime = new SimpleDateFormat("hh.mm aa", Locale.ENGLISH);
                    DateFormat sysTime = new SimpleDateFormat("hh.mm", Locale.ENGLISH);
                    Calendar co = Calendar.getInstance();
                    Date at = appointTime.parse(String.valueOf(mAppointments.get(mAppointmentIndex).getTime()));
                    Date st = sysTime.parse(String.valueOf(co.get(Calendar.HOUR_OF_DAY) + "." + co.get(Calendar.MINUTE)));
                    Log.e(TAG, "This" + String.valueOf((at.getTime() - st.getTime())/60/60/60/24));
                    // Then check if time difference is below 1 hour
                    if ((at.getTime() - st.getTime())/60/60/60/24 < 1) {
                        // Then check if user is inside Geofence area
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
                            // Tell user to check-in when they reached
                            new AlertDialog.Builder(this)
                                    .setTitle("Appointment")
                                    .setMessage("You can only checkin when you're at the Hospital!")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            //Action
                                        }
                                    })
                                    .show();
                        }
                    } else {
                        // Tell user to check-in later
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
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == -1){
                    requestPermissions(new String[]{
                            android.Manifest.permission.CAMERA
                    }, 111);
                } else {
                    Intent intent = new Intent(this, RoutingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("result", mAppointments.get(mAppointmentIndex).getRoom());
                    startActivity(intent);
                }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 111){
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                log("CAMERA permission has now been granted. Showing preview.");

                Intent intent = new Intent(this, RoutingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("result", mAppointments.get(mAppointmentIndex).getRoom());
                startActivity(intent);
            } else {
                logUser("CAMERA permission not granted");
            }
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

    // Add appointment method
    public void addAppoint(View view) {
        // Layout inflater
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.book_dialog, null);
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AppointmentChecker.this);
        builder.setTitle("Appointment Booking");
        builder.setView(dialoglayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String sd = dateSpinner.getSelectedItem().toString();
                String sc = timeSpinner.getSelectedItem().toString();
                String st = typeSpinner.getSelectedItem().toString();
                String c = "No";
                String d = "TBC";
                String r = "TBC";

                Appointments appointment = new Appointments(sd, sc, st, c, d, r);
                DatabaseReference newRef = patientAppointDB.push();
                newRef.setValue(appointment);
                mTimeslotDate.clear();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                mTimeslotDate.clear();
            }
        });
        builder.show();

        // Date Spinner
        dateSpinner = (Spinner) dialoglayout.findViewById(R.id.dateSpinner);
        dataAdapter = new ArrayAdapter<String>(AppointmentChecker.this, android.R.layout.simple_spinner_item, mTimeslotDate);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(dataAdapter);
        dateSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListener());

        // Time Spinner
        timeSpinner = (Spinner) dialoglayout.findViewById(R.id.timeSpinner);
        timeAdapter = new ArrayAdapter<String>(AppointmentChecker.this, android.R.layout.simple_spinner_item, mTimeslotTime);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(timeAdapter);

        // Type Spinner
        typeSpinner = (Spinner) dialoglayout.findViewById(R.id.typeSpinner);

        //Initialize ChildEventListener for timeslot
        ChildEventListener timeslotListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // Get Post object and use the values to update the UI
                Timeslot timeslot = dataSnapshot.getValue(Timeslot.class);
                mTimeslot.add(timeslot);
                mTimeslotDate.add(timeslot.date);
                Log.e("Timeslot Available", "Testing" + mTimeslotDate);
                Log.e("Timeslot Date:", "Testing" + timeslot.date);
                dataAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Getting appointment failed, log a message
                Toast.makeText(AppointmentChecker.this, "Failed to load appointment.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        // Attach listener to database reference
        timeslotDB.addChildEventListener(timeslotListener);
        vTimeslotListener = timeslotListener;
    }

    // Pressing back with bring user to MainActivity
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(AppointmentChecker.this, MainActivity.class));
        finish();
    }

    public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            mTimeslotTime.clear();
            mTimeslotTime.add(mTimeslot.get(pos).slot1);
            mTimeslotTime.add(mTimeslot.get(pos).slot2);
            mTimeslotTime.add(mTimeslot.get(pos).slot3);
            Log.e("TESTING", "Time:" + mTimeslotTime);
            timeAdapter.notifyDataSetChanged();
        }
        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
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
                    holder.field1View.setText("\u2691");
                    holder.field1View.setTextColor(Color.parseColor("#B33A3A"));
                    holder.field2View.setText(appointments.date);
                    holder.field3View.setText(appointments.time);
                    holder.detail1View.setText(appointments.type);
                    holder.detail2View.setText("Date:");
                    holder.detail3View.setText("Time:");
                } else {
                    holder.field2View.setText(appointments.date);
                    holder.field3View.setText(appointments.time);
                    holder.detail1View.setText(appointments.type);
                    holder.detail2View.setText("Date:");
                    holder.detail3View.setText("Time:");
                }
            } else {
                Log.e(TAG, "tell me" + appointments.checkin);
                holder.field1View.setText("\u2713");
                holder.field1View.setTextColor(Color.parseColor("#32CD32"));
                holder.field2View.setText(appointments.doctor);
                holder.field3View.setText(appointments.room);
                holder.detail1View.setText(appointments.type);
                holder.detail2View.setText("Doctor:");
                holder.detail3View.setText("Room:");
            }
        }

        @Override
        public int getItemCount() {
            return mAppointments.size();
        }
    }

    private void initSwipe(){
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (direction == ItemTouchHelper.LEFT){
                    mAdapter.notifyItemChanged(position);
                    deleteAppointment(position);
                    Log.e("MOVE LEFT", "SWIPE LEFT");
                } else {
                    mAdapter.notifyItemChanged(position);
                    rescheduleAppointment(position);
                    Log.e("MOVE RIGHT", "SWIPE RIGHT");
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mAppointmentRecycler);
    }

    private void deleteAppointment(final int index) {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Appointment")
                .setMessage("Are you sure you would like to cancel this appointment?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        patientAppointDB.child(mAppointmentIds.get(index)).removeValue();
                    }
                })
                .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void rescheduleAppointment(final int index) {
        // Layout inflater
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.reschedule_dialog, null);
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(AppointmentChecker.this);
        builder.setTitle("Reschedule Appointment");
        builder.setView(dialoglayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String sd = dateSpinner.getSelectedItem().toString();
                String sc = timeSpinner.getSelectedItem().toString();

                patientAppointDB.child(mAppointmentIds.get(index)).child("date").setValue(sd);
                patientAppointDB.child(mAppointmentIds.get(index)).child("time").setValue(sc);
                mTimeslotDate.clear();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                mTimeslotDate.clear();
            }
        });
        builder.show();

        // Date Spinner
        dateSpinner = (Spinner) dialoglayout.findViewById(R.id.dateSpinner);
        dataAdapter = new ArrayAdapter<String>(AppointmentChecker.this, android.R.layout.simple_spinner_item, mTimeslotDate);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpinner.setAdapter(dataAdapter);
        dateSpinner.setOnItemSelectedListener(new CustomOnItemSelectedListener());

        // Time Spinner
        timeSpinner = (Spinner) dialoglayout.findViewById(R.id.timeSpinner);
        timeAdapter = new ArrayAdapter<String>(AppointmentChecker.this, android.R.layout.simple_spinner_item, mTimeslotTime);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(timeAdapter);


        //Initialize ChildEventListener for timeslot
        ChildEventListener timeslotListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // Get Post object and use the values to update the UI
                Timeslot timeslot = dataSnapshot.getValue(Timeslot.class);
                mTimeslot.add(timeslot);
                mTimeslotDate.add(timeslot.date);
                Log.e("Timeslot Available", "Testing" + mTimeslotDate);
                Log.e("Timeslot Date:", "Testing" + timeslot.date);
                dataAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Getting appointment failed, log a message
                Toast.makeText(AppointmentChecker.this, "Failed to load appointment.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        // Attach listener to database reference
        timeslotDB.addChildEventListener(timeslotListener);
        vTimeslotListener = timeslotListener;
    }

    // Jian Wei - is to Log a string and Toast at the same time for easier debugging
    private void log(String str) {
        Log.e("GH", str);
    }

    private void log(String str, Throwable t) {
        Log.e("GH", str, t);
    }

    private void logUser(String str) {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
}