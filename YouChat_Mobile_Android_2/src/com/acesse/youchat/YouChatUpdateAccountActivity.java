package com.acesse.youchat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import com.crittercism.app.Crittercism;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;




public class YouChatUpdateAccountActivity extends Activity {

    private static final String TAG = "YOUC";
    //private YouChatService service;
    private String photoUrl;
    private String acctId;
    private String password;
    private boolean isLoggedIn;
    private Handler handler = new Handler();
    private Intent originIntent;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);
        findViewById(R.id.update_profile_pic).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //                Intent galleryIntent = new Intent();
                //                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                //                galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
                //                galleryIntent.setType("image/*");
                //                galleryIntent.putExtra("return-data", true);
                //                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_or_capture_photo));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { takePhotoIntent });
                startActivityForResult(chooserIntent, 1);
            }
        });

        //Populate the user/password from intent
        acctId     = getIntent().getExtras().getString("USER"); 
        password = getIntent().getExtras().getString("PASSWORD");
        isLoggedIn = getIntent().getExtras().getBoolean("ISLOGGEDIN");
        if (Config.DEBUG) {
            Log.d(TAG, "Updating Account for " + acctId);
        }

        if (Config.DEBUG) {
            String cmd = getIntent().getStringExtra("cmd");
            if (!isLoggedIn && cmd != null && cmd.equals("create.account")) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        ((EditText) findViewById(R.id.firstname_text_2)).setText(getIntent().getStringExtra("first"));
                        ((EditText) findViewById(R.id.lastname_text_2)).setText(getIntent().getStringExtra("last"));
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                findViewById(R.id.update_profile_button).performClick();
                            }
                        }, 500);
                    }
                }, 500);
            }
        }
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

    public void onUpdateProfile(View v) {
        if (Config.DEBUG) {
            Log.d(TAG, "onUpdateProfile clicked");
        }
        final String firstName = ((EditText) findViewById(R.id.firstname_text_2)).getText().toString();
        final String lastName  = ((EditText) findViewById(R.id.lastname_text_2)).getText().toString();
        final String company   = ((EditText) findViewById(R.id.company_text_2)).getText().toString();

        if (firstName == null || firstName.length() < 1 || lastName == null || lastName.length() < 1){
            new AlertDialog.Builder(YouChatUpdateAccountActivity.this)
            .setMessage(getString(R.string.error_profile_required_fields))
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            }).create().show();
            return;
        }
        findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);

        new UpdateAccountAsyncTask() {
            @Override
            protected void onPostExecute(Object result) {
                findViewById(R.id.progressbar_container).setVisibility(View.GONE);
                if (result instanceof Exception) {
                    String msg = "Error Updating Account: " + result.toString();
                    new AlertDialog.Builder(YouChatUpdateAccountActivity.this)
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    }).create().show();
                } else {
                    //Toast.makeText(YouChatUpdateAccountActivity.this, "Account updated.", Toast.LENGTH_SHORT).show();
                    ((TextView)findViewById(R.id.progressBarText)).setText(R.string.account_updated);
                    findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);
                    if (isLoggedIn) {
                        if (Config.DEBUG) {
                            Log.d(TAG,"isLoggedIn is true");
                        }
                        //Set the new values in the contact DAO
                        /*
                        if (ContactDAO.getInstance().getAccountUser() != null) {
                            ContactDAO.getInstance().getAccountUser().setFirstName(firstName);
                            ContactDAO.getInstance().getAccountUser().setLastName(lastName);
                            ContactDAO.getInstance().getAccountUser().setCompany(company);
                            if (photoUrl != null) {
                                ContactDAO.getInstance().getAccountUser().setPhotoUri(Uri.parse(photoUrl));
                            }
                        }
                        */
                        startActivity(new Intent(MainApplication.getInstance(), YouChatHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        finishAffinity();
                    } else {
                        /*
                        service.login(acctId, password, new YouChatService.LoginResponse() {
                            @Override
                            public void onSuccess() {
                                finishAffinity();
                            }
                            @Override
                            public void onFailure() {
                                findViewById(R.id.progressbar_container).setVisibility(View.GONE);
                                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(YouChatUpdateAccountActivity.this);
                                pref.edit().remove("user").remove("pass").commit();
                                new AlertDialog.Builder(YouChatUpdateAccountActivity.this)
                                .setIcon(R.drawable.ic_error_icon)
                                .setTitle(R.string.error)
                                .setMessage(R.string.creation_success_login_failed)
                                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                })
                                .create().show();
                            }
                        });
                        */
                    }
                }
            }
        }.execute(acctId, firstName, lastName, company, photoUrl);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + ".onActivityResult " + requestCode + " " + resultCode);
        }

        switch(requestCode){
            case 1:
                if (intent != null && resultCode == Activity.RESULT_OK) {
                    originIntent = intent;
                    try {
                        Intent cropIntent = new Intent("com.android.camera.action.CROP"); 
                        cropIntent.setDataAndType(intent.getData(), "image/*");
                        cropIntent.putExtra("crop", "true");
                        cropIntent.putExtra("aspectX", 1);
                        cropIntent.putExtra("aspectY", 1);
                        cropIntent.putExtra("outputX", 128);
                        cropIntent.putExtra("outputY", 128);
                        cropIntent.putExtra("return-data", true);
                        startActivityForResult(cropIntent, 2);
                    } catch(ActivityNotFoundException anfe) {
                        processPhoto(intent);
                    }
                }
                break;
            case 2:
                if (resultCode == Activity.RESULT_CANCELED && originIntent != null && originIntent.getData() != null) {
                    intent = originIntent;
                }  
                processPhoto(intent);                   
                break;
        }
    }

    private void processPhoto(Intent intent) {
        try {
            Uri uri = intent.getData();
            if (Config.DEBUG) {
                Log.d(TAG, getClass().getSimpleName() + ".onActivityResult " + uri);
            }
            String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            File file = new File(MainApplication.getContactStorageDirectory(), fileName + ".jpg");

            //
            // Copy the photo to YouChat private dir...
            //
            if (uri != null) {
                try {               
                    InputStream in = new BufferedInputStream(getContentResolver().openInputStream(uri));
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    byte[] data = new byte[1024 * 8];
                    int bytesRead = 0;
                    while ((bytesRead = in.read(data)) > 0) {
                        out.write(data, 0, bytesRead);
                    }
                    out.close();
                    in.close();
                } catch (Exception ex) {
                    Log.w(TAG, "FAILED TO COPY CONTENT TO CONTACT STORAGE: " + file, ex);
                }
            } else if (intent.getExtras() != null && intent.getExtras().containsKey("data")) {
                try {               
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                    ((Bitmap) intent.getExtras().get("data")).compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();
                } catch (Exception ex) {
                    Log.w(TAG, "FAILED TO COPY CONTENT TO CONTACT STORAGE: " + file, ex);
                }
            }

            String filePath = file.getAbsolutePath();

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = Helper.getSampleSize(filePath, 400);

            Bitmap bmap = BitmapFactory.decodeFile(filePath, opts);
            bmap = Helper.rotateImage(filePath, bmap);

            //Set the image
            ((ImageView)findViewById(R.id.update_profile_pic)).setImageBitmap(bmap);

            new UploadPictureAsyncTask(acctId) {
                @Override
                protected void onPostExecute(Object result) {
                    if (result instanceof JSONObject) {
                        photoUrl = ((JSONObject) result).optString("return_picture_url");
                    } else {
                        ((ImageView)findViewById(R.id.update_profile_pic)).setImageResource(R.drawable.unknown_small);
                        Toast.makeText(YouChatUpdateAccountActivity.this, R.string.failed_upload_photo, Toast.LENGTH_SHORT).show();
                    }
                }
            }.execute(bmap);

        } catch (Exception ex) {
            Log.e(TAG, "Failed to get picture.", ex);
        }
    }



    @Override
    public void onBackPressed() {
        if (Config.DEBUG) {
            Log.d(TAG, "YouChatUpdateAccountActivity.onBackPressed");
        }
        //if (service != null) {
            //stopService(new Intent(this, YouChatService.class));
        //}
        super.onBackPressed();
    }



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