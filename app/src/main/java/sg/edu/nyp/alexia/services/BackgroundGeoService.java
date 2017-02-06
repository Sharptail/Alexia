package sg.edu.nyp.alexia.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import sg.edu.nyp.alexia.R;


/**
 * Created by Spencer on 6/2/2017.
 */

public class BackgroundGeoService extends Service {

    // For BackgroundGeoService
    Intent mServiceIntent;
    private GeoCheckinService mGeoCheckinService;

    public BackgroundGeoService(Context ctx) {
        super();
        Log.e("BACKGROUND GEO SERVICE", "Initialized");
        // Initialize SensorService
        mGeoCheckinService = new GeoCheckinService(ctx);
        mServiceIntent = new Intent(ctx, mGeoCheckinService.getClass());
        if (!isMyServiceRunning(mGeoCheckinService.getClass(), ctx)) {
            ctx.startService(mServiceIntent);
        }
    }

    public void onCreate() {
        super.onCreate();
        BackGeoReceiver mBackGeoReceiver = new BackGeoReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("sg.edu.nyp.alexia.enter");
        filter.addAction("sg.edu.nyp.alexia.exit");
        this.registerReceiver(mBackGeoReceiver, filter);
    }

    public BackgroundGeoService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    //Check if SensorService is running
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
