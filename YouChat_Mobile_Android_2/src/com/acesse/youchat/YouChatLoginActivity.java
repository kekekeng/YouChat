package com.acesse.youchat;

import org.jivesoftware.smack.Roster.SubscriptionMode;

import com.yml.youchatlib.Callbacks.ConnectChatCallback;
import com.yml.youchatlib.Settings;
import com.yml.youchatlib.YouChatManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;



public class YouChatLoginActivity extends Activity {

    private static final String TAG = "YOUC";
    //private static final String PASSWORD_PATTERN = "((?=.*\\d).{6,20})";
    //private YouChatService service;
    private Handler handler = new Handler();
    private SharedPreferences prefs;
    private int numRetries;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.d(TAG, "LoginActivity.onCreate()");
        }
        super.onCreate(savedInstanceState);

        if (MainApplication.hasNetworkConnectivity()) {
            //startService(new Intent(this, YouChatService.class));
        }

        // user selected notification
        if (getIntent().hasExtra("chat") && isRegistered()) {
            startActivity(new Intent(YouChatLoginActivity.this, YouChatHomeActivity.class).putExtras(getIntent().getExtras()));
            finishAffinity();
            return;
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setContentView(R.layout.activity_login);

        findViewById(R.id.splash_container).setVisibility(View.VISIBLE);
        findViewById(R.id.login_container).setVisibility(View.GONE);

        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView versionTextView = (TextView) findViewById(R.id.app_version_text_view);
            String versionString = "Version " + pinfo.versionName + " (" + pinfo.versionCode + ") ";
            versionString += Config.DEBUG ? "DEBUG " : "";
            versionString += Config.DOMAIN_XMPP.substring(0, Config.DOMAIN_XMPP.indexOf("-"));
            versionTextView.setText(versionString);
        } catch (Exception e) {
            Log.e(TAG, "FAILED TO GET PACKAGE INFO");
        }

        handler.postDelayed(new Runnable() {
            public void run() {
                if (Config.DEBUG) {
                    String cmd = getIntent().getStringExtra("cmd");
                    if (cmd != null && cmd.equals("create.account")) {
                        startActivity(new Intent(YouChatLoginActivity.this, YouChatCreateAccountActivity.class).putExtras(getIntent()));
                        return;
                    } else if (cmd != null && cmd.equals("login")) {
                        ((EditText) findViewById(R.id.username_text)).setText(getIntent().getStringExtra("name"));
                        ((EditText) findViewById(R.id.password_text)).setText(getIntent().getStringExtra("pass"));
                        findViewById(R.id.login_button).performClick();
                        return;
                    }
                }
                if (isRegistered()) {
                    startActivity(new Intent(YouChatLoginActivity.this, YouChatHomeActivity.class));
                    finishAffinity();
                } else {
                    findViewById(R.id.splash_container) .setVisibility(View.GONE);
                    findViewById(R.id.login_container).setVisibility( View.VISIBLE);

                    ((EditText) findViewById(R.id.username_text)).setText(prefs.getString("user", ""));

                    EditText password = (EditText) findViewById(R.id.password_text);
                    password.setTypeface(Typeface.DEFAULT);
                    password.setTransformationMethod(new PasswordTransformationMethod());
                    password.setText(prefs.getString("pass", ""));

                    //if (prefs.contains("crash")) {
                    //showCrashDialog();
                    //}
                    //if (getIntent().hasExtra("error")) {
                    //Toast.makeText(YouChatLoginActivity.this, R.string.app_restart_message, Toast.LENGTH_SHORT).show();
                    //}
                }
            }
        }, getIntent().hasExtra("logout") ? 1 : 1500);
    }


    @Override
    protected void onStop() {
        super.onStop();
        ((EditText) findViewById(R.id.username_text)).setText("");
        ((EditText) findViewById(R.id.password_text)).setText("");
    }



    @Override
    protected void onResume() {
        super.onResume();
        //bindService(new Intent(this, YouChatService.class), mConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onPause() {
        super.onPause();
        //unbindService(mConnection);
    }


    private boolean isRegistered() {
        /*
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        return lc != null && lc.isNetworkReachable() && lc.getDefaultProxyConfig() != null && lc.getDefaultProxyConfig().isRegistered();
         */
        return false;
    }


    public void onLogin(final View view) {

        if (!MainApplication.hasNetworkConnectivity()) {
            MainApplication.showNetworkUnavailableDialog(getFragmentManager());
            return;
        }

        /*
        if (service == null) {
            if (numRetries == 0) {
                //startService(new Intent(this, YouChatService.class));
                handler.postDelayed(new Runnable() {
                    public void run() {
                        onLogin(view);
                    }
                }, 500);
                numRetries++;
            } else {
                new AlertDialog.Builder(YouChatLoginActivity.this)
                .setIcon(R.drawable.ic_error_icon)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(R.string.error_starting_service)
                .create().show();
            }
            return;
        }
         */

        final String user = ((EditText) findViewById(R.id.username_text)).getText().toString().toLowerCase();
        final String password = ((EditText) findViewById(R.id.password_text)).getText().toString();

        if (user.isEmpty() || password.isEmpty()) {
            new AlertDialog.Builder(YouChatLoginActivity.this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.alert_login)
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        if (password.length() < 6 || user.length() < 3) {
            new AlertDialog.Builder(YouChatLoginActivity.this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.alert_login2)
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        // Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        // Matcher matcher = pattern.matcher(password);
        // if (!matcher.matches()) {
        // new
        // AlertDialog.Builder(YouChatLoginActivity.this).setIcon(android.R.drawable.ic_dialog_alert).setTitle("Alert").setMessage("Invalid password.")
        // .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dialog, int id) {
        // }
        // }).create().show();
        // return;
        // }
        findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);

        //Settings.setServerUrl("dev1-openfire1-youchat.acesse.com");
        Settings.setServerUrl(Config.DOMAIN_XMPP);
        Settings.setJabberIdSuffix("youchat");
        Settings.setSubscriptionMode(SubscriptionMode.accept_all);
        Settings.setContinuousPingInterval(0);

        YouChatManager.getInstance().getStreamController().connect(user, password, true, new ConnectChatCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "onConnected");
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "onError");
                handler.post(new Runnable() {
                    public void run() {
                        findViewById(R.id.progressbar_container).setVisibility(View.GONE);
                        if (!MainApplication.hasNetworkConnectivity()) {
                            MainApplication.showNetworkUnavailableDialog(getFragmentManager());
                            return;
                        }
                        new AlertDialog.Builder(YouChatLoginActivity.this)
                        .setIcon(R.drawable.ic_error_icon)
                        .setTitle(R.string.error_title)
                        .setMessage( R.string.login_failed)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick( DialogInterface dialog, int id) {
                            }
                        }).create().show();
                    }
                });
            }

            @Override
            public void onAuthenticated() {
                Log.d(TAG, "onAuthenticated");
                startActivity(new Intent(YouChatLoginActivity.this, YouChatHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                finishAffinity();
            }
        });
    }


    public void onCreateAccount(View view) {
        if (!MainApplication.hasNetworkConnectivity()) {
            MainApplication.showNetworkUnavailableDialog(getFragmentManager());
            return;
        }
        startActivity(new Intent(YouChatLoginActivity.this, YouChatCreateAccountActivity.class));
    }


    public void onForgotPassword(View view) {
        if (!MainApplication.hasNetworkConnectivity()) {
            MainApplication.showNetworkUnavailableDialog(getFragmentManager());
            return;
        }
        if (Config.DEBUG) {
            Log.d(TAG, "YouChatLoginActivity.onForgotPassword clicked");
        }
        final String accountId = ((EditText) findViewById(R.id.username_text)).getText().toString().trim();

        if (accountId == null || accountId.length() < 1) {
            new AlertDialog.Builder(YouChatLoginActivity.this).setMessage(getString(R.string.error_invalid_username2, accountId)).create().show();
            return;
        }
        new AlertDialog.Builder(this)
        .setMessage(R.string.temporary_password)
        .setIcon(R.drawable.ic_error_icon)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);
                if (Config.DEBUG) {
                    Log.d(TAG, "Forgot Password Yes  button clicked");
                }
                new CreateTempPasswordAsyncTask() {
                    @Override
                    protected void onPostExecute(Object result) {
                        findViewById(R.id.progressbar_container).setVisibility(View.GONE);
                        if (result instanceof Exception) {
                            if (result instanceof YouChatException && ((YouChatException) result).getCode() == 404) {
                                new AlertDialog.Builder(YouChatLoginActivity.this)
                                .setTitle(getResources().getString(R.string.reset_password))
                                .setMessage(getResources().getString(R.string.error_temporary_password))
                                .setNegativeButton(getResources().getString(R.string.contact_support), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://marketing.acesse.com/public/contact.php")));
                                    }
                                }).setPositiveButton(getResources().getString(R.string.try_again), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ((EditText)findViewById(R.id.username_text)).setText("");
                                        dialog.cancel();
                                    }
                                }).create()
                                .show();
                            } else {
                                new AlertDialog.Builder(YouChatLoginActivity.this).setMessage(R.string.error_temporary_password).create().show();
                            }
                        } else {
                            new AlertDialog.Builder(YouChatLoginActivity.this)
                            .setMessage(R.string.msg_temporary_password_created)
                            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick( DialogInterface dialog, int id) {
                                }
                            }).create().show();

                        }
                    }
                }.execute(accountId);
            }
        })
        .setNegativeButton(getResources().getString(R.string.no), null)
        .show();
    }


    @Override
    public void onBackPressed() {
        if (Config.DEBUG) {
            Log.d(TAG, "YouChatLoginActivity.onBackPressed");
        }
        //if (service != null) {
        //service.cancelLogin();
        //}
        super.onBackPressed();
    }


    /*
    private void showCrashDialog() {
        new AlertDialog.Builder(YouChatLoginActivity.this)
        .setTitle(R.string.oops_title)
        .setMessage(R.string.oops_msg)
        .setCancelable(false)
        .setPositiveButton(R.string.alert_send_report, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String txt = prefs.getString("crash", "");
                try {
                    PackageInfo pinfo = getPackageManager() .getPackageInfo(getPackageName(), 0);
                    txt += "App Version: " + pinfo.versionName + " (" + pinfo.versionCode + ")";
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage(), e);
                }
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "youchat@corp.acesse.com" });
                intent.putExtra(Intent.EXTRA_TEXT, txt);
                intent.putExtra(Intent.EXTRA_SUBJECT, "Android - YouChat Crash Report");
                intent.setType("message/rfc822");
                startActivity(Intent.createChooser(intent, "Send report:"));
                prefs.edit().remove("crash").commit();
            }
        })
        .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().remove("crash").commit();
            }
        }).create().show();
    }
     */


    /*
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            YouChatService.MyBinder b = (YouChatService.MyBinder) binder;
            service = b.getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };
     */


}