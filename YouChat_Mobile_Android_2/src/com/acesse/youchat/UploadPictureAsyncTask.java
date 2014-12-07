package com.acesse.youchat;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.util.Log;


/**
 * 
 */
public class UploadPictureAsyncTask extends AsyncTask<Bitmap, Integer, Object> {

    private static final String TAG = "YOUC";
    private String accountId = null;


    public UploadPictureAsyncTask(String accountId){
        this.accountId = accountId;
    }

    @Override
    protected Object doInBackground(Bitmap... args) {

        String fileName = "pict.jpg";
        HttpURLConnection connection = null;
        try {               
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "---------------------------14737809831466499882746641449";

            URL url = new URL("http://" + Config.DOMAIN_NODEJS + ":7000/api/account/" + URLEncoder.encode(accountId, "UTF-8") + "/picture");
            if (Config.DEBUG) {
                Log.d(TAG, "UPLOADING TO: " + url);
            }
            long startTime = System.currentTimeMillis();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setDoInput(true); 
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("ENCTYPE", "multipart/form-data");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

            dos.writeBytes(lineEnd + twoHyphens + boundary + lineEnd); 
            dos.writeBytes("Content-Disposition: form-data; name=\"profilePictureUploader\"; filename=\""+ fileName + "\"" + lineEnd);
            dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
            dos.writeBytes(lineEnd);

            args[0].compress(CompressFormat.JPEG, 90, dos);

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            dos.flush();
            dos.close();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Request Failed with " + connection.getResponseCode() + ": " + connection.getResponseMessage());
            }

            if (Config.DEBUG) {
                Log.d(TAG, "SENT TIME: " + (System.currentTimeMillis()-startTime) + "ms");
            }

            InputStream is = connection.getInputStream();

            byte[] data = new byte[2048];
            int bytesRead = is.read(data);
            is.close();

            if (bytesRead == -1) {
                throw new Exception("No response data");
            }

            String json = new String(data, 0, bytesRead);
            if (Config.DEBUG) {
                Log.d(TAG, "RCV: " + json);
            }
            return new JSONObject(json);
        } catch (Exception ex) {
            Log.w(TAG, "FAILED TO UPLOAD: " + fileName, ex);
            return ex;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}