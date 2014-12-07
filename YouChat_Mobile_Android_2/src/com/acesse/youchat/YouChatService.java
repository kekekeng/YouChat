package com.acesse.youchat;

import java.io.File;
import java.text.SimpleDateFormat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crittercism.app.Crittercism;
import com.yml.youchatlib.YouChatManager;


public class YouChatService extends Service {

    private static final String TAG = "YOUC";

    private final IBinder mBinder = new MyBinder();
    private final Handler handler = new Handler();
    private LoginResponse loginResponse;
    private boolean cancelLogin;
    private BroadcastReceiver netReceiver;
    private boolean isOnline;


    public static interface LoginResponse {
        public void onSuccess();
        public void onFailure();
    }


    @Override
    public void onCreate() {
        if (Config.DEBUG) {
            Log.d(TAG, "YouChatService.onCreate");
        }
        super.onCreate();

        //Settings.setServerUrl("dev1-openfire1-youchat.acesse.com");

        /*
        YouChatManager.getInstance().getStreamController().connectUsingPrefs(new ConnectChatCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected");
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "onError");
            }

            @Override
            public void onAuthenticated() {
                Log.d(TAG, "onAuthenticated");
            }
        });
        */
    }


    @Override
    public void onDestroy() {
        if (Config.DEBUG) {
            Log.d(TAG, "YouChatService.onDestroy");
        }
        //cleanup();
        super.onDestroy();
    }


    public void cancelLogin() {
        cancelLogin = true;
    }


    public void login(String user, String pass, LoginResponse response) {
        loginResponse = response;
    }




    /*
    public void displayMessageNotification(String fromSipUri, String fromName, String message) {

        Intent notifIntent = new Intent(this, YouChatLoginActivity.class);
        notifIntent.putExtra("chat", true);
        notifIntent.putExtra("contact", fromName);
        notifIntent.putExtra("contact_sip_iri", fromSipUri);

        PendingIntent notifContentIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        fromName = fromName == null ? fromSipUri : fromName;

        LinphoneChatRoom chatRoom = LinphoneManager.getLc().getOrCreateChatRoom(fromSipUri);
        int unreadCount = chatRoom.getUnreadMessagesCount();
        if (Config.DEBUG) {
            Log.d(TAG, "YouChatService.displayMessageNotification FROM: " + fromName + " #UNREAD: " + unreadCount);
        }

        mMsgNotifCount = mMsgNotif == null ? 1 : mMsgNotifCount+1;
        String title = mMsgNotifCount == 1 ? "Unread message from " + fromName : mMsgNotifCount + " unread messages";

        Bitmap contactIcon = null;
        try {
            contactIcon = ContactDAO.getInstance().getProfilePhoto(ContactDAO.getInstance().getContact(fromName).getPhotoUri());
        } catch (Exception e) {
        }
        //if (contactIcon == null) {
        //contactIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        //}
        Notification.Builder builder = new Notification.Builder(this)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.chat_icon_over)
        .setAutoCancel(true)
        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
        .setWhen(System.currentTimeMillis())
        .setLargeIcon(contactIcon);
        mMsgNotif = builder.build();
        mMsgNotif.contentIntent = notifContentIntent;

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(MESSAGE_NOTIF_ID, mMsgNotif);
    }


    private void onLoginFailure() {
        LinphoneManager.getInstance().stopIterating();
        LinphoneManager.removeListener(registrationListener);
        cleanup();
        loginResponse.onFailure();
    }


    private LinphoneOnRegistrationStateChangedListener registrationListener = new LinphoneOnRegistrationStateChangedListener() {
        public void onRegistrationStateChanged(RegistrationState state) {
            if (cancelLogin) {
                cancelLogin = false;
                isOnline = false;
                return;
            }
            if (state == RegistrationState.RegistrationOk) {

                isOnline = true;

                registerReceiver(netReceiver = new BroadcastReceiver() {      
                    public void onReceive(Context context, Intent intent) {
                        boolean hasConnectivity = MainApplication.hasNetworkConnectivity();
                        if (Config.DEBUG) {
                            Log.d(TAG, "----------------------> NETWORK CHANGE: " + (hasConnectivity ? "CONNECTIVITY" : "NO CONNECTIVITY"));
                        }
                        LinphoneCore core = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
                        if (hasConnectivity && core != null && core.getDefaultProxyConfig() != null && !core.getDefaultProxyConfig().isRegistered()) {
                            try {
                                LinphoneProxyConfig config = core.getDefaultProxyConfig();
                                config.enableRegister(true);
                                config.done();
                            } catch (Exception ex) {
                                Log.w(TAG, "UNABLE TO RE-REGISTER");
                            }
                        }
                    }
                }, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));


                LinphoneManager.removeListener(registrationListener);
                LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
                if (lc != null && lc.isNetworkReachable() && lc.getAuthInfosList() != null && lc.getAuthInfosList().length > 0 && lc.getDefaultProxyConfig() != null) {
                    final String acctId = lc.getAuthInfosList()[0].getUsername();
                    lc.getPresenceModel().setBasicStatus(PresenceBasicStatus.Open);
                    lc.enableKeepAlive(true);
                    new AsyncTask<Void,Void,Object>() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected Object doInBackground(Void... ignored) {
                            deleteOldMessages();
                             return ContactDAO.getInstance().init(acctId);
                        }
                        @Override
                        protected void onPostExecute(Object result) {
                            if (cancelLogin) {
                                cancelLogin = false;
                                return;
                            }
                            if (result instanceof Exception) {
                                onLoginFailure();
                            } else {
                                Contact acctContact = ContactDAO.getInstance().getAccountUser();
                                Crittercism.setUsername(acctContact.getName());
                                if (acctContact.getFirstName().isEmpty() || acctContact.getLastName().isEmpty()) {
                                    startActivity(new Intent(MainApplication.getInstance(), YouChatUpdateAccountActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("ISLOGGEDIN", true).putExtra("USER", acctContact.getName()));
                                } else {
                                    startActivity(new Intent(MainApplication.getInstance(), YouChatHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                }
                                loginResponse.onSuccess();
                            }
                        }
                    }.execute();
                } else {
                    Log.e(TAG, "Linphone registration ok, but proxy config was null.");
                    onLoginFailure();
                }
            } else if (state == RegistrationState.RegistrationFailed) {
                onLoginFailure();
            }
        }
    };


    public final void deleteOldMessages() {

        Runnable runner = new Runnable() {
            @Override
            public void run() {
                if (Config.DEBUG) {
                    Log.d(TAG, "Running delete old messages thread");
                }

                //
                // Delete any messages older than set history preference...
                //
                SimpleDateFormat sdf = new SimpleDateFormat("MMM, dd h:mm a");
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(YouChatService.this);
                String historyPref = preferences.getString("pref_history", "1 day");
                if (Config.DEBUG) {
                    Log.d(TAG, "Delete History Preference: " + historyPref);
                }
                long numDays = Long.parseLong(historyPref.substring(0, historyPref.indexOf(" ")));
                long diffTime = System.currentTimeMillis() - (numDays * 86400000L);
                if (Config.DEBUG) {
                    Log.d(TAG, "Delete Messages Older Than: " + sdf.format(diffTime));
                }
                boolean messagesDeleted = false;
                if (LinphoneManager.getLc() != null && LinphoneManager.getLc().getChatRooms() != null){
                    for (LinphoneChatRoom room: LinphoneManager.getLc().getChatRooms()){
                        for (LinphoneChatMessage msg: room.getHistory()){
                            if (msg.getTime() < diffTime){
                                if (Config.DEBUG) {
                                    Log.d(TAG, "Deleting message. From: "+ msg.getFrom() + " To:"  + msg.getPeerAddress() + " Time: " + sdf.format(msg.getTime()));
                                }
                                room.deleteMessage(msg);
                                messagesDeleted = true;
                                if (msg.getExternalBodyUrl() != null) {
                                    MainApplication.deleteContent(msg.getExternalBodyUrl());
                                }
                            }
                        }
                    }
                }

                //
                // Delete any dangling chat content...
                //
                if (Config.DEBUG) {
                    Log.d(TAG, "Delete Any Dangling Content Older Than: " + sdf.format(diffTime));
                }
                File dir = MainApplication.getChatStorageDirectory();
                for (File file : dir.listFiles()) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "CHAT: " + file);
                    }
                    if (file.lastModified() < diffTime) {
                        if (Config.DEBUG) {
                            Log.d(TAG, "Deleting file. " + file);
                        }
                        file.delete();
                    }
                }
            } 
        };
        Thread thread = new Thread(runner, "YOUCHAT-REAPER");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    } 


    public void clearNotification() {
        // delay to allow chime & vibrate if on chat details
        handler.postDelayed(new Runnable() {
            public void run() {
                removeMessageNotification();
                resetMessageNotifCount();
            }
        }, 2000);
    }


    public void goOffline() {
        if (isOnline && LinphoneManager.getLcIfManagerNotDestroyedOrNull() != null) {
            LinphoneManager.getLc().getPresenceModel().setBasicStatus(PresenceBasicStatus.Closed);
            ContactDAO.cleanup();
        }
        isOnline = false;
    }


    public void cleanup(){
        if (isOnline) {
            goOffline();
        }
        LinphoneManager.getLc().clearProxyConfigs();
        LinphoneManager.getLc().clearAuthInfos();
        try {
            unregisterReceiver(netReceiver);
        } catch (Exception ex) {
        }
        MainApplication.clearCaches();
    }
    */


    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }


    public class MyBinder extends Binder {
        YouChatService getService() {
            return YouChatService.this;
        }
    }

}