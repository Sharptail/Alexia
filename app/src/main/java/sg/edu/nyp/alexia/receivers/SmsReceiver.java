package sg.edu.nyp.alexia.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import sg.edu.nyp.alexia.checkin.OTPVerification;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = SmsReceiver.class.getSimpleName();

    public SmsReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null) {
                // Retrieve SMS Message
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
                    if (senderNum.equals("+17328317176")) {
                        String code;
                        int index = message.indexOf(":");
                        if (index != -1) {
                            // Pass OTP to received SMS
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
