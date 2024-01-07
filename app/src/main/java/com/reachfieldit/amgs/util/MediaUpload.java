package com.reachfieldit.amgs.util;

import android.util.Log;

import com.reachfieldit.amgs.BuildConfig;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MediaUpload {
    public void videoUploadDetails(Double decibelValue, String sensorId, String userId, String siteId, String companyId, String videoFilePath, String imageEvidence64) {
        Map<String, String> params = new HashMap<String, String>(5);
        long uniqueId = System.currentTimeMillis() / 1000L;
        params.put("unique_id", String.valueOf(uniqueId));
        params.put("site_id", siteId);
        params.put("sensor_id", sensorId);
        params.put("user_id", userId);
        params.put("decibel_value", String.format("%.2f", decibelValue));
        params.put("image_evidence", imageEvidence64);
        String serverResult = uploadToServer(params, videoFilePath, "video_evidence", "video/mp4", companyId);
        Log.d("##### VIDEO UPLOAD DETAILS: ","Upload binary response: " + serverResult);
    }

    public String uploadToServer(Map<String, String> params, String filepath, String filefield, String fileMimeType, String companyId) {
        HttpsURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";

        String result = "";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        String[] q = filepath.split("/");
        int idx = q.length - 1;

        try {
            File file = new File(filepath);
            FileInputStream fileInputStream = new FileInputStream(file);

            //URL url = new URL(BuildConfig.BASE_URL + "/" + companyId + "uploadAmgsEvidence");
            URL url = new URL("https://192.168.10.104/sgems2/api/recording/uploadAmgsEvidence");
            connection = (HttpsURLConnection) url.openConnection();

            //connection.setConnectTimeout(3000); //1 Second Timeout to Connect to Server
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            connection.setSSLSocketFactory(Constants.ctf.sslContext.getSocketFactory());

            Log.d("MEDIAUPLOAD#############","UPLOAD SERVER API: " + BuildConfig.BASE_URL + "/" + companyId + "/api/recording/uploadAmgsEvidence");
            Log.d("MEDIAUPLOAD#############","Connected to UPLOAD SERVER");

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            // Upload POST Data
            Iterator<String> keys = params.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = params.get(key);

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(value);
                outputStream.writeBytes(lineEnd);
            }

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            Log.d("MEDIAUPLOAD #############","SGEMS Response Code: " + connection.getResponseCode());
            if (200 != connection.getResponseCode()) {
                Log.d("MEDIAUPLOAD #############","Failed to upload video");
                throw new Exception("Failed to upload code:" + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            inputStream = connection.getInputStream();

            result = this.convertStreamToString(inputStream);

            fileInputStream.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
