package com.acesse.youchat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

import com.crittercism.app.Crittercism;

import android.os.AsyncTask;
import android.util.Log;


/**
 * 
 */
public class CreateTempPasswordAsyncTask extends AsyncTask<String, Integer, Object> {

    private static final String TAG = "YOUC";


    @Override
    protected Object doInBackground(String... params) {
        HttpURLConnection connection = null;  
        try {
            String uparams = "";
            URL url = new URL("http://" + Config.DOMAIN_NODEJS + ":7000/api/account/" + URLEncoder.encode(params[0],"UTF-8") + "/password/temp");
            if (Config.DEBUG) {
                Log.d(TAG, "CREATE TEMP PASSWORD");
                Log.d(TAG, "URL:    " + url);
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Language", "en-US");  
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();
            
            if(connection.getResponseMessage() != null)
         	   Crittercism.leaveBreadcrumb("CreateTempPasswordAsyncTask : " + connection.getResponseMessage());
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                Log.w(TAG,"ConnectionCode: " + connection.getResponseCode() + "responseMessage: " + connection.getResponseMessage());
                return new YouChatException(connection.getResponseCode(), connection.getResponseMessage());
            }
            
            return "OK";
        } catch (Exception ex) {
            Log.w(TAG, "FAILED TO CREATE TEMP PASSWORD", ex);
            return ex;
        } finally {
            if (connection != null) {
                connection.disconnect(); 
            }
        }
    }
}

