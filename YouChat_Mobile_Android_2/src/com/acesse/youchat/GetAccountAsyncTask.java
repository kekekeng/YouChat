package com.acesse.youchat; 

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;


/**
 * 
 */
public class GetAccountAsyncTask extends AsyncTask<String, Integer, Object> {

    private final String TAG = "YOUC"; 

    @Override
    protected Object doInBackground(String... params) {
        Scanner inStream = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://" + Config.DOMAIN_NODEJS + ":7000/api/contact/" + params[0]);
            if (Config.DEBUG) {
                Log.d(TAG, "GET CONTACT: " + url);
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.w(TAG,"ConnectionCode: " + connection.getResponseCode() + "responseMessage: " + connection.getResponseMessage());
                return new YouChatException(connection.getResponseCode(), connection.getResponseMessage());
            }
            inStream = new Scanner(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            while(inStream.hasNextLine()){
                response = response.append(inStream.nextLine());
            }

            JSONObject jobj = new JSONObject(response.toString());
            if (Config.DEBUG) {
                Log.d(TAG,"CONTACT RESULT:" + jobj);
            }
            //
            // TODO: look at using ContactDAO.updateContact instead...
            //
            final String pictureUrl = jobj.optString("profile_picture_url", "");
            /*
            final Contact contact = new Contact(UUID.randomUUID().toString(), params[0]);
            contact.setFirstName(jobj.optString("first_name", ""));
            contact.setLastName(jobj.optString("last_name", ""));
            contact.setCompany(jobj.optString("company", ""));
            contact.setEmail(jobj.optString("profile_email"));
            if (!pictureUrl.isEmpty()) {
                contact.setPhotoUri(Uri.parse(pictureUrl));
                ContactDAO.getInstance().fetchProfilePhoto(pictureUrl);
            }
            return contact;
            */
            return "";
        } catch (JSONException ex) {
            Log.e(TAG, "Get Account Failed - JSON error", ex);
            return ex;
        } catch (IOException ex) {
            Log.e(TAG, "Get Account Failed", ex);
            return ex;
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
    }
}