package com.acesse.youchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;



public class YouChatCreateAccountActivity extends Activity {

    private static final String TAG = "YOUC";

    private Handler handler = new Handler();



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        EditText password = (EditText) findViewById(R.id.password_text_1);
        EditText rePassword = (EditText) findViewById(R.id.password_text_2);

        password.setTypeface(Typeface.DEFAULT);
        password.setTransformationMethod(new PasswordTransformationMethod());

        rePassword.setTypeface(Typeface.DEFAULT);
        rePassword.setTransformationMethod(new PasswordTransformationMethod());

        if (Config.DEBUG) {
            String cmd = getIntent().getStringExtra("cmd");
            if (cmd != null && cmd.equals("create.account")) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        ((EditText) findViewById(R.id.username_text)).setText(getIntent().getStringExtra("user"));
                        ((EditText) findViewById(R.id.password_text_1)).setText(getIntent().getStringExtra("pass"));
                        ((EditText) findViewById(R.id.password_text_2)).setText(getIntent().getStringExtra("pass"));
                        ((EditText) findViewById(R.id.email_text)).setText(getIntent().getStringExtra("email"));
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                findViewById(R.id.create_account_button).performClick();
                            }
                        }, 500);
                    }
                }, 500);
            }
        }

        findViewById(R.id.cancel_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    public void onCreateAccount(View v) {

        final String user = ((EditText) findViewById(R.id.username_text)).getText().toString().trim();
        final String pass1 = ((EditText) findViewById(R.id.password_text_1)).getText().toString().trim();
        final String pass2 = ((EditText) findViewById(R.id.password_text_2)).getText().toString().trim();
        final String mail = ((EditText) findViewById(R.id.email_text)).getText().toString().trim();

        if (user.equalsIgnoreCase("registrar")) {
            String msg = getString(R.string.invalid_registrar, user);
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        if (user == null || user.length() == 0 || mail == null || mail.length() == 0) {
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.user_required)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        if (user.length() < 3 || user.length() > 20) {
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.invalid_user_id)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }
        if(!user.matches("^[[:alnum:]]*$")) {
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.youchat_id_rule)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(mail).matches()) {
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.email_invalid)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        if (pass1 == null || pass1.length() < 6 || pass1.length() > 25) {
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.alert_change_password2)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        if (!pass1.equals(pass2)) {
            new AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_error_icon)
            .setTitle(R.string.error_title)
            .setMessage(R.string.alert_change_password4)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }

        findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);

        new CreateAccountAsyncTask() {
            @Override
            protected void onPostExecute(Object result) {
                if (YouChatCreateAccountActivity.this.isFinishing()) {
                    return;
                }
                if (result instanceof Exception) {
                    findViewById(R.id.progressbar_container).setVisibility(View.GONE);
                    int msgId = R.string.error_account_general;
                    //Error Codes: 500 for fail | 409 for duplicate Registrar Account | 418 for duplicate YouChat ID | 419 for duplicate email bind to different Account
                    if (result instanceof YouChatException) {
                        int code = ((YouChatException) result).getCode();
                        if (code == 418) {
                            msgId = R.string.error_account_already_exists;
                        } else if (code == 419) {
                            msgId = R.string.error_account_email_exists;
                        }
                    }
                    new AlertDialog.Builder(YouChatCreateAccountActivity.this)
                    .setMessage(msgId)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    }).create().show();
                } else {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(YouChatCreateAccountActivity.this);
                    prefs.edit().putString("user", user).commit();
                    if (Config.DEBUG) {
                        finishAffinity();
                        startActivity(new Intent(YouChatCreateAccountActivity.this, YouChatHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        //startActivity(new Intent(YouChatCreateAccountActivity.this,YouChatUpdateAccountActivity.class).putExtra("USER", user).putExtra("PASSWORD", pass1).putExtras(getIntent()));
                    } else {
                        finishAffinity();
                        startActivity(new Intent(YouChatCreateAccountActivity.this, YouChatHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        //startActivity(new Intent(YouChatCreateAccountActivity.this,YouChatUpdateAccountActivity.class).putExtra("USER", user).putExtra("PASSWORD", pass1));
                    }
                }
            }
        }.execute(user, pass1, mail);
    }
}
