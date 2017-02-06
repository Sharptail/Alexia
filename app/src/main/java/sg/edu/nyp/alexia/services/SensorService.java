package sg.edu.nyp.alexia.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by Spencer on 4/2/2017.
 */

public class SensorService extends Service {

    public static AlarmManager am;
    public static Intent pintent;
    public static PendingIntent sender;

    public SensorService(Context applicationContext) {
        super();
        Log.i("SensorService", "Initialized");
    }

    public SensorService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startAlarm();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
        Intent broadcastIntent = new Intent("sg.edu.nyp.alexia.RestartSensor");
        sendBroadcast(broadcastIntent);
        am.cancel(sender);
    }

    public void startAlarm() {

        pintent = new Intent("sg.edu.nyp.alexia.AppRemindReceiver");
        sender = PendingIntent.getBroadcast(this, 100, pintent, PendingIntent.FLAG_UPDATE_CURRENT);

        am = (AlarmManager) getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 37);
        calendar.set(Calendar.SECOND, 30);

        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 1000 * 60 * 15, sender);
        Log.e("ALARM MANAGER", "ALARM STARTED");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}