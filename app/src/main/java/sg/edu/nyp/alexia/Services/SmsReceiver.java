package sg.edu.nyp.alexia.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import sg.edu.nyp.alexia.Checkin.OTPVerification;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = SmsReceiver.class.getSimpleName();

    public SmsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null) {
                SmsMessage smsMessage;

                if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                    SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    smsMessage = msgs[0];
                } else {
                    Object pdus[] = (Object[]) bundle.get("pdus");
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdus[0]);
                }
                String senderNum = smsMessage.getDisplayOriginatingAddress();
                String message = smsMessage.getMessageBody();
                Log.e(TAG, message);
                Log.e(TAG, senderNum);
                try {
                    if (senderNum.equals("+61448541925")) {
                        String code = null;
                        int index = message.indexOf(":");
                        if (index != -1) {
                            int start = index + 2;
                            int length = 6;
                            code = message.substring(start, start + length);
                            OTPVerification Sms = new OTPVerification();
                            Sms.receivedSms(code);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
