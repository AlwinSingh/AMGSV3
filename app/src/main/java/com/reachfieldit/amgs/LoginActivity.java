package com.reachfieldit.amgs;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.reachfieldit.amgs.util.Constants;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;

public class LoginActivity extends AppCompatActivity {
    private String LOGIN_API = "/api/mobile/login";
    Constants constants = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginBtn = (Button) findViewById(R.id.loginButton);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        constants = new Constants(LoginActivity.this);

        new SplashActivity().checkMediaFolder(LoginActivity.this);

        loginBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("[LOGINACTIVITY]", "LOGIN BUTTON PRESSED");
                progressBar.setVisibility(View.VISIBLE);
                EditText usernameInput = (EditText) findViewById(R.id.usernameInput);
                EditText passwordInput = (EditText) findViewById(R.id.passwordInput);
                EditText companyIdInput = (EditText) findViewById(R.id.companyIdInput);
                String usernameInputText = String.valueOf(usernameInput.getText());
                String passwordInputText = String.valueOf(passwordInput.getText());
                String companyIdInputText = String.valueOf(companyIdInput.getText());
                //Todo: Add validation handling
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String requestResponse = sendLoginHttpRequest(usernameInputText, passwordInputText, companyIdInputText);
                            if (requestResponse.equals("Success")) {
                                Intent intent=new Intent(LoginActivity.this,MainActivity.class);
                                startActivity(intent);
                            } else {
                                LoginActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast toast=Toast.makeText(getApplicationContext(),requestResponse,Toast.LENGTH_LONG);
                                        toast.setMargin(50,50);
                                        toast.show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
                thread.start();
            }
        });
    }

    //Method used to login through SGEMs API
    public String sendLoginHttpRequest(String username, String password, String companyId) {
        HttpsURLConnection httpClient = null;
        Log.d("[LOGINACTIVITY]", "Sending login http request");
        try {
            String userAgent = System.getProperty("http.agent");
            Log.d("############ ", BuildConfig.BASE_URL + "/" + companyId + LOGIN_API);
            httpClient = (HttpsURLConnection) new URL(BuildConfig.BASE_URL + "/" + companyId + LOGIN_API).openConnection();
            httpClient.setSSLSocketFactory(Constants.ctf.sslContext.getSocketFactory());
            httpClient.setConnectTimeout(2000);

            //add request header
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("User-Agent", userAgent);
            httpClient.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            httpClient.setRequestProperty("Content-Type", "text/plain");
            httpClient.setConnectTimeout(5000);

            //Login JSON Object
            JSONObject login_jObject = new JSONObject();
            login_jObject.put("username", username);
            login_jObject.put("password", password);

            // Send post request
            httpClient.setDoOutput(true);
            OutputStream os = httpClient.getOutputStream();
            OutputStream out = new BufferedOutputStream(os);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
            bw.write(login_jObject.toString());
            bw.close();
            out.close();
            os.close(); // This is a must.

            int responseCode = httpClient.getResponseCode();
            System.out.println("\nSending 'POST' request to URL : " + BuildConfig.BASE_URL + "/" + companyId + LOGIN_API);
            System.out.println("Response Code : " + responseCode);

            if (responseCode == 200) {
                BufferedReader responseBody = new BufferedReader(new InputStreamReader(httpClient.getInputStream()));
                String responseData = responseBody.readLine();
                Log.d("[LOGINACTIVITY]", "Login HTTP Response: " +responseData);
                String decodedJwtToken = decodeJWTToken(responseData);
                Log.d("[LOGINACTIVITY]", "Decoded JWT Token: " +decodedJwtToken);
                constants.editor.putString(Constants.userdata_prefs, decodedJwtToken);
                constants.editor.commit();
                return "Success";
            } else if (responseCode == 403) {
                return "Please login as a Site User!";
            } else if (responseCode == 400) {
                return "Failed to login! Invalid credentials!";
            } else {
                return "Failed to login!";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error encountered! Failed to login!";
        } finally {
            httpClient.disconnect();
        }
    }

    private String decodeJWTToken(String jwtToken) {
        if (jwtToken != null) {
            String[] chunks = jwtToken.split("\\.");
            Base64.Decoder decoder = Base64.getDecoder();
            String header = new String(decoder.decode(chunks[0]));
            String payload = new String(decoder.decode(chunks[1]));
            return payload;
        }
        return null;
    }
}