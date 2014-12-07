package com.acesse.youchat;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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

import com.crittercism.app.Crittercism;

import com.yml.youchatlib.models.User;


/**
 * 
 */
public class ContactListFragment extends ListFragment {

    private static final String TAG = "YOUC";

    protected AddEditContactFragment contactFragment;
    private List<User> list = new ArrayList<User>();
    private List<String> chattedContacts;
    private Handler handler = new Handler();
    private String acctId;



    /**
     * Use this factory method to create a new instance of this fragment using
     * the provided parameters.
     */
    public static ContactListFragment newInstance(String acctId) {
        ContactListFragment fragment = new ContactListFragment();
        fragment.acctId = acctId;
        return fragment;
    }

    public ContactListFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        Crittercism.leaveBreadcrumb("ContactListFragment: onCreate");

        if (Config.DEBUG) {
            Log.d(TAG,"ContactListFragment.onCreate()");
        }
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    /* (non-Javadoc)
     * @see android.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Crittercism.leaveBreadcrumb("ContactListFragment: onCreateView");

        if (Config.DEBUG) {
            Log.d(TAG,"ContactListFragment.onCreateView()");
        }

        View view = inflater.inflate(R.layout.fragment_contact_list, container, false);

        //
        // YC-838: Fragment restoration weird state... return empty listview and hope home activity exits...
        //
        /*
        if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() == null || ContactDAO.getInstance().getAccountUser() == null) {
            return view;
        }
        */

        chattedContacts = getChattedList();
        Collections.sort(list, new AlphaCompare());
        ArrayAdapter<User> adapter = new ArrayAdapter<User>(getActivity(), android.R.layout.simple_list_item_1, list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final User user = list.get(position);
                final View view = LayoutInflater.from(getActivity()).inflate(R.layout.contact_row_item, parent, false);
                /*
                if (chattedContacts != null && !chattedContacts.contains(contact.getName())){
                    ((TextView)view.findViewById(R.id.button_chat)).setVisibility(View.GONE);
                }else {
                    ((TextView) view.findViewById(R.id.button_chat)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getActivity().getActionBar().setSelectedNavigationItem(1);
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    ChatListFragment f = (ChatListFragment) getFragmentManager().findFragmentByTag(ChatListFragment.class.getSimpleName());
                                    if (f != null) {
                                        f.replaceFragment(contact);
                                    }
                                }
                            }, 30);
                        }
                    });
                }
                String name = contact.getFirstName() + " " + contact.getLastName();
                name = name.length() == 1 ? contact.getName() : name + " <small>(" + contact.getName() + ")</small>";
                ((TextView) view.findViewById(R.id.name_text)).setText(Html.fromHtml(name));
                Bitmap bitmap = ContactDAO.getInstance().getProfilePhoto(contact.getPhotoUri());
                ((ImageView) view.findViewById(R.id.button_photo)).setImageBitmap(bitmap);

                LinphoneFriend friend = contact.getFriend();
                if (friend != null && friend.getPresenceModel() != null && friend.getPresenceModel().getBasicStatus() == PresenceBasicStatus.Open) {
                    ((ImageView) view.findViewById(R.id.presence)).setImageLevel(1);
                }
                */
                return view;
            }
        };
        setListAdapter(adapter);

        //        if (savedInstanceState != null && savedInstanceState.containsKey("contact")) {
        //            String savedContactName = savedInstanceState.getString("contact");
        //            Fragment fragment = getFragmentManager().findFragmentByTag(AddEditContactFragment.class.getSimpleName());
        //            if (fragment != null) {
        //                for (Contact contact : list) {
        //                    if (savedContactName.equals(contact.getName())) {
        //                        ((AddEditContactFragment) fragment).contact = contact;
        //                        break;
        //                    }
        //                }
        //            }
        //        }
        return view;
    }

    // implement Comparator to compare Strings alphabetically
    private static class AlphaCompare implements Comparator {
        public int compare(Object o1, Object o2) {
            return (((String) o1).toLowerCase(). compareTo(((String) o2).toLowerCase()));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        if (Config.DEBUG) {
            Log.d(TAG,"ContactListFragment.onAttach()");
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        if (Config.DEBUG) {
            Log.d(TAG,"ContactListFragment.onDetach()");
        }
        super.onDetach();
    }

    @Override
    public void onStart() {
        if (Config.DEBUG) {
            Log.d(TAG, "ContactListFragment.onStart()");
        }
        super.onStart();

        populateList(true);

        addReceiver();
    }


    @Override
    public void onStop() {
        if (Config.DEBUG) {
            Log.d(TAG, "ContactListFragment.onStop()");
        }
        super.onStop();
        MainApplication.unregisterLocalReceiver(mReceiver);
    }


    @Override
    public void onResume() {
        if (Config.DEBUG) {
            Log.d(TAG, "ContactListFragment.onResume()");
        }
        super.onResume();
    }


    @Override
    public void onPause() {
        if (Config.DEBUG) {
            Log.d(TAG, "ContactListFragment.onPause()");
        }
        super.onPause();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (Config.DEBUG) {
            Log.d(TAG, "ContactListFragment.onSaveInstanceState()");
        }
        //        if (contactFragment != null && contactFragment.isAdded()) {
        //            outState.putString("contact", contactFragment.contact.getName());
        //        }
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (Config.DEBUG) {
            Log.d(TAG, "ContactListFragment:onListItem clicked");
        }
        replaceFragment((User) getListAdapter().getItem(position));
    }


    private void addReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("SUBSCRIPTION");
        filter.addAction("PRESENCE");
        filter.addAction("MESSAGE.RCVD");
        MainApplication.registerLocalReceiver(mReceiver, filter);
    }


    private void populateList(boolean animate) {
        list.clear();
        /*
        list.addAll(ContactDAO.getInstance().getContacts());
        chattedContacts = getChattedList();
        Collections.sort(list, new Comparator<Contact>() {
            @Override
            public int compare(Contact c1, Contact c2) {
                String f1 = c1.getFirstName().isEmpty() ? c1.getName() : c1.getFirstName();
                String f2 = c2.getFirstName().isEmpty() ? c2.getName() : c2.getFirstName();
                return (f1.toLowerCase()).compareTo(f2.toLowerCase());
            }
        });
        if (animate) {
            getListView().setLayoutAnimation(new LayoutAnimationController(AnimationUtils.loadAnimation(getActivity(), R.anim.slide_top_to_bottom)));
        }
        */
        ((ArrayAdapter) getListView().getAdapter()).notifyDataSetChanged();
    }


    private List<String> getChattedList(){
        List<String> previouslyChattedContacts = new ArrayList<String>();
        /*
        for (LinphoneChatRoom room : LinphoneManager.getLc().getChatRooms()) {
            //Log.d(TAG, "ContactListFragment:getChattedList " + room.getPeerAddress().getUserName() + "history: " + room.getHistory().length);
            //if (room.getHistory().length > 0){
            previouslyChattedContacts.add(room.getPeerAddress().getUserName());
            //}
        }
        */
        return previouslyChattedContacts;
    }


    protected void replaceFragment(User user) {

        Crittercism.leaveBreadcrumb("ContactListFragment: replacing with AddEditContactFragment");

        onHiddenChanged(true);
        contactFragment = AddEditContactFragment.newInstance(user, acctId);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainerContact, contactFragment, contactFragment.getClass().getSimpleName());
        transaction.addToBackStack(contactFragment.getClass().getSimpleName());
        transaction.commit();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Config.DEBUG) {
            Log.d(TAG,"ContactListFragment: onCreateOptionsMenu");
        }
        inflater.inflate(R.menu.contact_list_menu, menu);
        menu.findItem(R.id.menu_add_contact).setVisible(contactFragment == null || !contactFragment.isVisible());
        menu.findItem(R.id.menu_refresh_contacts).setVisible(list != null && list.size() > 0);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_contact:
                if (Config.DEBUG) {
                    Log.d(TAG, "ContactListFragment:menu_add_contact clicked");
                }
                //replaceFragment(new Contact(null, ""));
                break;
            case R.id.menu_refresh_contacts:
                if (Config.DEBUG) {
                    Log.d(TAG, "ContactListFragment:refresh_contacts clicked");
                }
                new GetAccountsAsyncTask() {
                    @Override
                    protected void onPostExecute(Void ignored) {
                        ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
                    }
                }.execute((Void[]) null);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    } 


    @Override
    public void onHiddenChanged(boolean hidden) {
        if (Config.DEBUG) {
            Log.d(TAG, "ContactListFragment.onHiddenChanged(" + hidden + ")");
        }
        super.onHiddenChanged(hidden);
        if (contactFragment != null && contactFragment.isVisible()) {
            contactFragment.onHiddenChanged(hidden);
        } 
        if (!hidden) {
            populateList(false);
            addReceiver();
        } else {
            MainApplication.unregisterLocalReceiver(mReceiver);
        }
        getActivity().invalidateOptionsMenu();
    }

    public void changeStatus(){
        /*
        for (Contact c : list){
            c.getFriend().getPresenceModel().setBasicStatus(PresenceBasicStatus.Closed);            
        }
        */
        ((ArrayAdapter) getListAdapter()).notifyDataSetChanged();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Config.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("ContactListFragment.onReceive: ").append(intent.getAction());
                /*
                if (action.equals("PRESENCE")) {
                    LinphoneFriend friend = (LinphoneFriend) intent.getSerializableExtra("friend");
                    if (friend != null) {
                        sb.append(" ").append(friend.getAddress().getUserName().toUpperCase());
                        sb.append(" (").append(friend.getStatus().toString().toUpperCase()).append(")");
                    }
                    Log.d(TAG, sb.toString());
                }
                */
            }
            // remove/delay to handle presence thundering herd...
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (getActivity() != null) {
                        try {
                            ((ArrayAdapter) getListView().getAdapter()).notifyDataSetChanged();
                        } catch(Exception ex) {
                        }
                    }
                }
            }, 250);
        }
    };
}
