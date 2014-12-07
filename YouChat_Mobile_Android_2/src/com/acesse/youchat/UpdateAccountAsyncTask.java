package com.acesse.youchat;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.os.AsyncTask;
import android.util.Log;


/**
 * Update account.
 */
public class UpdateAccountAsyncTask extends AsyncTask<String, Integer, Object> {

    private static final String TAG = "YOUC";


    @Override
    protected Object doInBackground(String... params) {
        HttpURLConnection conn = null;  
        try {

            long startTime = System.currentTimeMillis();

            String uparams = "";
            uparams += "firstName=" + URLEncoder.encode(params[1], "UTF-8");
            uparams += "&lastName=" + URLEncoder.encode(params[2], "UTF-8");
            uparams += "&company=" + URLEncoder.encode(params[3] == null ? "" : params[3], "UTF-8");
            uparams += "&pictureUrl=" + URLEncoder.encode(params[4] == null ? "" : params[4], "UTF-8");
            URL url = new URL("http://" + Config.DOMAIN_NODEJS + ":7000/api/account/" + URLEncoder.encode(params[0], "UTF-8"));
            if (Config.DEBUG) {
                Log.d(TAG, "UPDATE ACCOUNT");
                Log.d(TAG, "URL:    " + url);
                Log.d(TAG, "PARAMS: " + uparams);
            }
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", "" + Integer.toString(uparams.getBytes().length));
            conn.setRequestProperty("Content-Language", "en-US");  
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(uparams.getBytes());
            os.flush();
            os.close();

            if (Config.DEBUG) {
                Log.d(TAG, "SENT TIME: " + (System.currentTimeMillis()-startTime) + "ms");
            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("Request Failed with " + conn.getResponseCode() + ": " + conn.getResponseMessage());
            }

            if (params[4] != null && !params[4].isEmpty()) {
                File file = new File(MainApplication.getContactStorageDirectory(), params[4].substring(params[4].lastIndexOf("/")+1));
                if (!file.exists()) {
                    // fetch photo and store into file system & cache...
                    //ContactDAO.getInstance().fetchProfilePhoto(params[4]);
                }
            }

            return "OK";
        } catch (Exception ex) {
            Log.w(TAG, "FAILED TO UPDATE ACCOUNT", ex);
            return ex;
        } finally {
            if (conn != null) {
                conn.disconnect(); 
            }
        }
    }
}