package com.acesse.youchat;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.crittercism.app.Crittercism;

import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;



public class ChangePasswordTask extends AsyncTask<String, Void, Object> {

    private static final String TAG = "YOUC";


    @Override
    protected Object doInBackground(String... params) {

        HttpURLConnection connection = null;
        try {
            Crittercism.leaveBreadcrumb("ChangePasswordTask: doInBackground");
            String acctId = PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance()).getString("user", "");
            String passwordUrl = "http://" + Config.DOMAIN_NODEJS + ":7000/api/account/" + acctId + "/password/change" ;
            String passwordString = "newPassword=" + params[0];
            if (Config.DEBUG) {
                Log.d(TAG, passwordUrl);
            }
            URL url = new URL(passwordUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(passwordString.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");  

            OutputStream os = connection.getOutputStream();
            os.write(passwordString.getBytes());
            os.close();

            if (connection.getResponseCode() != 200) {
                return new Exception(connection.getResponseCode() + ": " + connection.getResponseMessage());
            }

            //LinphoneManager.getLc().getAuthInfosList()[0].setPassword(params[0]);

        } catch (Exception ex) {
            Log.e(TAG, "Change password failed", ex);
            return ex;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "OK";
    }
}
