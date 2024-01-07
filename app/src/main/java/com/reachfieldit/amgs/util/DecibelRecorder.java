package com.reachfieldit.amgs.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;

public class DecibelRecorder extends AppCompatActivity {
    static final private double EMA_FILTER = 0.6;
    private static double mEMA = 0.0;
    private MediaRecorder mRecorder;
    private Context mCtx;

    public DecibelRecorder(Context mCtx) {
        this.mCtx = mCtx;
    }

    public String getDecibelReading() {
        double amplitude = mRecorder.getMaxAmplitude();
        if(amplitude > 0 && amplitude < 1000000) {
            double dbl = Math.round(convertToDb(amplitude)*10.0)/10.0;
            //Log.d(Constants.LOGGING_TAG, Double.toString(dbl) + "dB");
            return Double.toString(dbl);
        }
        return null;
    }

    public void startRecorder() {
        if (mRecorder == null) {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            String audioPath = mCtx.getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/" + "decibel_audio" + ".mp3";
            mRecorder.setOutputFile(audioPath);
            try {
                mRecorder.prepare();
            } catch (java.io.IOException ioe) {
                android.util.Log.e("DECIBELRECORDER", "IOException: " +
                        android.util.Log.getStackTraceString(ioe));

            } catch (java.lang.SecurityException e) {
                android.util.Log.e("DECIBELRECORDER", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }
            try {
                mRecorder.start();
            } catch (java.lang.SecurityException e) {
                android.util.Log.e("DECIBELRECORDER", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }

            //mEMA = 0.0;
        }
    }

    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void onResume() {
        super.onResume();
        startRecorder();
    }

    public void onPause() {
        super.onPause();
    }

    /* Below are the UTILITY FUNCTIONS */
    /*public double soundDb(double ampl) {
        return 20 * (float) Math.log10(getAmplitudeEMA() / ampl);
    }*/

    public double convertToDb(double amplitude) {
        // Cellphones can catch up to 90 db + -
        // getMaxAmplitude returns a value between 0-32767 (in most phones). that means that if the maximum db is 90, the pressure
        // at the microphone is 0.6325 Pascal.
        // it does a comparison with the previous value of getMaxAmplitude.
        // we need to divide maxAmplitude with (32767/0.6325)
        //51805.5336 or if 100db so 46676.6381
        double EMA_FILTER = 0.6;
        SharedPreferences sp = mCtx.getSharedPreferences("device-base", MODE_PRIVATE);
        double amp = (double) sp.getFloat("amplitude", 0);
        double mEMAValue = EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * mEMA;
        //Log.d(Constants.LOGGING_TAG, "db " + Double.toString(amp));
        //Assuming that the minimum reference pressure is 0.000085 Pascal (on most phones) is equal to 0 db
        // samsung S9 0.000028251
        return 20 * (float) Math.log10((mEMAValue/51805.5336)/ 0.000028251);
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