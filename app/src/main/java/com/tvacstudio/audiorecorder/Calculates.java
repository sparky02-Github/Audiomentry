package com.tvacstudio.audiorecorder;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
/*
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;*/

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Calculates extends AppCompatActivity {

    static final private double EMA_FILTER = 0.6;
    private static double mEMA = 0.0;
    final Handler mHandler = new Handler();
    TextView mStatusView, mStatusAvgView, mStatus;
    MediaRecorder mRecorder;
    private static String LOG="Calculates";
    final Runnable updater = new Runnable() {

        public void run() {
            Log.d(LOG,"Run()");
            updateTv();
        }

        ;
    };
    Thread runner;
    private List<Double> valuesAvg = new ArrayList<>();
    private long timestamp = System.currentTimeMillis() / 1000L;
    private long lastTimestamp = System.currentTimeMillis() / 1000L;
    private String schedule = "NA";

    Date c = Calendar.getInstance().getTime();
    SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
    SimpleDateFormat time = new SimpleDateFormat("HH:mm:");

    String formattedDate = df.format(c);
    private Object BuildDev;
    public static final int RECORD_AUDIO = 0;

    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    */
    /*FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void addTodb(Map<String, Object> data, String name ){
        db.collection("database").document(name)
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("tag", "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("tag", "Error writing document", e);
                    }
                });
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.calculates);
        mStatusView = (TextView) findViewById(R.id.dbText);
        mStatusAvgView = (TextView) findViewById(R.id.dbAvgText);
        mStatus = (TextView) findViewById(R.id.dbStatus);
        Log.i(LOG, "onCreate()");


        if (runner == null) {
            runner = new Thread() {
                public void run() {
                    while (runner != null) {
                        try {
                            Thread.sleep(1000);
                            Log.i(LOG, "Tock");
                        } catch (InterruptedException e) {
                        }
                        ;
                        mHandler.post(updater);
                    }
                }
            };
            runner.start();
            Log.d(LOG ,"start runner()");
        }
    }

    public void onResume() {
        super.onResume();
        startRecorder();
    }

    public void onPause() {
        super.onPause();
        //stopRecorder();
    }

    public void startRecorder() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.RECORD_AUDIO },
                    10);
        } else {
            if (mRecorder == null) {
                Log.d(LOG,"insideStartRec()");
                mRecorder = new MediaRecorder();

                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null");
                try {
                    mRecorder.prepare();
                } catch (IOException ioe) {
                    Log.e("[Monkey]", "IOException: " +
                            Log.getStackTraceString(ioe));

                } catch (SecurityException e) {
                    Log.e("[Monkey]", "SecurityException: " +
                            Log.getStackTraceString(e));
                }
                try {
                    mRecorder.start();
                } catch (SecurityException e) {
                    Log.e("[Monkey]", "SecurityException: " +
                            Log.getStackTraceString(e));
                }

                //mEMA = 0.0;
            }
        }

    }

    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void updateTv() {
        Log.d(LOG,"inside UPDATETV()");
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (hourOfDay == 19){
            schedule = "night";
        }
        else if(hourOfDay == 8){
            schedule = "day";

        }else{
            schedule = "NA";
        }
        mStatus.setText(schedule);

        // mStatusView.setText(Double.toString((getAmplitudeEMA())) + " dB");
        double amplitude = mRecorder.getMaxAmplitude();
        if(amplitude > 0 && amplitude < 1000000) {
            Log.d(LOG,"calculating()");
            double dbl = convertdDb(amplitude);
            mStatusView.setText(Double.toString(dbl)+ "dB");

            valuesAvg.add(dbl);
            lastTimestamp = System.currentTimeMillis() / 1000L;

            if(lastTimestamp - timestamp > 60 ){
                double sum = 0;
                int count = 0;

                for(Double value : valuesAvg) {
                    count++;
                    sum+= value;
                }
                valuesAvg = new ArrayList<>();
                timestamp = lastTimestamp;
                float average = (float) sum/count;

                mStatusAvgView.setText(String.format("%.2f", average)+ "dB");

                Date currentTime = Calendar.getInstance().getTime();

                if(schedule != "NA"){
                    Map<String, Object> avg = new HashMap<>();
                    double ans = Double.parseDouble(new DecimalFormat("##.##").format(average));

                    avg.put("value", ans);
                    avg.put("date", formattedDate);
                    avg.put("time", time.format(Calendar.getInstance().getTime()));
                    avg.put("schedule", schedule);

                    //addTodb(avg, String.valueOf(lastTimestamp));
                }

            }
        }
    }

    public double soundDb(double ampl) {
        return 20 * (float) Math.log10(getAmplitudeEMA() / ampl);
    }
    public double convertdDb(double amplitude) {
        // Cellphones can catch up to 90 db + -
        // getMaxAmplitude returns a value between 0-32767 (in most phones). that means that if the maximum db is 90, the pressure
        // at the microphone is 0.6325 Pascal.
        // it does a comparison with the previous value of getMaxAmplitude.
        // we need to divide maxAmplitude with (32767/0.6325)
        //51805.5336 or if 100db so 46676.6381
        double EMA_FILTER = 0.6;
        SharedPreferences sp = this.getSharedPreferences("device-base", MODE_PRIVATE);
        double amp = (double) sp.getFloat("amplitude", 0);
        double mEMAValue = EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * mEMA;
        Log.d(LOG, Double.toString(amp));
        //Assuming that the minimum reference pressure is 0.000085 Pascal (on most phones) is equal to 0 db
        // samsung S9 0.000028251
        return 20 * (float) Math.log10((mEMAValue/51805.5336) /0.000028251);
    }


    public double getAmplitude() {
        if (mRecorder != null)
            return (mRecorder.getMaxAmplitude());
        else
            return 0;

    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

}


