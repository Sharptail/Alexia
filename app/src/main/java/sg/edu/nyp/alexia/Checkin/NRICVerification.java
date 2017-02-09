package sg.edu.nyp.alexia.checkin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.security.SecureRandom;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sg.edu.nyp.alexia.R;
import sg.edu.nyp.alexia.receivers.SmsReceiver;

public class NRICVerification extends AppCompatActivity {

    public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "NRICVerification";
    public static String OTP;
    public static String smsPhone;
    public static String NRIC;
    static ProgressDialog progress;
    SmsReceiver smsReceiver;
    private EditText mTo;
    private Button mSend;
    private OkHttpClient mClient = new OkHttpClient();
    private Context mContext;
    private DatabaseReference rootRef;
    IntentFilter filter;

    public static String getNRIC() {
        return NRIC;
    }

    public static void setNRIC(String NRIC) {
        NRICVerification.NRIC = NRIC;
    }

    public String getSmsPhone() {
        return smsPhone;
    }

    public void setSmsPhone(String smsPhone) {
        this.smsPhone = smsPhone;
    }

    public String getOTP() {
        return OTP;
    }

    public void setOTP(String OTP) {
        this.OTP = OTP;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nric_verify);

        filter = new IntentFilter(SMS_RECEIVED_ACTION);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        rootRef = FirebaseDatabase.getInstance().getReference().child("Patients");

        mTo = (EditText) findViewById(R.id.txtNRIC);
        mSend = (Button) findViewById(R.id.btnSendSMS);
        mContext = getApplicationContext();

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progress = ProgressDialog.show(NRICVerification.this, "Sending SMS", "Please Wait A Moment", true);

                rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.hasChild(mTo.getText().toString())) {
                            setNRIC(mTo.getText().toString());
                            // run some code
                            rootRef.child(mTo.getText().toString()).child("Details").child("phone").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    Log.e(TAG, "This is phone number: " + snapshot.getValue());
                                    setSmsPhone(snapshot.getValue().toString());
                                    Log.e(TAG, "This is snapshot: " + smsPhone);
                                    posting();
                                    progress.dismiss();
                                    Intent intent = new Intent(NRICVerification.this, sg.edu.nyp.alexia.checkin.OTPVerification.class);
                                    startActivity(intent);
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                        } else {
                            progress.dismiss();
                            Toast.makeText(getApplicationContext(), "Invalid NRIC!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Getting Post failed, log a message
                        Log.w(TAG, "loadAppointment:onCancelled", databaseError.toException());
                        // [START_EXCLUDE]
                        Toast.makeText(NRICVerification.this, "Failed to send SMS",
                                Toast.LENGTH_SHORT).show();
                        // [END_EXCLUDE]
                    }
                });
            }
        });

        EditText editText = (EditText) findViewById(R.id.txtNRIC);
        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    mSend.performClick();
                    handled = true;
                }
                return handled;
            }
        });
        editText.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
    }


    public void posting() {
        try {
            post(mContext.getString(R.string.backend_url), new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "SMS Sent!", Toast.LENGTH_SHORT).show();
                            mTo.setText("");
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bla Bla Code
    }

    // SMS Post to Heroku Server
    Call post(String url, Callback callback) throws IOException {
        char[] chars = "0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 6; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }

        setOTP(sb.toString());

        RequestBody formBody = new FormBody.Builder()
                .add("From", "+61448541925")
                .add("To", smsPhone)
                .add("Body", "This is your verification password: " + OTP)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        Call response = mClient.newCall(request);
        response.enqueue(callback);
        return response;
    }
}
