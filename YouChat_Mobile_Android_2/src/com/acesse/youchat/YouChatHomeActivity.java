package com.acesse.youchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.yml.youchatlib.SessionManager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager.BackStackEntry;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;



public class YouChatHomeActivity extends Activity implements ActionBar.TabListener {


    private static final String TAG = "YOUC";
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

    //protected YouChatService service;

    private Handler handler = new Handler();
    private Fragment mFragment;
    private TextView numUnreadTextView;
    private BroadcastReceiver netReceiver;
    private String acctId;
    private SharedPreferences prefs;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        acctId = SessionManager.getInstance().getUserHandle();

        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + ".onCreate() " + acctId);
        }

        if (acctId.isEmpty()) {
            if (Config.DEBUG) {
                Log.e(TAG, "Account ID is null", new Exception());
            }
            finishAffinity();
            MainApplication.restartApp();
            return;
        }

        //
        // YC-838: Fragment restoration weird state... restart the app...
        //
        /*
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc == null || ContactDAO.getInstance().getAccountUser() == null) {
            if (Config.DEBUG) {
                Log.w(TAG, "LinphoneCore is null or account contact is null");
            }
            finishAffinity();
            MainApplication.restartApp();
            return;
        }
        */

        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(ContactDAO.getInstance().getAccountFullName());
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.tab_contact_states).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setCustomView(R.layout.chat_tab).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.acesse_tab_states).setTabListener(this), false);
        actionBar.addTab(actionBar.newTab().setIcon(R.drawable.tab_settings_states).setTabListener(this), false);


        numUnreadTextView = (TextView) actionBar.getTabAt(1).getCustomView().findViewById(R.id.num_unread_text);

        if (getIntent().hasExtra("chat")) {
            actionBar.setSelectedNavigationItem(1);
            String name = getIntent().getStringExtra("contact");
            if (Config.DEBUG) {
                Log.d(TAG, "CHAT CONTACT " + name);
            }
            /*
            final Contact contact = ContactDAO.getInstance().getContact(name);
            if (contact == null) {
                if (!name.equalsIgnoreCase("registrar")) {
                    showUnknownContactDialog(name);
                }
            } else {
                //
                // ensure enough delay to allow chatlist tab to be selected and service to be bound...
                //
                handler.postDelayed(new Runnable() {
                    public void run() {
                        ChatListFragment chatListFragment = (ChatListFragment) getFragmentManager().findFragmentByTag(ChatListFragment.class.getSimpleName());
                        if (chatListFragment != null) {
                            chatListFragment.replaceFragment(contact);
                        }
                    }
                }, 50);
            }
            */
        } else {
            actionBar.setSelectedNavigationItem(0);
        }

        /*
        // for config change...
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment chatListFragment = getFragmentManager().findFragmentByTag(ChatListFragment.class.getSimpleName());
            if (chatListFragment != null) {
                ft.hide(chatListFragment);
            }
            Fragment contactListFragment = getFragmentManager().findFragmentByTag(ContactListFragment.class.getSimpleName());
            if (contactListFragment != null) {
                ft.hide(contactListFragment);
            }
            Fragment settingsFragment = getFragmentManager().findFragmentByTag(SettingsFragment.class.getSimpleName());
            if (settingsFragment != null) {
                ft.hide(settingsFragment);
            }
            ft.commit();
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        } else {
            actionBar.setSelectedNavigationItem(0);
        }
         */

        getFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {    
            public void onBackStackChanged() {
                invalidateOptionsMenu();
            }
        });
    }


    @Override
    public void onDestroy() {
        if (Config.DEBUG) {
            Log.d(TAG, "Home.onDestroy()");
        }
        super.onDestroy();
        MainApplication.getBitmapCache().evictAll();
    }


    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (Config.DEBUG) {
            final String cmd = intent.getStringExtra("cmd");
            final String name = intent.getStringExtra("name");
            if (cmd != null && cmd.equals("add.contact")) {
                getActionBar().setSelectedNavigationItem(0);
                final ContactListFragment f1 = (ContactListFragment) mFragment;
                //f1.replaceFragment(new Contact(null, ""));
                handler.postDelayed(new Runnable() {
                    public void run() {
                        ((EditText) f1.contactFragment.getView().findViewById(R.id.username_text)).setText(name);
                        f1.contactFragment.findAccount();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                f1.contactFragment.getView().findViewById(R.id.contact_action_button).performClick();
                            }
                        }, 9000);
                    }
                }, 900);
            } else if (cmd != null && cmd.startsWith("delete.contact")) {
                getActionBar().setSelectedNavigationItem(0);
                /*
                ContactDAO.getInstance().removeContact(ContactDAO.getInstance().getContact(name));
                ((ArrayAdapter) ((ContactListFragment) mFragment).getListView().getAdapter()).notifyDataSetChanged();
                */
            } else if (cmd != null && cmd.startsWith("chat.")) {
                getActionBar().setSelectedNavigationItem(1);
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (mFragment instanceof ChatListFragment) {
                            final ChatListFragment f2 = (ChatListFragment) mFragment;
                            Runnable runner = new Runnable() {
                                public void run() {
                                    String item = cmd.substring(5);
                                    String n = intent.hasExtra("count") ? intent.getStringExtra("count") : "5";
                                    f2.chatFragment.startAutomation(item, Integer.parseInt(n));
                                }
                            };
                            /*
                            if (f2.chatFragment != null && f2.chatFragment.isVisible() && f2.chatFragment.toContact.getName().equals(name)) {
                                runner.run();
                            } else {
                                //f2.replaceFragment(ContactDAO.getInstance().getContact(name));
                                handler.postDelayed(runner, 2000);
                            }
                            */
                        } else {
                            Log.d(TAG, "Unable to start chat test because the app is not on the Chat List tab.");
                        }
                    }
                }, 999);
            } else if (cmd != null && cmd.startsWith("delete.session")) {
                getActionBar().setSelectedNavigationItem(1);
                handler.postDelayed(new Runnable() {
                    public void run() {
                        ((ChatListFragment) mFragment).deleteSession(name);
                    }
                }, 2000);
            } else if (cmd != null && cmd.equals("logout")) {
                //stopService(new Intent(this, YouChatService.class));
                finishAffinity();
            }
        }
    }


    /*
    public YouChatService getService() {
        return service;
    }
    */


    public void updateUnreadText() {
        int numUnread = 0;
        /*
        List<LinphoneChatRoom> rooms = new ArrayList<LinphoneChatRoom>(Arrays.asList(LinphoneManager.getLc().getChatRooms()));
        List<Contact> list = new ArrayList<Contact>(ContactDAO.getInstance().getContacts());
        list.add(ContactDAO.getInstance().getRegistrarContact());
        for (Contact contact : list) {
            String name = contact.getName();
            for (LinphoneChatRoom room : rooms) {
                if (room.getPeerAddress().getUserName().equals(name)) {
                    //numUnread += room.getUnreadMessagesCount();
                    //shared room case, have to iterate over all messages or the last 25...
                    for (LinphoneChatMessage msg : room.getHistory(25)) {
                        if (msg.getFrom().getUserName().equals(acctId) || msg.getTo().getUserName().equals(acctId)) {
                            if (!msg.isRead()) {
                                numUnread++;
                            }
                        }
                    }
                    rooms.remove(room);
                    break;
                }
            }
        }
        */
        if (numUnread > 0) {
            numUnreadTextView.setText(String.valueOf(numUnread));
            numUnreadTextView.setVisibility(View.VISIBLE);
        } else {
            numUnreadTextView.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onBackPressed() {
        if (Config.DEBUG) {
            Log.d(TAG, "Home.onBackPressed");
        }
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            int selectedIndex = getActionBar().getSelectedNavigationIndex();
            BackStackEntry entry = getFragmentManager().getBackStackEntryAt(getFragmentManager().getBackStackEntryCount()-1);
            if (Config.DEBUG) {
                Log.d(TAG, "Back Stack: " + entry.getName());
            }
            Fragment f = getFragmentManager().findFragmentByTag(entry.getName());
            if (Config.DEBUG) {
                Log.d(TAG, "Fragment: " + f);
            }
            if (f != null) {
                getFragmentManager().beginTransaction().hide(f).commit();
            }
            getFragmentManager().popBackStack();
            f = null;
            if (selectedIndex == 0) {
                f = getFragmentManager().findFragmentByTag(ContactListFragment.class.getSimpleName());
            } else if (selectedIndex == 1) {
                f = getFragmentManager().findFragmentByTag(ChatListFragment.class.getSimpleName());
            }
            if (f != null) {
                //WTF no hidden state change
                //getFragmentManager().beginTransaction().show(f).commit();
                final Fragment frag = f;
                handler.postDelayed(new Runnable() {
                    public void run() {
                        frag.onHiddenChanged(false);
                    }
                }, 5);
            }
        } else {
            super.onBackPressed();
        }
    }



    @Override
    public void onStart() {
        if (Config.DEBUG) {
            Log.d(TAG, "Home.onStart()");
        }
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction("MESSAGE.RCVD");
        filter.addAction("MESSAGE.READ");
        MainApplication.registerLocalReceiver(mReceiver, filter);

        updateUnreadText();

        registerReceiver(netReceiver = new BroadcastReceiver() {      
            public void onReceive(Context context, Intent intent) {
                findViewById(R.id.network_unreachable_text).setVisibility(MainApplication.hasNetworkConnectivity() ? View.GONE : View.VISIBLE);
                
                if (!MainApplication.hasNetworkConnectivity()){
                    if (mFragment instanceof ChatListFragment) {
                        ((ChatListFragment) mFragment).changeStatus();
                    }else if(mFragment instanceof ContactListFragment){
                        ((ContactListFragment) mFragment).changeStatus();
                    }                    
                }
                //                DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("network.dialog");
                //                if (f != null && f.getDialog().isShowing() && hasConnectivity) {
                //                    f.getDialog().hide();
                //                } else if (!hasConnectivity) {
                //                    showNetworkUnavailableDialog();
                //                }
            }
        }, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }


    @Override
    public void onStop() {
        if (Config.DEBUG) {
            Log.d(TAG, "Home.onStop()");
        }
        super.onStop();
        MainApplication.unregisterLocalReceiver(mReceiver);
        try {
            unregisterReceiver(netReceiver);
        } catch (Exception ex) {
        }
    }


    @Override
    public void onResume() {
        if (Config.DEBUG) {
            Log.d(TAG, "Home.onResume()");
        }
        super.onResume();
        //bindService(new Intent(this, YouChatService.class), mConnection, Context.BIND_AUTO_CREATE);

        findViewById(R.id.network_unreachable_text).setVisibility(MainApplication.hasNetworkConnectivity() ? View.GONE : View.VISIBLE);
        //        if (!MainApplication.hasNetworkConnectivity()){
        //            showNetworkUnavailableDialog();
        //        }
        registerReceiver(netReceiver = new BroadcastReceiver() {      
            public void onReceive(Context context, Intent intent) {
                findViewById(R.id.network_unreachable_text).setVisibility(MainApplication.hasNetworkConnectivity() ? View.GONE : View.VISIBLE);
                
                if (!MainApplication.hasNetworkConnectivity()){
                    if (mFragment instanceof ChatListFragment) {
                        ((ChatListFragment) mFragment).changeStatus();
                    }else if(mFragment instanceof ContactListFragment){
                        ((ContactListFragment) mFragment).changeStatus();
                    }                    
                }
                //                DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("network.dialog");
                //                if (f != null && f.getDialog().isShowing() && hasConnectivity) {
                //                    f.getDialog().hide();
                //                } else if (!hasConnectivity) {
                //                    showNetworkUnavailableDialog();
                //                }
            }
        }, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    
    }


    @Override
    public void onPause() {
        if (Config.DEBUG) {
            Log.d(TAG, "Home.onPause()");
        }
        super.onPause();
        //unbindService(mConnection);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //
        // Because we're currently portrait only we need not save state...
        //
        //outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
        //super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.you_chat_main, menu);
        if (Config.DEBUG) {
            menu.add("DEBUG: Cause GC");
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                prefs.edit().remove("pass").commit();
                //service.goOffline();
                //
                // delay to allow presence info to be sent...
                //
                handler.postDelayed(new Runnable() {
                    public void run() {
                        stopService(new Intent(MainApplication.getInstance(), YouChatService.class));
                        finishAffinity();
                        startActivity(new Intent(getApplicationContext(), YouChatLoginActivity.class).putExtra("logout", true));
                    }
                }, 100);
                break;
            default:
                if (Config.DEBUG) {
                    if (item.getTitle() != null && item.getTitle().toString().endsWith("GC")) {
                        System.gc();
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    } 


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + ".onActivityResult");
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }


    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction transaction) {
        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + ".onTabSelected");
        }

        int position = tab.getPosition();
        if (position == 0) {
            mFragment = getFragmentManager().findFragmentByTag(ContactListFragment.class.getSimpleName());
            if (mFragment == null) {
                transaction.add(R.id.fragmentContainer, mFragment = ContactListFragment.newInstance(acctId), ContactListFragment.class.getSimpleName());
            } else {
                transaction.show(mFragment);
            }
        } else if (position == 1) {
            mFragment = getFragmentManager().findFragmentByTag(ChatListFragment.class.getSimpleName());
            if (mFragment == null) {
                transaction.add(R.id.fragmentContainer, mFragment = ChatListFragment.newInstance(acctId), ChatListFragment.class.getSimpleName());
            } else {
                transaction.show(mFragment);
            }
        } else if (position == 2) {
            mFragment = getFragmentManager().findFragmentByTag(AcesseTabFragment.class.getSimpleName());
            if(mFragment == null) {
                transaction.add(R.id.fragmentContainer, mFragment = new AcesseTabFragment(), AcesseTabFragment.class.getSimpleName());
            } else {
                transaction.show(mFragment);
            }
        } else {
            mFragment = getFragmentManager().findFragmentByTag(SettingsFragment.class.getSimpleName());
            if (mFragment == null) {
                transaction.add(R.id.fragmentContainer, mFragment = SettingsFragment.newInstance(acctId), SettingsFragment.class.getSimpleName());
            } else {
                transaction.show(mFragment);
            }
        }

        numUnreadTextView.setTextColor(getResources().getColor(position == 1 ? R.color.acesse_lightblue : R.color.black));
        invalidateOptionsMenu();
    }


    private void showUnknownContactDialog(final String user) {
        new AlertDialog.Builder(YouChatHomeActivity.this)
        .setMessage(getString(R.string.unknown_user_message, user))
        .setTitle(R.string.alert_text)
        .setIcon(R.drawable.ic_error_icon)
        .setPositiveButton(R.string.alert_add_contact, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                new GetAccountAsyncTask() {
                    @Override
                    protected void onPostExecute(Object result) {
                        /*
                        if (result instanceof Contact) {
                            final Contact contact = (Contact) result;
                            ContactDAO.getInstance().addContact(contact);
                            Toast.makeText(YouChatHomeActivity.this, getResources().getString(R.string.added_contact_message, user), Toast.LENGTH_SHORT).show();
                            if (getActionBar().getSelectedNavigationIndex() == 0 && mFragment instanceof ContactListFragment) {
                                ((ArrayAdapter) ((ListFragment) mFragment).getListView().getAdapter()).notifyDataSetChanged();
                                ((ContactListFragment) mFragment).replaceFragment(contact);
                            } else {
                                getActionBar().setSelectedNavigationItem(0);
                                handler.postDelayed(new Runnable() {
                                    public void run() {
                                        ((ContactListFragment) mFragment).replaceFragment(contact);
                                    }
                                }, 50);
                            }
                        } else if (result instanceof Exception) {
                            Toast.makeText(YouChatHomeActivity.this, getResources().getString(R.string.failed_adding_contact, user), Toast.LENGTH_SHORT).show();
                        }
                        */
                    }
                }.execute(user.toLowerCase());
            }
        })
        .setNegativeButton(getResources().getString(R.string.alert_dialog_cancel), new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
            }
        }).show();
    }


    //    public void showNetworkUnavailableDialog() {
    //        DialogFragment f = (DialogFragment) getFragmentManager().findFragmentByTag("network.dialog");
    //        if (f == null || !f.getDialog().isShowing()) {
    //            FragmentTransaction ft = getFragmentManager().beginTransaction();
    //            f = NetworkAlertDialogFragment.newInstance();
    //            f.show(ft, "network.dialog");
    //        }
    //    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (!isFinishing()) {
                        updateUnreadText();
                    }
                }
            }, 500);
        }
    };


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


    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction transaction) {
        if (Config.DEBUG) {
            Log.d(TAG, getClass().getSimpleName() + ".onTabUnselected " + mFragment.toString());
        }
        if (mFragment != null) {
            View view = mFragment.getView();
            transaction.hide(mFragment);
            if (view != null) {
                view = view.findFocus();
                if (view != null && Helper.isSoftKeyboardVisible(YouChatHomeActivity.this)) {
                    Helper.hideSoftKeyboard(YouChatHomeActivity.this, view.getWindowToken());
                }
            }
        }
    }


    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        View view = mFragment.getView().findFocus();
        if (view != null && Helper.isSoftKeyboardVisible(YouChatHomeActivity.this)) {
            Helper.hideSoftKeyboard(YouChatHomeActivity.this, view.getWindowToken());
        }
    }
}