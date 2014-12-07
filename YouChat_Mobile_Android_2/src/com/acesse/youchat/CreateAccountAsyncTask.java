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
public class CreateAccountAsyncTask extends AsyncTask<String, Integer, Object> {

    private static final String TAG = "YOUC";


    @Override
    protected Object doInBackground(String... params) {
        HttpURLConnection connection = null;  
        try {

            //http://example.com:9090/plugins/userService/userservice?type=add&secret=bigsecret&username=kafka&password=drowssap&name=franz&email=franz@kafka.com
            String uparams = "";
            uparams += "type=add";
            uparams += "&secret=asdfasdf";
            uparams += "&username=" + URLEncoder.encode(params[0], "UTF-8");
            uparams += "&password=" + URLEncoder.encode(params[1], "UTF-8");
            uparams += "&name=" + URLEncoder.encode(params[0], "UTF-8");
            uparams += "&email=" + URLEncoder.encode(params[2], "UTF-8");
            URL url = new URL("http://" + Config.DOMAIN_XMPP + ":9090/plugins/userService/userservice");
            /*
            String uparams = "";
            uparams += "accountID=" + URLEncoder.encode(params[0], "UTF-8");
            uparams += "&password=" + URLEncoder.encode(params[1], "UTF-8");
            uparams += "&email=" + URLEncoder.encode(params[2], "UTF-8");
            URL url = new URL("http://" + Config.DOMAIN_NODEJS + ":7000/api/account");
             */
            if (Config.DEBUG) {
                Log.d(TAG, "CREATE ACCOUNT");
                Log.d(TAG, "URL:    " + url);
                Log.d(TAG, "PARAMS: " + uparams);
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(uparams.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");  
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(uparams);
            wr.flush();
            wr.close();

            if (connection.getResponseMessage() != null) {
                Crittercism.leaveBreadcrumb("CreateAccountAsyncTask : " + connection.getResponseMessage());
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            //if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
                Log.w(TAG, "ConnectionCode: " + connection.getResponseCode() + " responseMessage: " + connection.getResponseMessage());
                return new YouChatException(connection.getResponseCode(), connection.getResponseMessage());
            }
            
            // parse the xml response for an error message...

            return "OK";
        } catch (Exception ex) {
            Log.w(TAG, "FAILED TO CREATE ACCOUNT", ex);
            return ex;
        } finally {
            if (connection != null) {
                connection.disconnect(); 
            }
        }
    }
}

