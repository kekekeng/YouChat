package com.acesse.youchat;

import com.crittercism.app.Crittercism;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ChangePasswordActivity extends Activity {

    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText reenterPasswordEditText;
    private Button changePasswordButton;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.change_password_layout);
        
        Crittercism.leaveBreadcrumb("ChangePasswordActivity : onCreate");

        getActionBar().setTitle("\t\tPassword");

        context = this;

        currentPasswordEditText = (EditText) findViewById(R.id.current_password_edit_text);
        newPasswordEditText = (EditText) findViewById(R.id.new_password_edit_text);
        reenterPasswordEditText = (EditText) findViewById(R.id.reenter_password_edit_text);
        changePasswordButton = (Button) findViewById(R.id.change_password_button);

        currentPasswordEditText.setTypeface(Typeface.DEFAULT);
        currentPasswordEditText
                .setTransformationMethod(new PasswordTransformationMethod());

        newPasswordEditText.setTypeface(Typeface.DEFAULT);
        newPasswordEditText
                .setTransformationMethod(new PasswordTransformationMethod());

        reenterPasswordEditText.setTypeface(Typeface.DEFAULT);
        reenterPasswordEditText
                .setTransformationMethod(new PasswordTransformationMethod());

        changePasswordButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (!MainApplication.hasNetworkConnectivity()) {
                    MainApplication
                            .showNetworkUnavailableDialog(getFragmentManager());
                    return;
                }
                //String currentPassword = LinphoneManager.getLc() .getAuthInfosList()[0].getPassword();
                String currentPassword = "";

                String newPassword = newPasswordEditText.getText().toString();

                String reenteredPassword = reenterPasswordEditText.getText()
                        .toString();

                // Pattern pattern =
                // Pattern.compile(YouChatLoginActivity.PASSWORD_PATTERN);
                // Matcher matcher = pattern.matcher(newPassword);

                if ((currentPasswordEditText.getText().toString() == null || currentPasswordEditText
                        .getText().toString().isEmpty())
                        || (newPassword == null || newPassword.isEmpty())
                        || (reenteredPassword == null || reenteredPassword
                                .isEmpty())) {
                    new AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_error_icon)
                            .setTitle(getString(R.string.error_title))
                            .setMessage(
                                    getString(R.string.alert_change_password3))
                            .setNegativeButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                        }
                                    }).create().show();
                    return;
                }

                if (newPassword != null && newPassword.trim().length() > 5
                        && newPassword.trim().length() < 26) {
                    if (newPassword.equals(reenteredPassword)
                            && currentPasswordEditText.getText().toString()
                                    .equals(currentPassword)) {

                        if (currentPassword.equals(newPassword)) {
                            new AlertDialog.Builder(context)
                                    .setIcon(R.drawable.ic_error_icon)
                                    .setTitle(getString(R.string.error_title))
                                    .setMessage(
                                            getString(R.string.alert_change_password))
                                    .setNegativeButton(
                                            android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int id) {
                                                }
                                            }).create().show();

                            return;
                        }

                        changePasswordButton.setText("Changing Password ...");
                        new ChangePasswordTask() {
                            @Override
                            protected void onPostExecute(Object result) {
                                if (result instanceof Exception) {
                                    new AlertDialog.Builder(context)
                                            .setIcon(
                                                    R.drawable.ic_error_icon)
                                            .setTitle(
                                                    getString(R.string.error_title))
                                            .setMessage(
                                                    getString(R.string.error_update_password))
                                            .setNegativeButton(
                                                    android.R.string.ok,
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(
                                                                DialogInterface dialog,
                                                                int id) {
                                                        }
                                                    }).create().show();

                                } else {
                                	Crittercism.leaveBreadcrumb("ChangePasswordActivity : Password Changed");
                                    new AlertDialog.Builder(context)
                                            .setMessage(
                                                    getString(R.string.password_updated))
                                            .setPositiveButton(
                                                    getResources().getString(
                                                            R.string.ok),
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(
                                                                DialogInterface dialog,
                                                                int which) {
                                                            if (Config.DEBUG
                                                                    && PreferenceManager
                                                                            .getDefaultSharedPreferences(
                                                                                    context)
                                                                            .contains(
                                                                                    "user")) {
                                                                PreferenceManager
                                                                        .getDefaultSharedPreferences(
                                                                                context)
                                                                        .edit()
                                                                        .putString(
                                                                                "pass",
                                                                                "")
                                                                        .commit();
                                                            }
                                                            finish();
                                                        }
                                                    }).create().show();
                                }
                            }
                        }.execute(newPassword);
                    } else {
                        if (!currentPasswordEditText.getText().toString()
                                .equals(currentPassword)) {
                            new AlertDialog.Builder(context)
                                    .setIcon(R.drawable.ic_error_icon)
                                    .setTitle(getString(R.string.error_title))
                                    .setMessage(
                                            getString(R.string.alert_change_password1))
                                    .setNegativeButton(
                                            android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int id) {
                                                }
                                            }).create().show();
                        }

                        else if (!newPassword.equals(reenteredPassword)) {
                            new AlertDialog.Builder(context)
                                    .setIcon(R.drawable.ic_error_icon)
                                    .setTitle(getString(R.string.error_title))
                                    .setMessage(
                                            getString(R.string.alert_change_password4))
                                    .setNegativeButton(
                                            android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int id) {
                                                }
                                            }).create().show();
                        } else if (newPasswordEditText
                                .getText()
                                .toString()
                                .equals(currentPasswordEditText.getText()
                                        .toString())) {
                            new AlertDialog.Builder(context)
                                    .setIcon(R.drawable.ic_error_icon)
                                    .setTitle(getString(R.string.error_title))
                                    .setMessage(
                                            getString(R.string.alert_change_password))
                                    .setNegativeButton(
                                            android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int id) {
                                                }
                                            }).create().show();
                        } else {
                            new AlertDialog.Builder(context)
                                    .setIcon(R.drawable.ic_error_icon)
                                    .setTitle(getString(R.string.error_title))
                                    .setMessage(
                                            getString(R.string.alert_change_password3))
                                    .setNegativeButton(
                                            android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int id) {
                                                }
                                            }).create().show();
                        }
                    }
                } else {
                    new AlertDialog.Builder(context)
                            .setIcon(R.drawable.ic_error_icon)
                            .setTitle(getString(R.string.error_title))
                            .setMessage(
                                    getString(R.string.alert_change_password2))
                            .setNegativeButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog, int id) {
                                        }
                                    }).create().show();
                }
            }
        });

        newPasswordEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 25)
                    Toast.makeText(context,
                            getString(R.string.alert_change_password2),
                            Toast.LENGTH_LONG).show();

            }
        });

        reenterPasswordEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 25)
                    Toast.makeText(context,
                            getString(R.string.alert_change_password2),
                            Toast.LENGTH_LONG).show();

            }
        });
    }
}
