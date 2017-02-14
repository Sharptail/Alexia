package sg.edu.nyp.alexia.checkin;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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

public class NRICVerification extends AppCompatActivity {

    // Activity tagging
    private static final String TAG = "NRICVerification";

    // OTP variables
    public static String OTP;
    public static String smsPhone;
    public static String NRIC;

    // View variables
    private EditText mTo;
    private Button mSend;
    ProgressDialog progress;

    // Http post request
    private OkHttpClient mClient = new OkHttpClient();
    private Context mContext;

    // Firebase's data verification
    private DatabaseReference rootRef;

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

        mTo = (EditText) findViewById(R.id.txtNRIC);
        mSend = (Button) findViewById(R.id.btnSendSMS);
        mContext = getApplicationContext();
        rootRef = FirebaseDatabase.getInstance().getReference().child("Patients");

        // Display keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Show progress dialog
                progress = ProgressDialog.show(NRICVerification.this, "Sending SMS", "Please Wait A Moment", true);

                // Verify and retrieve related phone number
                rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        // If NRIC exists in database
                        if (snapshot.hasChild(mTo.getText().toString())) {
                            // Save NRIC
                            setNRIC(mTo.getText().toString());
                            // Retrieve phone number
                            Log.e(TAG, "This is phone number: " + String.valueOf(snapshot.child(mTo.getText().toString()).child("Details").child("phone").getValue()));
                            setSmsPhone(String.valueOf(snapshot.child(mTo.getText().toString()).child("Details").child("phone").getValue()));
                            Log.e(TAG, "This is snapshot: " + smsPhone);
                            // Start posting request to Heroku server
                            posting();
                            // Remove progress dialog
                            progress.dismiss();
                            // Start OTP verification
                            Intent intent = new Intent(NRICVerification.this, sg.edu.nyp.alexia.checkin.OTPVerification.class);
                            startActivity(intent);
                        } else {
                            // Remove progress dialog
                            progress.dismiss();
                            // Tell user invalid NRIC
                            Toast.makeText(getApplicationContext(), "Invalid NRIC!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Getting Post failed, log a message
                        Log.w(TAG, "loadAppointment:onCancelled", databaseError.toException());
                        Toast.makeText(NRICVerification.this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // NRIC field's setting
        EditText editText = (EditText) findViewById(R.id.txtNRIC);
        editText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Set soft keyboard NEXT action
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    mSend.performClick();
                    handled = true;
                }
                return handled;
            }
        });
        // Set all inputs to caps
        editText.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bla Bla Code
    }

    public void posting() {
        try {
            // Post request to heroku server
            post(mContext.getString(R.string.backend_url), new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // Async task
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "SMS Sent!", Toast.LENGTH_SHORT).show();
                            mTo.setText("");
                            finish();
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // SMS Post to Heroku Server
    Call post(String url, Callback callback) throws IOException {
        // Generate OTP
        char[] chars = "0123456789".toCharArray();
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 6; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        setOTP(sb.toString());

        // Generate post request
        RequestBody formBody = new FormBody.Builder()
                .add("From", "+17328317176")
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
