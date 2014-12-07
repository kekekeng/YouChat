package com.acesse.youchat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.json.JSONObject;

import com.yml.youchatlib.models.User;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;



public class SettingsFragment extends Fragment {

    private static final String TAG = "YOUC";
    private String photoUrl;
    private SharedPreferences preferences;
    private User user;
    private TextView profileTextView;
    private Button logoutButton;
    private ImageView profilePictureImageView;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText companyNameEditText;
    private Button changePasswordButton;
    private LinearLayout saveHistoryLayout;
    private TextView historyTextView;
    //private LinearLayout videoSizeLayout;
    //private TextView videoSizeTextView;
    private LinearLayout aboutLayout;   
    private SharedPreferences pref;
    //ContactDAO contactDAO;

    private String firstName;
    private String lastName;
    private String companyName;
    private String acctId;
    private Intent originIntent;


    /**
     * Use this factory method to create a new instance of this fragment using
     * the provided parameters.
     */
    public static SettingsFragment newInstance(String acctId) {
        SettingsFragment fragment = new SettingsFragment();
        fragment.acctId = acctId;
        return fragment;
    }


    public SettingsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.settings_layout, container, false);

        logoutButton = (Button) view.findViewById(R.id.logout_button);
        profileTextView = (TextView) view.findViewById(R.id.profile_text_view);
        profilePictureImageView = (ImageView) view.findViewById(R.id.profile_picture);
        firstNameEditText = (EditText) view.findViewById(R.id.first_name_edit_text);
        lastNameEditText = (EditText) view.findViewById(R.id.last_name_edit_text);
        companyNameEditText = (EditText) view.findViewById(R.id.company_name_edit_text);
        changePasswordButton = (Button) view.findViewById(R.id.change_password_button);
        saveHistoryLayout = (LinearLayout) view.findViewById(R.id.save_history_layout);
        historyTextView = (TextView) view.findViewById(R.id.history_text_view);
        //videoSizeLayout = (LinearLayout) view.findViewById(R.id.preferred_video_size_layout);
        //videoSizeTextView = (TextView) view.findViewById(R.id.video_size_text_view);
        aboutLayout = (LinearLayout) view.findViewById(R.id.about_layout);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String history = pref.getString("pref_history", "");
        //String videoSize = pref.getString("pref_video", "");

        if(history != null && history.length() > 0)
        {
            historyTextView.setText(history);
        }

        //if (videoSize != null && videoSize.length() > 0) {
        //videoSizeTextView.setText(videoSize);
        //}

        //contactDAO = ContactDAO.getInstance();
        //contact = contactDAO.getAccountUser();

        /*
        firstName = contact.getFirstName();
        lastName = contact.getLastName();
        companyName = contact.getCompany();
        String email = contact.getEmail();
        photoUrl = contact.getPhotoUri() == null ? null : contact.getPhotoUri().toString();

        if (email != null && !email.isEmpty()) {
            profileTextView.setText("Profile  - " + acctId + " (" + email + ")");
        } else {
            profileTextView.setText("Profile  - " + acctId );
        }    
        */

        firstNameEditText.setText(firstName);
        lastNameEditText.setText(lastName);
        companyNameEditText.setText(companyName);

        //profilePictureImageView.setImageBitmap(ContactDAO.getInstance().getProfilePhoto(contact.getPhotoUri()));

        firstNameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                /*
                if(!firstNameEditText.getText().toString().equals(contact.getFirstName()))
                    saveAccount();
                    */
            }
        });

        lastNameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                /*
                if(!lastNameEditText.getText().toString().equals(contact.getLastName()))
                    saveAccount();
                    */
            }
        });

        companyNameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                /*
                if(!companyNameEditText.getText().toString().equals(contact.getCompany()))
                    saveAccount();
                    */
            }
        });	


        saveHistoryLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                final CharSequence[] items = new CharSequence[] {"1 day", "7 days", "14 days", "30 days"};

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                int choice = Arrays.asList(items).indexOf(pref.getString("pref_history", "1 day"));

                builder.setTitle("History").setSingleChoiceItems(items, choice, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        historyTextView.setText(items[which]);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("pref_history", items[which].toString());
                        editor.commit();
                    }
                }).setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();

            }
        });

        aboutLayout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
            }
        });

        /*
        videoSizeLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence[] items = new CharSequence[] {"QCGA (320*420)", "VGA (640*480)", "HD (960*720)"};
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Video Size").setItems(items, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        videoSizeTextView.setText(items[which]);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("pref_video", items[which].toString());
                        editor.commit();
                    }
                }).create().show();
            }
        });
         */

        profilePictureImageView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String[] editPhoto;
                if(photoUrl==null){
                    editPhoto = new String[]{"Camera", "Gallery"};
                }else{
                    editPhoto = new String[]{"Camera", "Gallery", "Delete Photo"};
                }

                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

                dialog.setItems(editPhoto, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        if (id == 0){
                            // Call camera Intent
                            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(takePhotoIntent, 0);
                        }

                        if (id == 1){
                            // Call gallery Intent
                            Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            //                        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                            //                        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
                            //                        galleryIntent.setType("image/*");
                            //                        galleryIntent.putExtra("return-data", true);
                            //                        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(galleryIntent, 1);
                        }
                        if (id == 2){
                            // Call delete
                            deletePhoto();
                        }
                    }
                });
                dialog.setNeutralButton("cancel",new android.content.DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();               
                    }});
                dialog.show(); 
            }
        });


        changePasswordButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
            }
        });


        return view;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + ".onActivityResult " + requestCode + " " + resultCode + " " + intent);
        }
        switch(requestCode){
            case 0:                
            case 1:
                if (intent != null &&   resultCode == Activity.RESULT_OK && (intent.getData() != null || intent.hasExtra("data"))) {
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
                        startActivityForResult(cropIntent, 3);
                    } catch(ActivityNotFoundException anfe) {
                        processPhoto(intent);
                    }
                }
                break;
            case 2:
                deletePhoto();
                break;
            case 3:
                if (resultCode == Activity.RESULT_CANCELED && originIntent != null && (originIntent.getData() != null || originIntent.hasExtra("data"))) {
                    intent = originIntent;
                }  
                processPhoto(intent);
                break;
        } 
    }


    private void processPhoto(Intent intent) {
        getActivity().findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);
        Uri uri = intent.getData();
        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + ".onActivityResult " + uri);
        }
        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        File file = new File(MainApplication.getContactStorageDirectory(), fileName + ".jpg");
        String filePath = file.getAbsolutePath();

        //
        // Copy the photo to YouChat private dir...
        //
        if (uri != null) {
            try {               
                InputStream in = new BufferedInputStream(getActivity().getContentResolver().openInputStream(uri));
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

        BitmapFactory.Options opts = new BitmapFactory.Options();
        try {
            opts.inSampleSize = Helper.getSampleSize(filePath, 400);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final Bitmap bmap = Helper.rotateImage(filePath, BitmapFactory.decodeFile(filePath, opts));
        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + " photo size " + bmap.getWidth() + "x" + bmap.getHeight());            
        }

        new UploadPictureAsyncTask(acctId) {
            @Override
            protected void onPostExecute(Object result) {
                if (result instanceof JSONObject) {
                    photoUrl = ((JSONObject) result).optString("return_picture_url");
                    profilePictureImageView.setImageBitmap(MainApplication.createRoundedBitmap(bmap));
                    saveAccount();
                    bmap.recycle();
                } else {
                    Toast.makeText(getActivity(), R.string.failed_upload_photo, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(bmap);
    }


    private void saveAccount() {
        final String first = firstNameEditText.getText().toString();
        final String last = lastNameEditText.getText().toString();
        final String company = companyNameEditText.getText().toString();
        if (first == null || first.length() < 1 || last == null || last.length() < 1){
            new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.error_profile_required_fields)).create().show();
            return;
        }
        getActivity().findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);
        new UpdateAccountAsyncTask() {
            @Override
            protected void onPostExecute(Object obj) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().findViewById(R.id.progressbar_container).setVisibility(View.GONE);
                if (obj instanceof Exception) {
                    Toast.makeText(getActivity(), "Error updating account.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Account updated.", Toast.LENGTH_SHORT).show();
                    getActivity().getActionBar().setSubtitle(firstNameEditText.getText().toString() + " " + lastNameEditText.getText().toString());
                    /*
                    if (ContactDAO.getInstance().getAccountUser() != null) {
                        ContactDAO.getInstance().getAccountUser().setFirstName(first);
                        ContactDAO.getInstance().getAccountUser().setLastName(last);
                        ContactDAO.getInstance().getAccountUser().setCompany(company);

                        if(photoUrl != null && photoUrl.length() > 0) {    
                            ContactDAO.getInstance().getAccountUser().setPhotoUri(Uri.parse(photoUrl));
                        }
                    } 
                    */
                }
            }
        }.execute(acctId, first, last, company, photoUrl);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.settings_menu, menu);
        if (Config.DEBUG) {
            Log.d(TAG, "SettingsFragment:onCreateOptionsMenu");
        }
    }

    public void deletePhoto(){
        photoUrl = "";
        saveAccount();
        profilePictureImageView.setImageResource(R.drawable.contact_image_placeholder);
    }
} 
