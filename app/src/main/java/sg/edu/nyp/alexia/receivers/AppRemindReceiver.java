package sg.edu.nyp.alexia.receivers;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import sg.edu.nyp.alexia.R;
import sg.edu.nyp.alexia.model.Appointments;
import sg.edu.nyp.alexia.services.GeoCheckinService;
import sg.edu.nyp.alexia.model.MyNriceFile;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Spencer on 3/2/2017.
 */

public class AppRemindReceiver extends BroadcastReceiver implements Serializable{

    public List<Appointments> mAppointments = new ArrayList<Appointments>();
    private DatabaseReference patientAppointDB;
    private ValueEventListener vAppointListener;
    private DatabaseReference mDatabaseReference;
    private ChildEventListener mChildEventListener;
    // For NRIC
    public static String nricLog;
    MyNriceFile MyNricFile = new MyNriceFile();

    // For GeoCheckinService
    Intent mServiceIntent;
    private GeoCheckinService mGeoCheckinService;

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

    private Boolean postNotification;

    public void postNote(String formattedDate, Context ctx) {

        for (int i = 0; i < mAppointments.size(); i++) {
            Log.e("OnDataChange", String.valueOf(mAppointments.get(i).getDate()));
            if (mAppointments.get(i).getDate().equals(formattedDate) && mAppointments.get(i).getCheckin().equals("No")) {
                Log.e("INSIDE DATASNAP", formattedDate);

                NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);

                Intent resultIntent = new Intent("sg.edu.nyp.alexia.GoogleMap");
                PendingIntent resultPendingIntent = PendingIntent.getBroadcast(ctx,(int) System.currentTimeMillis(),resultIntent,0);

                NotificationCompat.Builder noti = new NotificationCompat.Builder(ctx);
                noti.setContentTitle(String.valueOf(mAppointments.get(i).getType()) + " Appointment");
                noti.setContentText("Time: " + String.valueOf(mAppointments.get(i).getTime()));
                noti.setSmallIcon(R.drawable.notification_icon);
                noti.addAction(new NotificationCompat.Action(0,"Get Direction", resultPendingIntent));
                noti.setPriority(NotificationCompat.PRIORITY_MAX);
                noti.setWhen(0);
                noti.setOngoing(true);
                noti.setOnlyAlertOnce(true);
                noti.setAutoCancel(false);
                notificationManager.notify(i + 1000, noti.build());

                postNotification = true;
            }
        }
        if (postNotification) {
            // Initialize GeoCheckinService
            Bundle bundle = new Bundle();
            bundle.putSerializable("Appointment_List", (Serializable) mAppointments);
            mGeoCheckinService = new GeoCheckinService(ctx);
            mServiceIntent = new Intent(ctx, mGeoCheckinService.getClass());
            mServiceIntent.putExtras(bundle);
            if (!isMyServiceRunning(mGeoCheckinService.getClass(), ctx)) {
                ctx.startService(mServiceIntent);
            }
        }
        patientAppointDB.removeEventListener(vAppointListener);
    }

    //Check if mGeoCheckinService is running
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
