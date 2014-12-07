package com.acesse.youchat; 

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;


/**
 * 
 */
public class GetAccountsAsyncTask extends AsyncTask<Void, Void, Void> {

    private final String TAG = "YOUC"; 

    @Override
    protected Void doInBackground(Void... ignored) {
        Scanner inStream = null;
        HttpURLConnection connection = null;
        try {
            List names = new ArrayList();
            /*
            List<Contact> contacts = ContactDAO.getInstance().getContacts();
            List names = new ArrayList(contacts.size());
            for (Contact c : contacts) {
                names.add(c.getName());
            }
            */
            JSONArray jarr = new JSONArray(names);
            String params = "ids=" + URLEncoder.encode(jarr.toString(),"UTF-8");
            URL url = new URL("http://" + Config.DOMAIN_NODEJS + ":7000/api/contacts/fetch");
            if (Config.DEBUG) {
                Log.d(TAG, "POST CONTACTS: " + url + " " + jarr);
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

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(params);
            wr.flush();
            wr.close();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new YouChatException(connection.getResponseCode(), connection.getResponseMessage());
            }

            inStream = new Scanner(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            while(inStream.hasNextLine()){
                response = response.append(inStream.nextLine());
            }

            jarr = new JSONArray(response.toString());
            if (Config.DEBUG) {
                Log.d(TAG,"CONTACTS RESULT:" + jarr);
            }
            for (int i = 0; i < jarr.length(); i++){
                //ContactDAO.getInstance().updateContact(jarr.getJSONObject(i));
            }
        } catch (Exception ex) {
            Log.e(TAG, "Get Accounts Failed", ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch(Exception ex) {}
        }
        return null;
    }
}