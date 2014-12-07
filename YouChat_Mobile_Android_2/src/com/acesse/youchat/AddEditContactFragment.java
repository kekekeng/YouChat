package com.acesse.youchat;

import com.yml.youchatlib.models.User;

import com.crittercism.app.Crittercism;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * 
 */
public class AddEditContactFragment extends Fragment implements OnClickListener {

    private static final String TAG = "YOUC";

    protected User user;
    private boolean isEditMode = false;
    private SharedPreferences preferences;
    private String acctId;
    private Handler handler = new Handler();


    public static AddEditContactFragment newInstance(User user, String acctId) {
        AddEditContactFragment fragment = new AddEditContactFragment();
        fragment.user = user;
        //fragment.isEditMode = user.getID() != null;
        fragment.acctId = acctId;
        return fragment;
    }


    public AddEditContactFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Crittercism.leaveBreadcrumb("AddEditContactFragment: onCreateView");

        if (Config.DEBUG) {
            Log.d(TAG,"AddEditContactFragment.onCreateView");
        }

        //
        // YC-838: Fragment restoration weird state... don't create any views and hope home activity exits...
        //
        /*
        if (ContactDAO.getInstance().getAccountUser() == null) {
            return new TextView(getActivity());
        }
        */

        final View view = inflater.inflate(R.layout.fragment_add_contact, container,false);
        final TextView addButton = (TextView) view.findViewById(R.id.contact_action_button);
        addButton.setOnClickListener(this);

        if (isEditMode) {
            addButton.setBackgroundResource(R.color.acesse_darkblue);
            addButton.setText(R.string.action_chat_now);
            //Populate the data from contact
            /*
            if (contact != null){
                ((EditText) view.findViewById(R.id.username_text)).setText(contact.getName());
                ((EditText) view.findViewById(R.id.username_text)).setEnabled(false);
                ((TextView) view.findViewById(R.id.name_text)).setText(contact.getFirstName() + " " + contact.getLastName());
                ((TextView) view.findViewById(R.id.company_text)).setText(contact.getCompany());
                Bitmap bitmap = ContactDAO.getInstance().getProfilePhoto(contact.getPhotoUri());
                ((ImageView) view.findViewById(R.id.picture)).setImageBitmap(bitmap);
            }
            */
        } else {
            /*
            ((ImageView) view.findViewById(R.id.picture)).setImageBitmap(ContactDAO.getInstance().getPlaceholderBitmap());
            addButton.setBackgroundResource(R.drawable.acesse_button);
            view.findViewById(R.id.contact_details_container).setAlpha(0);
            addButton.setText(R.string.action_find);
            ((EditText) view.findViewById(R.id.username_text)).setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE && getActivity() != null) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        addButton.setText(R.string.action_find );
                        clearPreviousData();
                        findAccount();
                        return true;
                    }
                    return false;
                }
            });
            */
        }
        return view;
    }

    private void showErrorMessage(int resId) {
        // This is the fix for YC-1010 : Contact finder need consistent error handling on invalid or non-existing YouChat ID
        // TextView errorView = (TextView) getView().findViewById(R.id.error_contact_text);
        // errorView.setText(resId);
        // errorView.setVisibility(View.VISIBLE);

        // This is the fix for YC-1010 : Contact finder need consistent error handling on invalid or non-existing YouChat ID
        showMessageDialog(getResources().getString(R.string.error), getResources().getString(resId));
    }

    private void showErrorMessage(int resId, String youChatID) {
        // This is the fix for YC-1010 : Contact finder need consistent error handling on invalid or non-existing YouChat ID
        showMessageDialog(getResources().getString(R.string.error), getString(resId, youChatID));
    }

    protected void findAccount() {
        if (!MainApplication.hasNetworkConnectivity()) {
            MainApplication.showNetworkUnavailableDialog(getFragmentManager());
            return;
        }
        final String account = ((TextView) getView().findViewById(R.id.username_text)).getText().toString().trim();
        /*
        if (ContactDAO.getInstance().getContact(account) != null) {
            getActivity().onBackPressed();
        } else if (account == null || account.isEmpty()) {
            showErrorMessage(R.string.error_invalid_username2, account);
        } else if(account.length() <3) {
            showMessageDialog(getResources().getString(R.string.error), "The YouChat ID \"" + account + "\" is invalid. Please enter a valid YouChat ID." );
        } else if (account.equals(preferences.getString("user", "")) && !Config.DEBUG) {
            showErrorMessage(R.string.error_contact_is_user);
        } else {
            getView().findViewById(R.id.error_contact_text).setVisibility(View.GONE);
            getView().findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);
            getView().findViewById(R.id.contact_details_container).setAlpha(0);
            new GetAccountAsyncTask() {
                @Override
                protected void onPostExecute(Object result) {
                    if (getActivity() == null || getView() == null) {
                        return;
                    }
                    getView().findViewById(R.id.progressbar_container).setVisibility(View.GONE);
                    if (result instanceof Exception) {
                        if (result instanceof YouChatException && ((YouChatException) result).getCode() == 404) {
                            showErrorMessage(R.string.error_invalid_username2, account);
                        } else {
                            if(((Exception) result).getMessage() != null && ((Exception) result).getMessage().length() > 0)
                            {	
                                showMessageDialog(getResources().getString(R.string.error), ((Exception) result).getMessage());
                            }
                            else
                            {
                                showMessageDialog(getString(R.string.error), getString(R.string.error_invalid_username2, account));
                            }	
                        }
                    } else if (result instanceof Contact) {
                        Crittercism.leaveBreadcrumb("AddEditContactFragment: Contact Found. Populating Data.");
                        contact = (Contact) result;
                        ((TextView) getView().findViewById(R.id.name_text)).setText(contact.getFirstName() + " " + contact.getLastName());
                        ((TextView) getView().findViewById(R.id.company_text)).setText(contact.getCompany());
                        ((TextView) getView().findViewById(R.id.contact_action_button)).setText(getResources().getText(R.string.action_add));
                        Bitmap bitmap = ContactDAO.getInstance().getProfilePhoto(contact.getPhotoUri());
                        ((ImageView) getView().findViewById(R.id.picture)).setImageBitmap(bitmap);
                        getView().findViewById(R.id.contact_details_container).animate().alpha(1f).setDuration(800).setListener(null);

                        EditText userName = (EditText) getView().findViewById(R.id.username_text);
                        userName.setEnabled(false);
                        userName.setKeyListener(null);
                    }
                }
            }.execute(account.toLowerCase());
        }
        */
    }


    private void clearPreviousData(){
        ((TextView) getView().findViewById(R.id.name_text)).setText(null);
        ((TextView) getView().findViewById(R.id.company_text)).setText(null);
        //((ImageView) getView().findViewById(R.id.picture)).setImageBitmap(ContactDAO.getInstance().getPlaceholderBitmap());
    }


    private void showMessageDialog(String title, String message) {
        new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message)
        .setNegativeButton(getResources().getString(R.string.ok),new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        }).setIcon(R.drawable.ic_error_icon).create().show();
    }


    @Override
    public void onClick(View v) {
        /*
        switch(v.getId()){
            case R.id.contact_action_button:
                if (Config.DEBUG) {
                    Log.d(TAG,"AddEditContactFragment.addContactButton clicked");
                }
                String txt = ((TextView) v).getText().toString();
                if (txt.equals(getResources().getString(R.string.action_add))) {
                    boolean found = false;
                    for (Contact c : ContactDAO.getInstance().getContacts()) {
                        if (c.getName().equalsIgnoreCase(contact.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        showMessageDialog(getResources().getString(R.string.oops_title), getResources().getString(R.string.already_contact));
                    } else {
                        ContactDAO.getInstance().addContact(contact);
                        getActivity().onBackPressed();

                        String deletedSessions = preferences.getString(acctId + "_deleted_chat_sessions", "");
                        if (deletedSessions.indexOf(contact.getName()) != -1) {
                            deletedSessions = deletedSessions.replace(contact.getName(), "");
                            preferences.edit().putString(acctId + "_deleted_chat_sessions", deletedSessions).commit();
                        }
                    }
                } else if (txt.equals(getResources().getString(R.string.action_find))) {
                    findAccount();
                } else if (txt.equals(getResources().getString(R.string.action_chat_now))) {
                    final String tag = ChatListFragment.class.getSimpleName();
                    final FragmentManager fmanager = getFragmentManager();
                    getActivity().onBackPressed();
                    getActivity().getActionBar().setSelectedNavigationItem(1);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            ChatListFragment f = (ChatListFragment) fmanager.findFragmentByTag(tag);
                            if (f != null) {
                                f.replaceFragment(contact);
                            }
                        }
                    }, fmanager.findFragmentByTag(tag) == null ? 30 : 0);
                }
                break;
            default:
                break;
        }
        */
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Config.DEBUG) {
            Log.d(TAG,"AddEditContactFragment: onCreateOptionsMenu");
        }
        inflater.inflate(R.menu.add_contact_menu, menu);
        MenuItem item = menu.findItem(R.id.menu_delete_contact);
        if (item != null) {
            item.setVisible(!hidden && isEditMode);
        }
        item = menu.findItem(R.id.menu_add_contact);
        if (item != null) {
            item.setVisible(false);
        }
        item = menu.findItem(R.id.menu_refresh_contacts);
        if (item != null) {
            item.setVisible(false);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        switch (item.getItemId()) {
            case R.id.menu_delete_contact: {
                new AlertDialog.Builder(getActivity()).setMessage(getResources().getString(R.string.message_delete)).setTitle(getResources().getString(R.string.confirm_delete))
                .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getActivity().findViewById(R.id.progressbar_container).setVisibility(View.VISIBLE);
                        ContactDAO.getInstance().removeContact(contact);
                        getActivity().onBackPressed();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton){
                        getActivity().onBackPressed();
                    }
                }).show();
                break;
            }
            default:
                break;
        }
        */
        return super.onOptionsItemSelected(item);
    } 


    //
    // STUPID NESTED FRAGMENTS - MUST MAINTAIN OUR OWN HIDDEN STATE
    //
    private boolean hidden = false;


    @Override
    public void onHiddenChanged(boolean hidden) {
        if (Config.DEBUG) {
            Log.d(TAG, "AddEditContactFragment.onHiddenChanged(" + hidden + ")");
        }
        super.onHiddenChanged(this.hidden = hidden);
        getActivity().invalidateOptionsMenu();
    }

}
