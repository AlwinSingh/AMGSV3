package com.reachfieldit.amgs;

import android.content.Intent;
import android.graphics.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.reachfieldit.amgs.util.DecibelRecorder;
import com.reachfieldit.amgs.util.RecursiveFileObserver;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class RecordingActivity extends AppCompatActivity {
    private DecibelRecorder dRec = null;
    private int decibelCount = 1;
    private Double decibelTotalCountOverXSeconds = 0.0;
    private Timer decibelTimer;
    public static Double lastStoredAverageDecibel = 0.0;
    private Double sensorDecibelThreshold = 30.0;
    private int AVERAGE_TIME_LAPSE_SECONDS = 10; //10 SECONDS TIMER
    private TextView decibelText, alertTextView;
    private Button exitButton;
    private CameraFragment camFragment;
    private int exitButtonCount = 1;

    //The values here are changed everytime theres a new upload!
    public String sensorId, userId, siteId, companyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sensorId = extras.getString("sensorId");
            userId = extras.getString("userId");
            siteId = extras.getString("siteId");
            companyId = extras.getString("companyId");

            Log.d("######## sensorId", " sensorId " + sensorId);
            Log.d("######## userId", " userId " + userId);
            Log.d("######## siteId", " siteId " + siteId);
            Log.d("######## companyId", " companyId " + companyId);

            if (sensorId == null || userId == null || siteId == null || companyId == null) {
                Intent i = new Intent(RecordingActivity.this, MainActivity.class);
                startActivity(i);
            }
        } else {
            Intent i = new Intent(RecordingActivity.this, MainActivity.class);
            startActivity(i);
        }

        //RECURSIVE FILE OBSERVER IS NO LONGER IN USE
        /*try {
            File dir = new File( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AMGS");
            RecursiveFileObserver rfo = new RecursiveFileObserver(dir.getAbsolutePath());
            rfo.startWatching();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        FragmentManager manager = getSupportFragmentManager();
        camFragment = (CameraFragment) manager.findFragmentById(R.id.cameraFragmentContainer);

        decibelText = findViewById(R.id.decibelText);
        alertTextView = findViewById(R.id.alertTextView);
        exitButton = findViewById(R.id.exitButton);

        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("################################", "########################## exitButtonCount " + exitButtonCount);
                exitButtonCount = exitButtonCount + 1;
                if (exitButtonCount == 5) {
                    if (decibelTimer != null) decibelTimer.cancel();
                    Intent i = new Intent(RecordingActivity.this, MainActivity.class);
                    startActivity(i);
                    finish();
                }
            }
        });

        dRec = new DecibelRecorder(RecordingActivity.this);

        try {
            dRec.startRecorder();
            decibelTimer = new Timer();
            decibelTimer.schedule(new DecibelRunner(), 0, 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class DecibelRunner extends TimerTask {
        public void run() {
            try {
                if (dRec != null) {
                    String decibelValue = dRec.getDecibelReading();
                    String sensorValue = "";

                    if (decibelValue != null && sensorValue != null) {
                        decibelTotalCountOverXSeconds = decibelTotalCountOverXSeconds + Double.valueOf(decibelValue);
                        double decibelAverageOverXSeconds = decibelTotalCountOverXSeconds / decibelCount;
                        Log.d("########## CHECK DECIBEL", "count: " + decibelCount + ", average: " + decibelAverageOverXSeconds + ", current: " + decibelValue + ", isRecording: " + camFragment.isRecording);

                        if (decibelCount == AVERAGE_TIME_LAPSE_SECONDS) {
                            //loadDecibel(); //Get the current sensor decibel value
                            if (RecordingActivity.this != null && decibelAverageOverXSeconds > sensorDecibelThreshold /*&& !((CameraActivity) getActivity()).isRecording*/) {
                                // Save the average now for upload
                                lastStoredAverageDecibel = decibelAverageOverXSeconds;
                                Log.d("################# DECIBEL DECIBEL", "Decibel threshold exceeded, start recording");
                                Log.d("############### DECIBEL DECIBEL", "avg: " + decibelAverageOverXSeconds + " > threshold: " + sensorDecibelThreshold);
                                startRecordLogic();
                            }
                            decibelCount = 1;
                            decibelTotalCountOverXSeconds = 0.0;
                        } else {
                            decibelCount++;
                        }

                        // here you check the value of getActivity() and break up if needed
                        if(RecordingActivity.this == null)
                            return;

                        RecordingActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Log.d("####### DECIBEL DECIBEL",decibelValue + " dB");
                                decibelText.setText(decibelValue + " dB");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //This logic simply takes a 5 Second Video
    private void startRecordLogic() {
        RecordingActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                alertTextView.setText("Recording In Progress. Lower your volume.");
                //if (!camFragment.isRecording) {
                    camFragment.toggleRecording(RecordingActivity.this,lastStoredAverageDecibel, sensorId, userId, siteId, companyId);
                //}
            }
        });
    }
}
