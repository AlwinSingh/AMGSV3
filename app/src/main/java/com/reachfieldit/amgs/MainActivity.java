package com.reachfieldit.amgs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.reachfieldit.amgs.util.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private int userId, siteId;
    private String fullName, companyId, siteImageName, siteName;
    private ImageView siteImageView;
    private TextView siteTextView, sensorLocationTv, sensorDescriptionTv, sensorThresholdTv, sensorTypeTv;
    private Button startBtn, logoutBtn, saveBtn;
    private Spinner sensorSpinner;
    private SeekBar dBSeekBar;
    private LinearLayout thresholdLayout, spinnerLayout;
    private JSONArray sensorList = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        siteImageView = (ImageView) findViewById(R.id.siteImage);
        siteTextView = (TextView) findViewById(R.id.siteTextView);
        sensorLocationTv = (TextView) findViewById(R.id.sensorLocationTv);
        sensorDescriptionTv = (TextView) findViewById(R.id.sensorDescriptionTv);
        sensorThresholdTv = (TextView) findViewById(R.id.sensorThresholdTv);
        sensorTypeTv = (TextView) findViewById(R.id.sensorTypeTv);
        startBtn = (Button) findViewById(R.id.startBtn);
        logoutBtn = (Button) findViewById(R.id.logoutButton);
        sensorSpinner = (Spinner) findViewById(R.id.sensorSpinner);
        dBSeekBar = (SeekBar) findViewById(R.id.dbSeekBar);
        thresholdLayout = (LinearLayout) findViewById(R.id.thresholdLayout);
        spinnerLayout = (LinearLayout) findViewById(R.id.spinnerLinearLayout);
        //saveBtn = (Button) findViewById(R.id.saveBtn);
        getUserData();
        initSpinnerData();

        startBtn = (Button) findViewById(R.id.startBtn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent(MainActivity.this, RecordingActivity.class);
                    i.putExtra("companyId", String.valueOf(companyId));
                    i.putExtra("sensorId", String.valueOf(sensorList.getJSONObject(sensorSpinner.getSelectedItemPosition()).getInt("sensorID")));
                    i.putExtra("userId", String.valueOf(userId));
                    i.putExtra("siteId", String.valueOf(siteId));
                    startActivity(i);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Constants constants = new Constants(MainActivity.this);
                constants.editor.remove(Constants.userdata_prefs);
                constants.editor.commit();
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        dBSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { //link "seekbar" with seek bar change listener
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                sensorThresholdTv.setText("Sensor threshold: " + progress + " dB"); //update "progress" value and pass it to textview
                dBSeekBar.setProgress(progress); // also update "progress" value and pass it to progress bar
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    int spinnerPosition = sensorSpinner.getSelectedItemPosition();
                    int sensorId = sensorList.getJSONObject(spinnerPosition).getInt("sensorID");
                    double oldThreshold = sensorList.getJSONObject(spinnerPosition).getDouble("sensorValue");
                    double newThreshold = seekBar.getProgress();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setEnabled(false);
                            seekBar.setAlpha(0.5f);
                        }
                    });

                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                boolean isUpdateSuccess = updateSensor(sensorId,newThreshold);
                                if (isUpdateSuccess) {
                                    sensorList.getJSONObject(spinnerPosition).remove("sensorValue");
                                    sensorList.getJSONObject(spinnerPosition).put("sensorValue", newThreshold);
                                } else {
                                    seekBar.setProgress((int) oldThreshold);
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast toast=Toast.makeText(getApplicationContext(),"Failed to update decibel threshold!",Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        seekBar.setEnabled(true);
                                        seekBar.setAlpha(1.0f);
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initSpinnerData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                sensorList = getAllSensors();
                Log.d("[MAINACTIVITY]", "############## " +sensorList.length());

                // Stuff that updates the UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (sensorList.length() > 0) {
                            ArrayList<String> options = new ArrayList<String>();
                            for (int i = 0; i < sensorList.length(); i++) {
                                try {
                                    options.add(sensorList.getJSONObject(i).getString("sensorDesc"));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_item, options);
                            sensorSpinner.setAdapter(adapter);
                            startBtn.setEnabled(true);
                            startBtn.setText("Start Recording");
                            startBtn.setBackgroundColor(Color.parseColor("#368bde"));
                            startBtn.setAlpha(1.0F);
                            spinnerLayout.setVisibility(View.VISIBLE);
                        } else {
                            startBtn.setEnabled(false);
                            startBtn.setText("No sensors found");
                            startBtn.setBackgroundColor(Color.parseColor("#c3c3c3"));
                            startBtn.setAlpha(0.5F);
                            spinnerLayout.setVisibility(View.INVISIBLE);
                        }
                    }
                });

            }
        }).start();

        sensorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                try {
                    sensorLocationTv.setText("Sensor location: " + sensorList.getJSONObject(position).getJSONObject("slBean").getString("locationAddr"));
                    sensorDescriptionTv.setText("Sensor description: " + sensorList.getJSONObject(position).getString("sensorDesc"));
                    sensorThresholdTv.setText("Sensor threshold: " + sensorList.getJSONObject(position).getDouble("sensorValue") + " dB");
                    sensorTypeTv.setText("Sensor type: " + sensorList.getJSONObject(position).getString("sensorType"));
                    thresholdLayout.setVisibility(View.VISIBLE);
                    dBSeekBar.setProgress((int) sensorList.getJSONObject(position).getDouble("sensorValue"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }

        });
    }

    private void getUserData() {
        try {
            String jwtTokenStr = Constants.mPrefs.getString(Constants.userdata_prefs, null);
            JSONObject jwtToken = new JSONObject(jwtTokenStr);
            userId = jwtToken.getInt("userID");
            siteId = jwtToken.getInt(("siteID"));
            fullName = jwtToken.getString(("fullName"));
            siteImageName = jwtToken.getString(("siteImage"));
            siteName = jwtToken.getString(("siteName"));
            companyId = jwtToken.getString(("appName"));
            Glide.with(MainActivity.this)
                    .load(BuildConfig.BASE_URL + "/" + companyId + "/siteImage/" + siteImageName)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(siteImageView);
            siteTextView.setText(siteName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONArray getAllSensors() {
        String load_sensor_url = BuildConfig.BASE_URL + "/" + companyId + "/api/sensor/retrieveSensors?siteID=" + siteId;
        HttpsURLConnection con = null;

        try {
            URL obj = new URL(load_sensor_url);
            Log.d("[MAINACTIVITY]", "Sending GET request " + load_sensor_url);
            con = (HttpsURLConnection) obj.openConnection();
            con.setSSLSocketFactory(Constants.ctf.sslContext.getSocketFactory());

            String userAgent = System.getProperty("http.agent");

            con.setConnectTimeout(5000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", userAgent);

            int responseCode = con.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                if (response.toString() != null) {
                    JSONObject sensor_jObject = new JSONObject(response.toString());
                    JSONArray sensor_jArray = sensor_jObject.getJSONArray("data");

                    if (sensor_jArray.length() > 0) {
                        return sensor_jArray;
                    } else {
                        Toast toast=Toast.makeText(getApplicationContext(),"No sound sensors found for this site",Toast.LENGTH_LONG);
                        toast.setMargin(50,50);
                        toast.show();
                        return new JSONArray();
                    }
                }
            } else {
                Log.d("[MAINACTIVITY]", "Failed to retrieve Sensors List from AMGS");
                return new JSONArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONArray();
        } finally {
            con.disconnect();
        }
        return new JSONArray();
    }

    private boolean updateSensor(int sensorId, double newDecibelThreshold) {
        String update_sensor_url = BuildConfig.BASE_URL + "/" + companyId + "/api/sensor/updateSensor/" + userId + "/" + sensorId + "/" + newDecibelThreshold;
        HttpsURLConnection httpClient = null;

        try {
            String userAgent = System.getProperty("http.agent");

            httpClient = (HttpsURLConnection) new URL(update_sensor_url).openConnection();
            httpClient.setSSLSocketFactory(Constants.ctf.sslContext.getSocketFactory());

            //add request header
            httpClient.setConnectTimeout(2000);
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("User-Agent", userAgent);
            httpClient.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            httpClient.setRequestProperty("Content-Type", "text/plain");

            // Send post request
            httpClient.setDoOutput(true);

            int responseCode = httpClient.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + update_sensor_url);
            System.out.println("Response Code : " + responseCode);

            if (responseCode == 200) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            httpClient.disconnect();
        }
    }
}