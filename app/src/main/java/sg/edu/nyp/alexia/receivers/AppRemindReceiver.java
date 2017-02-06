package sg.edu.nyp.alexia.receivers;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import sg.edu.nyp.alexia.R;
import sg.edu.nyp.alexia.model.Appointments;
import sg.edu.nyp.alexia.services.BackgroundGeoService;
import sg.edu.nyp.alexia.services.GeoCheckinService;
import sg.edu.nyp.alexia.model.MyNriceFile;
import sg.edu.nyp.alexia.services.SensorService;

import static android.content.Context.NOTIFICATION_SERVICE;
/**
 * Created by Spencer on 3/2/2017.
 */

public class AppRemindReceiver extends BroadcastReceiver {

    public List<Appointments> mAppointments = new ArrayList<>();
    private DatabaseReference patientAppointDB;
    private ValueEventListener vAppointListener;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;
    // For NRIC
    public static String nricLog;
    MyNriceFile MyNricFile = new MyNriceFile();

    // For BackgroundGeoService
    Intent mServiceIntent;
    private BackgroundGeoService mBackgroundGeoService;

    @Override
    public void onReceive(Context context, Intent intent) {

        nricLog = MyNricFile.getNric(context);

        Log.e("APP REMIND", "RECEIVED");

        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());
        final Context ctx = context;

        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        final String formattedDate = df.format(c.getTime());
        patientAppointDB = FirebaseDatabase.getInstance().getReference().child("Patients").child(nricLog).child("Appointments");

        Log.e("FORMATTED DATE", formattedDate);

        //Initialize ValueEventListener
        ValueEventListener appointListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot appointSnap : dataSnapshot.getChildren()) {
                    Appointments appointments = appointSnap.getValue(Appointments.class);
                    mAppointments.add(appointments);
                }


                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        postNote(formattedDate, ctx);
                    }
                }, 5000);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w("AppRemindReceiver", "loadAppointment:onCancelled", databaseError.toException());
            }
        };

        patientAppointDB.addValueEventListener(appointListener);

        vAppointListener = appointListener;

    }

    public void postNote(String formattedDate, Context ctx) {

        for (int i = 0; i < mAppointments.size(); i++) {
            Log.e("OnDataChange", String.valueOf(mAppointments.get(i).getDate()));
            if (mAppointments.get(i).getDate().equals(formattedDate) && mAppointments.get(i).getCheckin().equals("No")) {
                Log.e("INSIDE DATASNAP", formattedDate);
//                NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
//
//                Notification noti = new Notification.Builder(ctx)
//                        .setContentTitle("Medical Appointment at Alexandra Health")
//                        .setContentText("Appointment Time: " + String.valueOf(mAppointments.get(i).getTime()))
//                        .setSmallIcon(R.drawable.notification_icon)
//                        .build();
//
//                notificationManager.notify(i, noti);

                // Initialize SensorService
                mBackgroundGeoService = new BackgroundGeoService(ctx);
                mServiceIntent = new Intent(ctx, mBackgroundGeoService.getClass());
                if (!isMyServiceRunning(mBackgroundGeoService.getClass(), ctx)) {
                    ctx.startService(mServiceIntent);
                }
            }
        }
        patientAppointDB.removeEventListener(vAppointListener);
    }

    //Check if mBackgroundGeoService is running
    private boolean isMyServiceRunning(Class<?> serviceClass, Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }
}
