package com.acesse.youchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.crittercism.app.Crittercism;

import com.yml.youchatlib.models.User;



public class ChatListFragment extends ListFragment {


    private static final String TAG = "YOUC";

    protected ChatFragment chatFragment;

    private Handler handler = new Handler();
    private final List<User> mList = new ArrayList<User>();
    private SharedPreferences preferences;
    private String acctId;
    private BroadcastReceiver mReceiver;


    /**
     * Use this factory method to create a new instance of this fragment using
     * the provided parameters.
     */
    public static ChatListFragment newInstance(String acctId) {
        ChatListFragment fragment = new ChatListFragment();
        fragment.acctId = acctId;
        return fragment;
    }


    public ChatListFragment() {
    }


    public void changeStatus(){
        /*
        for (Contact c : mList){
            c.getFriend().getPresenceModel().setBasicStatus(PresenceBasicStatus.Closed);            
        }
         */
        ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Crittercism.leaveBreadcrumb("ChatListFragment : onCreate");
        if (Config.DEBUG) {
            Log.d(TAG,"ChatListFragment.onCreate()");
        }
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Crittercism.leaveBreadcrumb("ChatListFragment : onCreateView");
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onCreateView()");
        }

        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        //
        // YC-838: Fragment restoration weird state... return empty listview and hope home activity exits...
        //
        /*
        if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() == null || ContactDAO.getInstance().getAccountUser() == null) {
            return view;
        }
         */

        ArrayAdapter<User> adapter = new ArrayAdapter<User>(getActivity(), android.R.layout.simple_list_item_1, mList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                //
                // TODO: performance item, convertView consideration...
                //
                final User user = mList.get(position);
                final View view = LayoutInflater.from(getActivity()).inflate(R.layout.chat_list_row_item, parent, false);
                view.setTag(user);
                /*
                String name = contact.getFirstName() + " " + contact.getLastName();
                name = name.length() == 1 ? contact.getName() : name + " <small>(" + contact.getName() + ")</small>";
                ((TextView) view.findViewById(R.id.name_text)).setText(Html.fromHtml(name));

                LinphoneFriend friend = contact.getFriend();
                if (friend != null && friend.getPresenceModel() != null && friend.getPresenceModel().getBasicStatus() == PresenceBasicStatus.Open) {
                    ((ImageView) view.findViewById(R.id.presence)).setImageLevel(1);
                }

                new AsyncTask<Void, Void, Long[]>() {
                    @Override
                    protected Long[] doInBackground(Void... arg0) {
                        return getChatRoomInfo(contact);
                    }
                    @Override
                    protected void onPostExecute(Long[] result) {
                        updateTextFields(view, result[0], result[1]);
                    }
                }.execute();

                ((ImageView) view.findViewById(R.id.button_photo)).setImageBitmap(ContactDAO.getInstance().getProfilePhoto(contact.getPhotoUri()));
                 */
                return view;
            }
        };
        setListAdapter(adapter);

        //        if (savedInstanceState != null && savedInstanceState.containsKey("contact")) {
        //            String savedContactName = savedInstanceState.getString("contact");
        //            Fragment fragment = getFragmentManager().findFragmentByTag(ChatFragment.class.getSimpleName());
        //            if (fragment != null) {
        //                for (ContactWrapper contact : list) {
        //                    if (savedContactName.equals(contact.getName())) {
        //                        ((ChatFragment) fragment).contact = contact.contact;
        //                        break;
        //                    }
        //                }
        //            }
        //        }

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onActivityCreated()");
        }
        super.onActivityCreated(savedInstanceState);
        getListView().setLayoutAnimation(new LayoutAnimationController(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_top_to_bottom)));
    }


    @Override
    public void onStart() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onStart()");
        }
        super.onStart();
        addReceiver();
        populateList();
    }


    @Override
    public void onStop() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onStop()");
        }
        super.onStop();
        MainApplication.unregisterLocalReceiver(mReceiver);
        mReceiver = null;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onResume()");
        }
        startTimer();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onPause()");
        }
        stopTimer();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
        //        Fragment fragment = getFragmentManager().findFragmentByTag(ChatFragment.class.getSimpleName());
        //        if (fragment != null && fragment.isAdded()) {
        //            outState.putString("contact", ((ChatFragment) fragment).contact.getName());
        //        }
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onListItem clicked");
        }
        replaceFragment((User) getListAdapter().getItem(position));
    }


    /*
    private Long[] getChatRoomInfo(Contact contact) {
        String to = "sip:" + contact.getName() + "@" + Config.DOMAIN_XMPP;
        LinphoneChatRoom room = LinphoneManager.getLc().getOrCreateChatRoom(to);

        //int unreadCount = room.getUnreadMessagesCount();
        //LinphoneChatMessage[] msgs = room.getHistory(1);
        //updateTimeStamp((TextView) view.findViewById(R.id.date_text), msgs.length == 1 ? msgs[0].getTime() : 0);

        //
        // Loop through messages to handle shared room case...
        //
        long lastTime = 0;
        long unreadCount = 0;
        for (LinphoneChatMessage msg : room.getHistory(25)) {
            if (msg.getFrom().getUserName().equals(acctId) || msg.getTo().getUserName().equals(acctId)) {
                lastTime = Math.max(lastTime, msg.getTime());
                if (!msg.isRead()) {
                    unreadCount++;
                }
            }
        }
        return new Long[] { unreadCount, lastTime };
    }
     */


    private synchronized void updateTextFields(View view, long unreadCount, long lastTime) {
        TextView msgText = (TextView) view.findViewById(R.id.message_text);
        msgText.setText(unreadCount > 0 ? unreadCount + " unread message" + (unreadCount > 1 ? "s" : "") : "");
        //Helper.updateTimeStamp((TextView) view.findViewById(R.id.date_text), lastTime);
    }


    private void addReceiver() {
        if (mReceiver != null) {
            MainApplication.unregisterLocalReceiver(mReceiver);
            mReceiver = null;
        }
        mReceiver = getReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("PRESENCE");
        filter.addAction("MESSAGE.RCVD");
        MainApplication.registerLocalReceiver(mReceiver, filter);
    }


    private BroadcastReceiver getReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Config.DEBUG) {
                    Log.d(TAG, "ChatListFragment.onReceive " + action);
                }
                if (action.equals("MESSAGE.RCVD")) {
                    String from = intent.getStringExtra("from");
                    if (getListView() != null && from != null) {
                        for (int i = getListView().getChildCount(); --i >= 0; ) {
                            View view = getListView().getChildAt(i);
                            if (view != null) {
                                /*
                                User user = (User) view.getTag();
                                if (contact != null && contact.getName().equalsIgnoreCase(from)) {
                                    Long info[] = getChatRoomInfo(contact);
                                    updateTextFields(view, info[0], info[1]);
                                    startTimer();
                                    break;
                                }
                                 */
                            }
                        }
                    }
                } else if (action.equals("PRESENCE")) {
                    // remove/delay to handle presence thundering herd...
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            ((ArrayAdapter) getListView().getAdapter()).notifyDataSetChanged();
                        }
                    }, 200);
                }
            }
        };
    }


    private void populateList() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.populateList");
        }

        /*
        List<String> deletedList = Arrays.asList(preferences.getString(acctId + "_deleted_chat_sessions", "").split(","));

        mList.clear();
        Contact registrarContact = ContactDAO.getInstance().getRegistrarContact();
        List<Contact> contacts = new ArrayList<Contact>(ContactDAO.getInstance().getContacts());
        for (LinphoneChatRoom room : LinphoneManager.getLc().getChatRooms()) {
            String roomUser = room.getPeerAddress().getUserName();
            if (deletedList.contains(roomUser)) {
                continue;
            }
            if (Config.DEBUG) {
                Log.d(TAG, "ROOM: " + roomUser);
            }
            boolean found = false;
            for (Contact contact : contacts) {
                if (roomUser.equals(contact.getName())) {
                    contacts.remove(contact);
                    mList.add(contact);
                    found = true;
                    break;
                }
            }
            if (!found && roomUser.equalsIgnoreCase("registrar") && !mList.contains(registrarContact)) {
                mList.add(registrarContact);
            }
        }

        //
        // Check the registrar contact for messages 
        //
        if (mList.contains(registrarContact)) {
            LinphoneChatRoom chatRoom = LinphoneManager.getLc().getOrCreateChatRoom("sip:registrar@" + Config.DOMAIN_XMPP);
            boolean hasMessage = false;
            for (LinphoneChatMessage msg : chatRoom.getHistory()) {
                if (msg.getTo().getUserName().equals(acctId)) {
                    hasMessage = true;
                }
            }
            if (!hasMessage) {
                mList.remove(registrarContact);
            }
        }

        if (Config.DEBUG) {
            Log.d(TAG, "LIST " + mList);
        }
        Collections.sort(mList, new Comparator<Contact>() {
            @Override
            public int compare(Contact c1, Contact c2) {
                String f1 = c1.getFirstName().isEmpty() ? c1.getName() : c1.getFirstName();
                String f2 = c2.getFirstName().isEmpty() ? c2.getName() : c2.getFirstName();
                return f1.toUpperCase().compareTo(f2.toUpperCase());
            }
        });
         */
        ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
    }


    protected void replaceFragment(User user) {
        /*
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.replaceFragment() " + contact.getName());
        }

        boolean previouslyVisible = chatFragment != null && chatFragment.isVisible();
        chatFragment = ChatFragment.newInstance(contact, acctId);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainerChat, chatFragment, chatFragment.getClass().getSimpleName());
        if (!previouslyVisible) {
            transaction.addToBackStack(chatFragment.getClass().getSimpleName());
        }
        transaction.commit();

        MainApplication.unregisterLocalReceiver(mReceiver);
        stopTimer();
        mReceiver = null;
         */
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat_list_menu, menu);
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment:onCreateOptionsMenu " + chatFragment + " " + (chatFragment == null ? " " : chatFragment.isVisible()));
        }
        menu.findItem(R.id.menu_new_chat).setVisible(chatFragment == null || !chatFragment.isVisible());
        menu.findItem(R.id.menu_delete_chat_room).setVisible((chatFragment == null || !chatFragment.isVisible()));
    }



    @SuppressWarnings("unchecked")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_chat: {
                if (Config.DEBUG) {
                    Log.d(TAG, "ChatListFragment:menu_new_chat clicked");
                }
                /*
                final List<Contact> contacts = new ArrayList<Contact>(ContactDAO.getInstance().getContacts());
                if (contacts.size() == 0) {
                    Toast.makeText(getActivity(), getResources().getString(R.string.add_contacts), Toast.LENGTH_LONG).show();
                    return true;
                }
                Collections.sort(contacts, new Comparator<Contact>() {
                    @Override
                    public int compare(Contact c1, Contact c2) {
                        String f1 = c1.getFirstName().isEmpty() ? c1.getName() : c1.getFirstName();
                        String f2 = c2.getFirstName().isEmpty() ? c2.getName() : c2.getFirstName();
                        return f1.toUpperCase().compareTo(f2.toUpperCase());
                    }
                });
                String items[] = new String[contacts.size()];
                for (int i = 0; i < items.length; i++) {
                    Contact contact = contacts.get(i);
                    String name = contact.getFirstName() + " " + contact.getLastName();
                    items[i] = name.length() == 1 ? contact.getName() : name + " (" + contact.getName() + ")";
                }
                new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_action_new_chat).setTitle(getResources().getString(R.string.new_chat_session))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        replaceFragment(contacts.get(which));
                    }
                }).create().show();
                 */
                break;
            }
            case R.id.menu_delete_chat_room: {
                /*
                if(mList == null || mList.isEmpty())
                    break;

                String items[] = new String[mList.size()];
                for (int i = 0; i < items.length; i++) {
                    Contact contact = mList.get(i);
                    String name = contact.getFirstName() + " " + contact.getLastName();
                    items[i] = name.length() == 1 ? contact.getName() : name + " (" + contact.getName() + ")";
                }
                if (Config.DEBUG) {
                    Log.d(TAG, "ChatListFragment:menu_delete_chat_room clicked");
                }
                new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_menu_delete)
                .setTitle(getResources().getString(R.string.delete_chat_session))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.delete_chat_session)).setMessage(getResources().getString(R.string.delete_chat_session2))
                        .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteSession(mList.get(which).getName());
                            }
                        })
                        .setNegativeButton(getResources().getString(R.string.no),new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        })
                        .create().show();
                    }
                }).create().show();
                */
                break;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    } 


    protected void deleteSession(final String name) {
        /*
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                LinphoneChatRoom chatRoom = LinphoneManager.getLc().getOrCreateChatRoom("sip:" + name + "@" + Config.DOMAIN_XMPP);
                boolean hasUnread = false;
                for (LinphoneChatMessage msg : chatRoom.getHistory()) {
                    if (msg.getFrom().getUserName().equals(acctId) || msg.getTo().getUserName().equals(acctId)) {
                        if (!msg.isRead()) {
                            hasUnread = true;
                        }
                        chatRoom.deleteMessage(msg);
                        if (msg.getExternalBodyUrl() != null) {
                            MainApplication.deleteContent(msg.getExternalBodyUrl());
                        }
                    }
                }
                if (hasUnread) {
                    MainApplication.sendLocalBroadcast(new Intent("MESSAGE.READ"));
                    ((YouChatHomeActivity) getActivity()).getService().clearNotification();
                }
                if (Config.DEBUG) {
                    Log.d(TAG, "# MSGS REMAINING: " + chatRoom.getHistory().length);
                }
                if (chatRoom.getHistory().length == 0) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "DESTROY ROOM");
                    }
                    chatRoom.destroy();
                    //
                    // shared chat room case, too bad for other user...
                    //
                } else {
                    // other messages in this chat room, shared chat room case...
                    String deletedSessions = preferences.getString(acctId + "_deleted_chat_sessions", "");
                    deletedSessions = (deletedSessions.isEmpty() ? "" : ",") + name;
                    preferences.edit().putString(acctId + "_deleted_chat_sessions", deletedSessions).commit();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void ignored) {
                if (getActivity() != null && getView() != null) {
                    populateList();
                    getActivity().invalidateOptionsMenu();
                }
            }
        }.execute();
        */
    }



    @Override
    public void onHiddenChanged(boolean hidden) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.onHiddenChanged(" + hidden + ")");
        }
        super.onHiddenChanged(hidden);
        if (chatFragment != null && chatFragment.isVisible()) {
            chatFragment.onHiddenChanged(hidden);
        } else { 
            if (!hidden) {
                addReceiver();
                populateList();
                startTimer();
            } else {
                MainApplication.unregisterLocalReceiver(mReceiver);
                stopTimer();
                mReceiver = null;
            }
        }
        getActivity().invalidateOptionsMenu();
    }



    private void startTimer() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.startTimer");
        }
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            public void run() {
                /*
                if (!MainApplication.hasNetworkConnectivity() || isHidden() || getActivity() == null || LinphoneManager.getLcIfManagerNotDestroyedOrNull() == null) {
                    return;
                }
                for (int i = getListView().getChildCount(); --i >= 0; ) {
                    TextView tview = (TextView) getListView().getChildAt(i).findViewById(R.id.date_text);
                    String to = "sip:" + mList.get(i).getName() + "@" + Config.DOMAIN_XMPP;
                    LinphoneChatMessage msgs[] = LinphoneManager.getLc().getOrCreateChatRoom(to).getHistory(1);
                    Helper.updateTimeStamp(tview, msgs.length == 1 ? msgs[0].getTime() : 0);
                }
                startTimer();
                */
            }
        }, getTimerDelay());
    }


    private void stopTimer() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatListFragment.stopTimer");
        }
        handler.removeCallbacksAndMessages(null);
    }


    private long getTimerDelay() {
        boolean hasHour = false;
        /*
        for (LinphoneChatRoom room : LinphoneManager.getLc().getChatRooms()) {
            LinphoneChatMessage msgs[] = room.getHistory(1);
            if (msgs.length == 1) {
                long diff = System.currentTimeMillis() - msgs[0].getTime();
                if (diff < 3600000) {
                    hasHour = true;
                }
            }
        }
        */
        return hasHour ? 60000 : 36000000;
    }
} 