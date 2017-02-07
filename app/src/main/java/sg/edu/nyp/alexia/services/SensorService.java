package sg.edu.nyp.alexia.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;

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
        GoogleMapReceiver mGoogleMapReceiver = new GoogleMapReceiver();
        this.registerReceiver(mGoogleMapReceiver, new IntentFilter("sg.edu.nyp.alexia.GoogleMap"));
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

//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.HOUR_OF_DAY, 9);
//        calendar.set(Calendar.MINUTE, 32);
//        calendar.set(Calendar.SECOND, 30);




        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, sender);
        Log.e("ALARM MANAGER", "ALARM STARTED");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class GoogleMapReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            double destinationLatitude = 1.379268;
            double destinationLongitude = 103.849878;
            String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%f,%f (%s)", destinationLatitude, destinationLongitude, "Block L - School of Information Technology");
            Intent pintent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            pintent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
            pintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                ctx.startActivity(pintent);
                collapsePanel(ctx);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(ctx, "Please install a maps application", Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps" + "" + "&hl=en")));
                    collapsePanel(ctx);

                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps&hl=en")));
                    collapsePanel(ctx);
                }
            }
        }

        private void collapsePanel(Context sctx) {
            try {
                Object sbservice = sctx.getSystemService("statusbar");
                Class<?> statusbarManager;
                statusbarManager = Class.forName("android.app.StatusBarManager");
                Method showsb;
                if (Build.VERSION.SDK_INT >= 17) {
                    showsb = statusbarManager.getMethod("collapsePanels");
                } else {
                    showsb = statusbarManager.getMethod("collapse");
                }
                showsb.invoke(sbservice);
            } catch (ClassNotFoundException _e) {
                _e.printStackTrace();
            } catch (NoSuchMethodException _e) {
                _e.printStackTrace();
            } catch (IllegalArgumentException _e) {
                _e.printStackTrace();
            } catch (IllegalAccessException _e) {
                _e.printStackTrace();
            } catch (InvocationTargetException _e) {
                _e.printStackTrace();
            }
        }
    }
}