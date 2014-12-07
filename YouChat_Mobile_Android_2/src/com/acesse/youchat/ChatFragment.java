package com.acesse.youchat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crittercism.app.Crittercism;
import com.yml.youchatlib.models.User;


/**
 * List fragment for text and media chatting.
 * 
 * @author brian
 */
public class ChatFragment extends ListFragment implements View.OnClickListener {


    private static final String TAG = "YOUC";
    private static final int REQUEST_PHOTO = 0;
    private static final int REQUEST_VIDEO = 1;
    private static final int REQUEST_AUDIO = 2;
    private static final int REQUEST_MEDIA = 3;

    //protected Contact fromContact, toContact;

    private String acctId;
    private Handler handler = new Handler();
    private Handler timer = new Handler();
    private List<MessageBean> mList = new ArrayList<MessageBean>();
    private List<String> mAttachPaths = new ArrayList<String>();
    private PopupWindow actionsPopup, attachmentsPopup;
    private EditText editText;
    private boolean isDeleteMode;
    private TextView headerView;
    private ListView mListView;
    private LinearLayout chatBarLayout;
    private ArrayAdapter<MessageBean> mAdapter;
    private int chatThumbnailMinSize;



    public static final ChatFragment newInstance(User user, String acctId) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.newInstance()");
        }
        ChatFragment fragment = new ChatFragment();
        //fragment.fromContact = ContactDAO.getInstance().getAccountUser();
        //fragment.toContact = contact;
        fragment.isDeleteMode = false; 
        fragment.acctId = acctId;

        return fragment;
    }



    public ChatFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onCreate()");
        }
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }




    @Override
    public void onDestroy() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onDestroy()");
        }
        super.onDestroy();
        timer.removeCallbacksAndMessages(null);

        if (MainApplication.getTaskCache().size() > 0) {
            if (Config.DEBUG) {
                Log.d(TAG, "Task Active Count " + MainApplication.getTaskCache().size());
                //
                // TODO: kill the tasks?
                //
                //ThreadPoolExecutor executor = (ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR;
                //executor.shutdown();
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onCreateView()");
        }

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        //
        // YC-838: Fragment restoration weird state... return empty listview and hope home activity exits...
        //
        /*
        if (LinphoneManager.getLcIfManagerNotDestroyedOrNull() == null || ContactDAO.getInstance().getAccountUser() == null || toContact == null) {
            return view;
        }
        */

        view.findViewById(R.id.button_send).setOnClickListener(this);
        view.findViewById(R.id.button_attach).setOnClickListener(this);
        editText = (EditText) view.findViewById(R.id.message_edit_text);
        chatBarLayout = (LinearLayout)view.findViewById(R.id.chat_bar);

        Resources resources = getResources();

        /*
        editText.setHint(resources.getString(R.string.chat_with) + " " + (toContact.getFirstName().isEmpty() ? toContact.getName() : toContact.getFirstName()));

        // Hide the chat bar if the session is with registrar
        if (toContact.equals(ContactDAO.getInstance().getRegistrarContact())) {
            chatBarLayout.setVisibility(View.GONE);
        }
        */

        chatThumbnailMinSize = resources.getDimensionPixelSize(R.dimen.chat_thumbnail_min_size);

        final int chatMargin = resources.getDimensionPixelSize(R.dimen.chat_margin);

        mAdapter = new ArrayAdapter<MessageBean>(getActivity(), android.R.layout.simple_list_item_1, mList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                MessageBean mbean = mList.get(position);
                View view = LayoutInflater.from(getActivity()).inflate(mbean.isOutgoing ? R.layout.chat_row_item_inverse : R.layout.chat_row_item, null, false);
                view.setTag(mbean);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-1);
                if (mbean.isOutgoing) {
                    lp.leftMargin = chatMargin;
                } else {
                    lp.rightMargin = chatMargin;
                }
                view.setLayoutParams(lp);

                setChatBubbleView(view, mbean);

                //
                // alpha animation in for new messages...
                //
                // YC-851 - seeing issue where view remains transparent...
                //                if (mbean.animateFlag) {
                //                    mbean.animateFlag = false;
                //                    view.setAlpha(0);
                //                    view.animate().alpha(1f).setDuration(500).setListener(null);
                //                }

                //
                // wrap so that margins are handled...
                //
                LinearLayout lay = new LinearLayout(getActivity());
                lay.setOrientation(LinearLayout.HORIZONTAL);
                lay.addView(view);

                return lay;
            }
        };

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onActivityCreated()");
        }
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        headerView = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.chat_header, null, false);
        mListView = getListView();
        mListView.addHeaderView(headerView);

        setListAdapter(mAdapter);
    }


    @Override
    public void onAttach(Activity activity) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onAttach()");
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onDetach()");
        }
        super.onDetach();
    }


    @Override
    public void onStart() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onStart() ");
        }
        super.onStart();
        loadHistory();
        IntentFilter filter = new IntentFilter();
        filter.addAction("MESSAGE.RCVD");
        filter.addAction("MESSAGE.UPDATE");
        MainApplication.registerLocalReceiver(mReceiver, filter);
    }


    @Override
    public void onStop() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onStop()");
        }
        super.onStop();
        MainApplication.unregisterLocalReceiver(mReceiver);
    }


    @Override
    public void onResume() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onResume()");
        }
        super.onResume();
    }


    @Override
    public void onPause() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onPause()");
        }
        super.onPause();
        stopTimer();
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onCreateOptionsMenu() " + isVisible());
        }
        inflater.inflate(R.menu.chat_fragment_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_delete_chats);
        menuItem.setVisible(mList.size() > 0 && !hidden);
        menuItem.setIcon(isDeleteMode ? R.drawable.ic_action_done : android.R.drawable.ic_menu_delete);
        if (Config.DEBUG) {
            if (!hidden) {
                menu.add("DEBUG: Automation");
                menu.add("DEBUG: Performance");
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete_chats:
                isDeleteMode = !isDeleteMode;
                chatBarLayout.setVisibility(isDeleteMode ? View.GONE : View.VISIBLE);
                mAdapter.notifyDataSetChanged();
                getActivity().invalidateOptionsMenu();
                break;
            default:
                if (Config.DEBUG) {
                    if (item.getTitle() != null && item.getTitle().toString().startsWith("DEBUG")) {
                        onDebugMenuItem(item.getTitle().toString());
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    } 


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onActivityResult " + requestCode + " " + resultCode);
        }
        if (resultCode == Activity.RESULT_OK && intent != null) {

            //REQUEST_PHOTO = 0;
            //REQUEST_VIDEO = 1;
            //REQUEST_AUDIO = 2;
            //REQUEST_MEDIA = 3;

            Uri attachUri = intent.getData();
            if (Config.DEBUG) {
                Log.d(TAG, "ATTACHING: " + attachUri);
            }

            if (attachUri == null) {
                new AlertDialog.Builder(getActivity()).setMessage(R.string.media_unavailable).create().show();
                return;
            }

            String scheme = attachUri.getScheme();
            String chatStoragePath = MainApplication.getChatStorageDirectory().getAbsolutePath();
            if ("content".equals(scheme) || ("file".equals(scheme) && attachUri.getPath().indexOf(chatStoragePath) == -1)) {
                editText.setHint(getResources().getString(R.string.attach_content));
                getView().findViewById(R.id.button_send).setEnabled(false);
                new AsyncTask<Uri, Void, Object>() {
                    @Override
                    protected Object doInBackground(Uri... path) {
                        return processExternalContent(path[0]);
                    }
                    @Override
                    protected void onPostExecute(Object result) {
                        if (getActivity() == null) {
                            return;
                        }
                        if (result instanceof Exception) {
                            Toast.makeText(getActivity(), getResources().getString(R.string.error_attach_content), Toast.LENGTH_SHORT).show();
                            editText.setHint(getResources().getString(R.string.type_messages));
                        } else {
                            mAttachPaths.add((String) result);
                            updateEditText();
                        }
                        getView().findViewById(R.id.button_send).setEnabled(true);
                        showAttachmentsPopup(true);
                    }
                }.execute(attachUri);
            } else if ("file".equals(attachUri.getScheme())) {
                mAttachPaths.add(attachUri.getPath().replace("file:", ""));
                updateEditText();
                showAttachmentsPopup(true);
                try {
                    synchronized (debugWaitLock) {
                        debugWaitLock.notifyAll();
                    }
                } catch (Exception ex) {
                }
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.unknown_content), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void updateEditText() {
        int photoCount = 0;
        int audioCount = 0;
        int videoCount = 0;
        for (String attachPath : mAttachPaths) {
            if (attachPath.endsWith(".jpg")) {
                photoCount++;
            } else if (attachPath.endsWith(".mp4")) {
                videoCount++;
            } else if (attachPath.endsWith(".aac")) {
                audioCount++;
            }
        }
        String prefix = null;
        if (photoCount > 0 && videoCount == 0 && audioCount == 0) {
            prefix = "Photo" + (photoCount == 1 ? "" : "s");
        } else if (photoCount == 0 && videoCount > 0 && audioCount == 0) {
            prefix = "Video" + (videoCount == 1 ? "" : "s");
        } else if (photoCount == 0 && videoCount == 0 && audioCount > 0) {
            prefix = "Audio" + (audioCount == 1 ? "" : "s");
        } else {
            int total = photoCount + videoCount + audioCount;
            if (total == 0) {
                editText.setHint(getResources().getString(R.string.type_messages));
                return;
            }
            prefix = (total == 1 ? "Content" : total + " items");
        }
        editText.setHint(getResources().getString(R.string.ready_to_send, prefix));
    }



    /**
     * On Activity Result has returned gallery content.  Perform pre-processing before
     * uploading to media server.
     */
    private Object processExternalContent(Uri attachUri) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.processExternalContent");
        }

        long startTime = System.currentTimeMillis();

        //        Cursor cursor = getActivity().getContentResolver().query(attachUri, new String[] { MediaStore.Files.FileColumns.DATA }, null, null, null);
        //        if (cursor != null) {
        //            cursor.moveToFirst();   
        //            String filePath = cursor.getString(0);
        //            cursor.close();
        //        }
        String filePath = attachUri.getPath();
        if (Config.DEBUG) {
            Log.d(TAG, "GALLERY FILE PATH: " + filePath);
        }

        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        if (filePath.indexOf("images") != -1 || filePath.indexOf(Environment.DIRECTORY_PICTURES) != -1) {
            fileName += ".jpg";
        } else if (filePath.indexOf("video") != -1 || filePath.indexOf(Environment.DIRECTORY_MOVIES) != -1) {
            fileName += ".mp4";
        } else if (filePath.indexOf("audio") != -1 || filePath.indexOf(Environment.DIRECTORY_MUSIC) != -1) {
            fileName += ".aac";
        } else {
            //
            // File type is unknown, try image decoding...
            //
            InputStream is = null;
            try {               
                is = getActivity().getContentResolver().openInputStream(attachUri);
            } catch (Exception ex) {
                Log.w(TAG, "Failed to open gallery content");
                return ex;
            }
            try {               
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null, opts);
                if (opts.outWidth > 0 && opts.outHeight > 0) {
                    fileName += ".jpg";
                } else {
                    throw new Exception();
                }
            } catch (Exception ex) {
                Log.d(TAG, "Unknown file type, failed to decode as image");
            } finally {
                try {
                    is.close();
                } catch (Exception ex) {
                }
            }
        }

        File file = new File(MainApplication.getChatStorageDirectory(), fileName);
        String attachPath = file.getAbsolutePath();
        if (Config.DEBUG) {
            Log.d(TAG, "COPY TO PATH: " + attachPath);
        }

        //
        // Copy the gallery content to YouChat private dir...
        //
        try {               
            InputStream in = new BufferedInputStream(getActivity().getContentResolver().openInputStream(attachUri));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            byte[] data = new byte[1024 * 8];
            int bytesRead = 0;
            while ((bytesRead = in.read(data)) > 0) {
                out.write(data, 0, bytesRead);
            }
            out.close();
            in.close();
        } catch (Exception ex) {
            Log.w(TAG, "FAILED TO COPY CONTENT TO CHAT STORAGE: " + attachPath, ex);
            return ex;
        }

        //
        // Post copy image modifications...
        //
        if (fileName.endsWith(".jpg")) {
            try {               
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = Helper.getSampleSize(attachPath, 800);

                Bitmap bitmap = BitmapFactory.decodeFile(attachPath, opts);
                bitmap = Helper.rotateImage(attachPath, bitmap);
                OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.close();
                if (Config.DEBUG) {
                    Log.d(TAG, "RESAMPLED IMAGE, SIZE: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                }
                bitmap.recycle();
            } catch (Exception ex) {
                Log.w(TAG, "RESAMPLE FAILED: "  + file);
            }
        }
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.processExternalContent Completed In " + (System.currentTimeMillis()-startTime) + "ms");
        }

        return attachPath;
    }



    //
    // STUPID NESTED FRAGMENTS - MUST MAINTAIN OUR OWN HIDDEN STATE
    //
    private boolean hidden = false;


    @Override
    public void onHiddenChanged(boolean hidden) {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.onHiddenChanged(" + hidden + ")");
        }
        super.onHiddenChanged(this.hidden = hidden);
        if (!hidden) {
            isDeleteMode = false; // reset delete mode
            chatBarLayout.setVisibility(View.VISIBLE);
            loadHistory();
            IntentFilter filter = new IntentFilter();
            filter.addAction("MESSAGE.RCVD");
            filter.addAction("MESSAGE.UPDATE");
            MainApplication.registerLocalReceiver(mReceiver, filter);
        } else {
            stopTimer();
            MainApplication.unregisterLocalReceiver(mReceiver);
            MainApplication.getBitmapCache().evictAll();
            mList.clear();
            mAdapter.notifyDataSetChanged();
        }
    }



    /**
     * Load the chat room history.  
     * 
     * PERF Note: this call can take about 1 second with 500 messages.
     */
    private final synchronized void loadHistory() {

        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.loadHistory()");
        }

        long startTime = System.currentTimeMillis();

        /*
        String firstName = toContact.getFirstName();
        headerView.setText(getResources().getString(R.string.chat_with) + " " + (firstName.isEmpty() ? toContact.getName() : firstName));

        ((TextView) mListView.getEmptyView()).setText(headerView.getText());

        mList.clear();

        chatRoom = LinphoneManager.getLc().getOrCreateChatRoom("sip:" + toContact.getName() + "@" + Config.DOMAIN_XMPP);
        LinphoneChatMessage msgs[] = chatRoom.getHistory();
        if (Config.DEBUG) {
            Log.d(TAG, "ROOM: " + chatRoom.getPeerAddress().getUserName() + " #MSGS: " + msgs.length);
        }
        String history = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_history", getString(R.string.pref_history_default));
        if (Config.DEBUG) {
            Log.d(TAG, "History Pref: " + history);
        }
        long numDays = 30;
        if (history.indexOf("day") != -1) {
            numDays = Long.parseLong(history.substring(0,history.indexOf(" ")));
        }
        if (Config.DEBUG) {
            Log.d(TAG, "NUM DAYS: " + numDays);
        }
        long cutoffTime = System.currentTimeMillis() - (numDays * 86400000L);
        final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy hh:mm:ss");
        if (Config.DEBUG) {
            Log.d(TAG, "CUT OFF TIME: " + sdf.format(cutoffTime));
        }
        boolean hasUnread = false;
        boolean hasOldMessages = false;
        int numText = 0;
        int numContent = 0;

        //
        // Marshal from LinphoneChatMessage to YouChat MessageBean. Ensure correct chat room and date.
        //
        for (LinphoneChatMessage msg : msgs) {
            String from = msg.getFrom().getUserName();
            String to = msg.getTo().getUserName();
            String txt = msg.getText();
            if (txt != null && !txt.isEmpty()) {
                numText++;
            } else {
                numContent++;
            }
            if (Config.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("MSG TIME: ").append(sdf.format(msg.getTime())).append(", FROM: ").append(from).append(", TO: ").append(to);
                sb.append(msg.isOutgoing() ? ", OUTGOING" : ", INCOMING");
                if (txt != null && !txt.isEmpty()) {
                    sb.append(", TEXT: ").append(txt);
                } else {
                    sb.append(", URL: ").append(msg.getExternalBodyUrl());
                }
                Log.d(TAG, sb.toString());
            }
            if (cutoffTime < msg.getTime()) {
                if (!hasUnread && !msg.isRead()) {
                    hasUnread = true;
                }
                if (from.equals(acctId) || to.equals(acctId)) {
                    mList.add(new MessageBean(msg, false));
                }
            } else {
                hasOldMessages = true;
            }
        }

        //
        // Account for any active (upload or download) tasks from a previous session details...
        //
        // Must recreate uploading message beans in the case that the user changes chat sessions
        // and then comes back where the upload has not finished.  
        //
        if (MainApplication.getTaskCache().size() > 0) {
            //ThreadPoolExecutor executor = (ThreadPoolExecutor) AsyncTask.THREAD_POOL_EXECUTOR;
            //Log.d(TAG, "ThreadPoolExecutor Active Count " + executor.getActiveCount());
            if (Config.DEBUG) {
                Log.d(TAG, "Active Task Count " + MainApplication.getTaskCache().size());
            }
            boolean hasUpload = false;
            for (MessageBean mbean : MainApplication.getTaskCache()) {
                if (mbean.isDownloading) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "Active Download: " + mbean);
                    }
                    for (int i = mList.size(); --i >= 0; ) {
                        if (mList.get(i).id == mbean.id) {
                            mList.set(i, mbean);
                            break;
                        }
                    }
                } else if (mbean.isUploading && mbean.to.equals(toContact.getName())) {
                    if (Config.DEBUG) {
                        Log.d(TAG, "Active Upload: " + mbean);
                    }
                    mList.add(mbean);
                    hasUpload = true;
                }
            }
            if (hasUpload) {
                //
                // Ensure order is maintained.
                //
                Collections.sort(mList, new Comparator<MessageBean>() {
                    @Override
                    public int compare(MessageBean b1, MessageBean b2) {
                        return Long.valueOf(b1.time).compareTo(Long.valueOf(b2.time));
                    }
                });
            }
        }

        if (hasUnread) {
            //
            // Shared chat room case, this clobbers other user's unread status...
            //
            chatRoom.markAsRead();
            MainApplication.sendLocalBroadcast(new Intent("MESSAGE.READ"));
            ((YouChatHomeActivity) getActivity()).getService().clearNotification();
        }

        if (hasOldMessages) {
            ((YouChatHomeActivity) getActivity()).getService().deleteOldMessages();
        }

        long diff = System.currentTimeMillis() - startTime;
        StringBuilder sb = new StringBuilder();
        sb.append(diff).append("ms");
        sb.append(" #MSGS: ").append(mList.size());
        sb.append(" #TEXT: ").append(numText);
        sb.append(" #CONTENT: ").append(numContent);
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.loadHistory() " + sb.toString());
        }
        Crittercism.leaveBreadcrumb("Chat: Load History " + sb.toString());
        */

        getActivity().invalidateOptionsMenu();
        mAdapter.notifyDataSetChanged();
        if (mList.size() > 1) {
            selectLastItem();
        }
        startTimer();
    }



    private void selectLastItem() {
        handler.post(new Runnable() {
            public void run() {
                mListView.setSelectionFromTop(mList.size()-1, -999);
            }
        });
    }



    /**
     * Set Incoming & Outgoing chat message view.
     * 
     * @param view
     * @param mbean
     */
    private void setChatBubbleView(final View view, final MessageBean mbean) {

        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.setChatBubbleView() " + mbean);
        }

        setChatBubbleBackground(view, mbean);

        if (isDeleteMode) {
            View buttonRemove = view.findViewById(R.id.button_remove);
            buttonRemove.setVisibility(View.VISIBLE);
            buttonRemove.setOnClickListener(this);
        }

        //
        // Photo...
        //
        /*
        Contact contact = mbean.isOutgoing ? fromContact : toContact;
        Bitmap bitmap = ContactDAO.getInstance().getProfilePhoto(contact.getPhotoUri());
        ((ImageView) view.findViewById(R.id.button_photo)).setImageBitmap(bitmap);

        //
        // Name...
        //
        ((TextView) view.findViewById(R.id.name_text)).setText(contact.getFirstName() + " " + contact.getLastName());
        */

        //
        // Text...
        //
        if (mbean.text == null || mbean.text.isEmpty()) {
            view.findViewById(R.id.message_text).setVisibility(View.GONE);
        } else {
            ((TextView) view.findViewById(R.id.message_text)).setText(mbean.text);
        }

        //
        // Time ago or stamp, or delivery status...
        //
        setChatBubbleDeliveryState(view, mbean);

        //
        // Chat media...
        //
        if (mbean.externalBodyUrl != null || mbean.localPath != null) {

            ViewStub stub = (ViewStub) view.findViewById(R.id.message_media);
            final View mediaView = stub != null ? stub.inflate() : view.findViewById(R.id.chat_media);

            final RecyclingImageView chatImage = (RecyclingImageView) mediaView.findViewById(R.id.chat_image);
            chatImage.setAlpha(1f);
            chatImage.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mbean.localPath == null && !mbean.isDownloading) {
                        chatImage.setAlpha(0.5f);
                        downloadMedia(mediaView, mbean);
                    } else {
                        showMedia(mbean);
                    }
                }
            });



            //
            // Check to see if we need to reload the local file likely due to an eviction from LruCache...
            //
            if (mbean.localPath != null && !mbean.localPath.isEmpty() && MainApplication.getLocalFilePath(mbean.externalBodyUrl) == null && !mbean.isDownloading && !mbean.isUploading) {
                mbean.localPath = null;
            }

            //
            // Look for local image and video file, and load thumbnail if necessary...
            //
            if (mbean.localPath == null && !mbean.isDownloading) {
                mbean.localPath = MainApplication.getLocalFilePath(mbean.externalBodyUrl);
                if (mbean.localPath == null && mbean.externalBodyUrl != null) {
                    final String name = mbean.externalBodyUrl.substring(mbean.externalBodyUrl.lastIndexOf("/")+1);
                    File file = new File(MainApplication.getChatStorageDirectory(), name);
                    //if (Config.DEBUG) {
                    //Log.d(TAG, "MSG CONTENT: " + mbean.externalBodyUrl + " " + file + (file.exists() ? " EXISTS" : " NOT EXIST"));
                    //}
                    if (file.exists()) {
                        mbean.localPath = file.getAbsolutePath();

                        //
                        // Chat video & image thumbnails are dynamic. On a GS4 thumbnail loading time
                        // is about 10~20ms for images, 30~40ms for video.  Not exactly smooth scrolling.
                        //

                        Bitmap bmap = MainApplication.getThumbnail(mbean.localPath);
                        if (bmap != null) {
                            MainApplication.addBitmap(mbean.externalBodyUrl, mbean.localPath, bmap);
                        }

                        //
                        // Load the thumbnail, and set...
                        //
                        //                        new AsyncTask<Void, Void, Bitmap>() {
                        //                            protected Bitmap doInBackground(Void... params) {
                        //                                return MainApplication.getThumbnail(mbean.localPath);
                        //                            }
                        //                            protected void onPostExecute(Bitmap bmap) {
                        //                                if (getActivity() == null) {
                        //                                    return;
                        //                                }
                        //                                if (bmap != null) {
                        //                                    chatImage.setImageBitmap(bmap);
                        //                                    //((View) chatImage.getParent()).getLayoutParams().width = bmap.getWidth();
                        //                                } 
                        //                                MainApplication.addBitmap(mbean.externalBodyUrl, mbean.localPath, bmap);
                        //                            }
                        //                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            } 

            //
            // Set chat image or video thumbnail or placeholder, initiate download of image if necessary...
            //
            Bitmap bmap = MainApplication.getBitmap(mbean.externalBodyUrl);
            if (bmap != null) {
                chatImage.setImageBitmap(bmap);
                chatImage.setKey(mbean.externalBodyUrl);
                chatImage.getLayoutParams().height = -2;
                view.requestLayout();
                //((View) chatImage.getParent()).getLayoutParams().width = rbmap.getWidth();
            } else {
                String name = mbean.localPath != null ? mbean.localPath : mbean.externalBodyUrl != null ? mbean.externalBodyUrl : "";
                if (name.endsWith("jpg") || name.endsWith(".mp4")) {
                    bmap = MainApplication.getThumbnail(mbean.localPath);
                    if (bmap == null) {
                        bmap = name.endsWith("jpg") ? MainApplication.getImagePlaceholderBitmap() : MainApplication.getVideoPlaceholderBitmap();
                        chatImage.setImageBitmap(bmap);
                        chatImage.getLayoutParams().height = chatThumbnailMinSize;
                    } else {
                        chatImage.setImageBitmap(bmap);
                        chatImage.setKey(name);
                    }
                } else if (name.endsWith(".aac")) {
                    chatImage.setImageBitmap(MainApplication.getAudioPlaceholderBitmap());
                    chatImage.getLayoutParams().height = chatThumbnailMinSize / 2;
                }
                if (mbean.localPath == null && !mbean.isOutgoing && !mbean.isDownloading) {
                    //
                    // Immediate download or w/o user tapping content...
                    //
                    chatImage.setAlpha(0.5f);
                    downloadMedia(mediaView, mbean);
                } 
            }

            //
            // Set audio or video duration...
            //
            if (mbean.localPath != null && !mbean.localPath.endsWith(".jpg") && !mbean.isDownloading) {
                if (mbean.localPath.endsWith(".mp4")) {
                    mediaView.findViewById(R.id.chat_image_play).setVisibility(View.VISIBLE);
                }
                final TextView tview = (TextView) mediaView.findViewById(R.id.chat_duration);
                if (mbean.duration == null && mbean.externalBodyUrl != null) {
                    //
                    // Load the audio or video duration, and set...
                    //
                    new AsyncTask<Void, Void, Void>() {
                        protected Void doInBackground(Void... params) {
                            mbean.duration = MainApplication.getMediaDuration(mbean.localPath);
                            return null;
                        }
                        protected void onPostExecute(Void ignored) {
                            if (getActivity() == null) {
                                return;
                            }
                            if (mbean.duration != null) {
                                tview.setVisibility(View.VISIBLE);
                                tview.setText(mbean.duration);
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else if (mbean.duration != null) {
                    tview.setVisibility(View.VISIBLE);
                    tview.setText(mbean.duration);
                }
            }

            //
            // Progress bars...
            //
            if (mbean.isUploading || mbean.isDownloading) {
                mediaView.findViewById(R.id.progressBar3).setVisibility(View.VISIBLE);
                mbean.progressBar = (ProgressBar) mediaView.findViewById(R.id.progressBar4);
                mbean.progressBar.setVisibility(View.VISIBLE);
            } else {
                mediaView.findViewById(R.id.progressBar3).setVisibility(View.GONE);
                mediaView.findViewById(R.id.progressBar4).setVisibility(View.GONE);
            }
        }
    }



    private final void setChatBubbleBackground(View view, MessageBean mbean) {
        if (mbean.isOutgoing) {
            /*
            if (mbean.state == LinphoneChatMessage.State.InProgress) {
                view.setBackgroundResource(R.drawable.rounded_bubble);
            } else if (mbean.state == LinphoneChatMessage.State.NotDelivered || mbean.state == LinphoneChatMessage.State.UploadFailed) {
                view.setBackgroundResource(R.drawable.rounded_bubble_red);
            } else {
                view.setBackgroundResource(R.drawable.rounded_bubble_green);
            }
            */
        } else {
            view.setBackgroundResource(R.drawable.rounded_bubble);
        }
    }


    private final void setChatBubbleDeliveryState(View view, MessageBean mbean) {
        //TextView dateText = (TextView) view.findViewById(R.id.date_text);
        /*
        if (mbean.state == LinphoneChatMessage.State.Delivered) {
            Helper.updateTimeStamp(dateText, mbean.time);
        } else if (mbean.state == LinphoneChatMessage.State.InProgress) {
            dateText.setText(getResources().getString(R.string.in_progress));
        } else if (mbean.state == LinphoneChatMessage.State.NotDelivered) {
            dateText.setText(getResources().getString(R.string.not_delivered));
        } else if (mbean.state == LinphoneChatMessage.State.UploadFailed) {
            dateText.setText(getResources().getString(R.string.upload_error));
        } else {
            dateText.setText("");
        }
        */
    }



    /**
     * Iterate over existing list children and if found update appropriate chat bubble view.
     * 
     * @param mbean
     */
    private void setChatBubbleView(MessageBean mbean) {
        for (int i = mListView.getChildCount(); --i >= 0; ) {
            View view = mListView.getChildAt(i);
            if (view instanceof LinearLayout) {
                view = ((LinearLayout) view).getChildAt(0);
                MessageBean b = (MessageBean) view.getTag();
                if (b != null && b.time == mbean.time && b.id == mbean.id) {
                    setChatBubbleView(view, mbean);
                    if (i == mListView.getChildCount()-1) {
                        selectLastItem();
                    }
                    break;
                }
            }
        }
    }



    private void setChatBubbleState(MessageBean mbean) {
        for (int i = mListView.getChildCount(); --i >= 0; ) {
            View view = mListView.getChildAt(i);
            if (view instanceof LinearLayout) {
                view = ((LinearLayout) view).getChildAt(0);
                MessageBean b = (MessageBean) view.getTag();
                if (b != null && b.time == mbean.time && b.id == mbean.id) {
                    setChatBubbleBackground(view, mbean);
                    setChatBubbleDeliveryState(view, mbean);
                    View mediaView = view.findViewById(R.id.chat_media);
                    /*
                    if (mediaView != null && mbean.state != LinphoneChatMessage.State.InProgress) {
                        mediaView.findViewById(R.id.progressBar3).setVisibility(View.GONE);
                        mediaView.findViewById(R.id.progressBar4).setVisibility(View.GONE);
                    }
                    */
                    if (i == mListView.getChildCount()-1) {
                        selectLastItem();
                    }
                    break;
                }
            }
        }
    }


    /**
     * Initiate a download of the media
     * 
     * @param container
     * @param mbean
     */
    private void downloadMedia(final View container, final MessageBean mbean) {
        if (!MainApplication.hasNetworkConnectivity()) {
            MainApplication.showNetworkUnavailableDialog(getFragmentManager());
            return;
        }
        container.findViewById(R.id.progressBar3).setVisibility(View.VISIBLE);
        mbean.progressBar = (ProgressBar) container.findViewById(R.id.progressBar4);
        mbean.progressBar.setVisibility(View.VISIBLE);
        mbean.isDownloading = true;
        new DownloadMediaAsyncTask() {
            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                if (result instanceof String && mList.indexOf(mbean) == mList.size()-1) {
                    mListView.setSelection(mList.size()-1);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mbean);
    }



    /**
     * User has tapped on the chat media, show or play the content.
     */
    private void showMedia(MessageBean mbean) {
        if (mbean.isDownloading) {
            Toast.makeText(getActivity(), getResources().getString(R.string.wait_to_finish_download), Toast.LENGTH_SHORT).show();
            return;
        }
        String pathName = mbean.localPath;
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.showMedia " + pathName);
        }
        if (pathName.endsWith(".jpg") || pathName.indexOf("images") != -1 || pathName.indexOf("Pictures") != -1) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ImageDialogFragment f = ImageDialogFragment.newInstance(pathName);
            f.show(ft, "dialog");
        } else if (pathName.endsWith(".mp4") || pathName.indexOf("video") != -1 || pathName.indexOf("Movies") != -1) {
            //FragmentTransaction ft = getFragmentManager().beginTransaction();
            //VideoDialogFragment f = VideoDialogFragment.newInstance(pathName);
            //f.show(ft, "dialog");
            if (MainApplication.getMediaDuration(pathName) != null) {
                startActivity(new Intent(getActivity(), VideoPlaybackActivity.class).putExtra("video.file", pathName));
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.unable_to_play), Toast.LENGTH_SHORT).show();
            }
        } else if (pathName.endsWith(".aac") || pathName.indexOf("audio") != -1) {
            if (MainApplication.getMediaDuration(pathName) != null) {
                startActivity(new Intent(getActivity(), AudioCaptureActivity.class).putExtra("audio.file", pathName));
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.unable_to_play), Toast.LENGTH_SHORT).show();
            }
        } else {
            // try to decode as an image...
            if (MainApplication.getImageDimensions(pathName.replace("file:", ""))[0] > 0) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ImageDialogFragment f = ImageDialogFragment.newInstance(pathName);
                f.show(ft, "dialog");
            } else {
                Toast.makeText(getActivity(), R.string.unknown_mime_type, Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Add a message, then scroll to the bottom.
     */
    private synchronized void addMessage(MessageBean mbean) {
        if (!isVisible()) {
            return;
        }
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.addMessage " + mbean);
        }
        mList.add(mbean);
        //
        // Ensure order is maintained.  (Offline messages case sends out-of-order bursts)
        //
        Collections.sort(mList, new Comparator<MessageBean>() {
            @Override
            public int compare(MessageBean b1, MessageBean b2) {
                return Long.valueOf(b1.time).compareTo(Long.valueOf(b2.time));
            }
        });

        mAdapter.notifyDataSetChanged();
        selectLastItem();
        getActivity().invalidateOptionsMenu();
    }


    /**
     * Send a Text message.
     */
    private void sendTextMessage() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.sendTextMessage");
        }
        /*
        LinphoneChatMessage msg = chatRoom.createLinphoneChatMessage(editText.getText().toString());
        chatRoom.markAsRead();
        chatRoom.sendMessage(msg, this);
        editText.setText("");
        MessageBean mbean = new MessageBean(msg, true);
        mbean.deliveryDuration = System.currentTimeMillis();
        addMessage(mbean);
        */
        Crittercism.leaveBreadcrumb("Chat: Text Message");
    }



    /**
     * Send a Photo, Video or Audio message.
     */
    private void sendMediaMessage() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.sendMediaMessage");
        }

        if (mAttachPaths.size() > 5) {
            Toast.makeText(getActivity(), getResources().getString(R.string.maximum_attachments), Toast.LENGTH_SHORT).show();
            return;
        }

        String text = editText.getText().toString();
        if (text != null && !text.isEmpty()) {
            sendTextMessage();
        }

        Crittercism.leaveBreadcrumb("Chat: Media Message");

        int size = mAttachPaths.size();
        for (int i = 0; i < size; i++) {
            long delay = i * 200;
            int msgId = new Random().nextInt();
            /*
            final MessageBean mbean = new MessageBean(msgId, acctId, toContact.getName(), "", System.currentTimeMillis()+delay, null, LinphoneChatMessage.State.InProgress, true, true);
            mbean.localPath = mAttachPaths.get(i);
            mbean.isUploading = true;
            Runnable runner = new Runnable() {
                public void run() {
                    addMessage(mbean);
                    new UploadMediaAsyncTask(ChatFragment.this) {
                        @Override
                        protected void onPostExecute(Object result) {
                            if (result instanceof String && mList.indexOf(mbean) == mList.size()-1) {
                                selectLastItem();
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mbean);
                }
            };
            if (size == 1) {
                runner.run();
            } else {
                // stagger
                handler.postDelayed(runner, delay);
            }
            */
        }

        editText.setHint(getResources().getString(R.string.type_messages));
        mAttachPaths.clear();
    }


    /**
     * Incoming messages received here.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() == null) {
                return;
            }
            String action = intent.getAction();
            if (action.equals("MESSAGE.RCVD")) {
                String from = intent.getStringExtra("from");
                    /*
                String room = chatRoom.getPeerAddress().getUserName();
                if (Config.DEBUG) {
                    Log.d(TAG, "ChatFragment.onReceive FROM: " + from + " ROOM: " + room);
                }
                if (room.equalsIgnoreCase(from)) {
                    chatRoom.markAsRead();
                    int id = intent.getIntExtra("id", 0);
                    for (LinphoneChatMessage msg : chatRoom.getHistory(3)) {
                        if (msg.getStorageId() == id) {
                            addMessage(new MessageBean(msg, true));
                            ((YouChatHomeActivity) getActivity()).getService().clearNotification();
                            startTimer();
                            break;
                        }
                    }
                }
                    */
            } else if (action.equals("MESSAGE.UPDATE")) {
                //
                // from download or upload media async task
                //
                final String cmd = intent.getStringExtra("cmd");
                int id = intent.getIntExtra("id", 0);
                long time = intent.getLongExtra("time", 0);
                if (Config.DEBUG) {
                    Log.d(TAG, "ChatFragment.onReceive " + cmd + " ID: " + id + " TIME: " + time);
                }
                for (int i = mList.size(); --i >= 0; ) {
                    final MessageBean mbean = mList.get(i);
                    if (mbean.time == time && mbean.id == id) {
                        if ("download".equals(cmd)) {
                            setChatBubbleView(mbean);
                        } else if ("upload".equals(cmd)) {
                            setChatBubbleState(mbean);
                        }
                    }
                }
            }
        }
    };




    /*
    @Override
    public void onLinphoneChatMessageStateChanged(LinphoneChatMessage msg, final LinphoneChatMessage.State state) {
        if (!isVisible() || getActivity() == null) {
            return;
        }
        int id = msg.getStorageId();
        long time = msg.getTime();
        if (Config.DEBUG) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Log.d(TAG, "ChatFragment.onLinphoneChatMessageStateChanged Id: " + id + ", Time:" + sdf.format(time) + ", State: " + state);
        }
        for (int i = mList.size(); --i >= 0; ) {
            final MessageBean mbean = mList.get(i);
            if (mbean.isOutgoing && time == mbean.time && mbean.id == id) {
                mbean.state = state;
                mbean.deliveryDuration = System.currentTimeMillis() - mbean.deliveryDuration;
                startTimer();
                handler.post(new Runnable() {
                    public void run() {
                        setChatBubbleState(mbean);
                    }
                });
                break;
            }
        }
        if (Config.DEBUG) {
            synchronized (debugWaitLock) {
                debugWaitLock.notifyAll();
            }
        }
    }
    */



    @Override
    public void onClick(final View v) {
        if (actionsPopup != null && actionsPopup.isShowing()) {
            actionsPopup.dismiss();
            actionsPopup = null;
        }
        switch (v.getId()) {
            case R.id.photo_button : {
                // External App Approach...
                //                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "youchat");
                //                if (!dir.exists() && !dir.mkdir()) {
                //                    Log.w(TAG, "Oops! Failed create " + dir + " directory");
                //                }
                //                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
                //                File file = new File(dir, timeStamp + ".jpg");
                //                mAttachPath = file.getAbsolutePath();
                //                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file)); 
                //                intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                //                startActivityForResult(intent, REQUEST_PHOTO);
                startActivityForResult(new Intent(getActivity(), ImageCaptureActivity.class), REQUEST_PHOTO);
                break;
            }
            case R.id.video_button : {
                startActivityForResult(new Intent(getActivity(), VideoCaptureActivity.class), REQUEST_VIDEO);
                break;
            }
            case R.id.audio_button : {
                // External App Approach...
                //                Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                //                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);
                //                startActivityForResult(intent, REQUEST_AUDIO);
                startActivityForResult(new Intent(getActivity(), AudioCaptureActivity.class), REQUEST_AUDIO);
                break;
            }
            case R.id.gallery_button : {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_MEDIA);
                break;
            }
            case R.id.attachments_button : {
                showAttachmentsPopup(false);
                break;
            }
            case R.id.button_attach : {
                LinearLayout view = (LinearLayout) LayoutInflater.from(getActivity()).inflate(R.layout.media_selection, null, false);
                view.findViewById(R.id.attachments_button).setVisibility(mAttachPaths.size() == 0 ? View.GONE : View.VISIBLE);
                for (int i = 0; i < view.getChildCount(); i++) {
                    view.getChildAt(i).setOnClickListener(this);
                }
                view.measure(0, 0);
                actionsPopup = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                actionsPopup.setBackgroundDrawable(new BitmapDrawable());
                actionsPopup.setOutsideTouchable(true);
                actionsPopup.setFocusable(true);
                actionsPopup.setAnimationStyle(R.style.Animations_GrowFromBottom);
                actionsPopup.showAsDropDown(v, 0, -view.getMeasuredHeight());
                break;
            }
            case R.id.button_send : {
                /*
                if (MainApplication.hasNetworkConnectivity() && LinphoneManager.getLc().getDefaultProxyConfig().isRegistered()) {
                    if (mAttachPaths.size() == 0) {
                        if (editText.getText().length() > 0) {
                            sendTextMessage();
                        }
                    } else {
                        sendMediaMessage();
                    }
                } else {
                    MainApplication.showNetworkUnavailableDialog(getFragmentManager());
                }    
                */
                break;
            }
            case R.id.button_remove : {
                final View parent = (View) v.getParent();
                final MessageBean bean = (MessageBean) parent.getTag();
                parent.animate().scaleY(0).alpha(0).setDuration(400).setListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        /*
                        for (LinphoneChatMessage msg : chatRoom.getHistory()) {
                            if (msg.getTime() == bean.time && msg.isOutgoing() == bean.isOutgoing) {
                                chatRoom.deleteMessage(msg);
                                if (msg.getExternalBodyUrl() != null) {
                                    MainApplication.deleteContent(msg.getExternalBodyUrl());
                                }
                                break;
                            }
                        }
                        */
                        mList.remove(bean);
                        mAdapter.notifyDataSetChanged();
                        if (mList.size() == 0) {
                            isDeleteMode = false;
                            getActivity().invalidateOptionsMenu();
                            chatBarLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }
    }


    private void showAttachmentsPopup(final boolean autoDismiss) {
        final int pad = getResources().getDimensionPixelSize(R.dimen.padding10);
        final LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(getResources().getColor(R.color.black));
        layout.setPadding(pad, pad, pad, pad);
        for (int i = 0; i < mAttachPaths.size(); i++) {
            final String apath = mAttachPaths.get(i);
            String type = null;
            Bitmap bmap = null;
            if (apath.endsWith(".jpg")) {
                type = "Photo";
                bmap = MainApplication.getThumbnail(apath);
            } else if (apath.endsWith(".mp4")) {
                type = "Video";
                bmap = MainApplication.getThumbnail(apath);
            } else if (apath.endsWith(".aac")) {
                type = "Audio";
                bmap = BitmapFactory.decodeResource(getResources(), R.drawable.audio_placeholder);
            } else {
                type = "Content";
                bmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_placeholder);
            }
            final View view = LayoutInflater.from(getActivity()).inflate(R.layout.attachment, null, false);
            ((ImageView) view.findViewById(R.id.attachment_image)).setImageBitmap(bmap);
            ((TextView) view.findViewById(R.id.attachment_text)).setText(type);
            view.setPadding(0, 0, 0, i < mAttachPaths.size()-1 ? pad/2 : 0);
            view.findViewById(R.id.delete_attachment).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    layout.removeView(view);
                    mAttachPaths.remove(apath);
                    updateEditText();
                    if (mAttachPaths.size() == 0 && attachmentsPopup != null && attachmentsPopup.isShowing()) {
                        attachmentsPopup.dismiss();
                        attachmentsPopup = null;
                    }
                }
            });
            layout.addView(view, -1, -1);
        }
        layout.measure(0, 0);
        layout.postDelayed(new Runnable() {
            public void run() {
                View v = getView().findViewById(R.id.button_attach);
                attachmentsPopup = new PopupWindow(layout, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                attachmentsPopup.setBackgroundDrawable(new BitmapDrawable());
                attachmentsPopup.setOutsideTouchable(true);
                attachmentsPopup.setFocusable(true);
                attachmentsPopup.setAnimationStyle(R.style.Animations_GrowFromBottom);
                attachmentsPopup.showAsDropDown(v, 0, -layout.getMeasuredHeight()-v.getMeasuredHeight()-pad/2);
            }
        }, 30);
        if (autoDismiss) {
            /*
            layout.postDelayed(new Runnable() {
                public void run() {
                    if (attachmentsPopup != null && attachmentsPopup.isShowing()) {
                        attachmentsPopup.dismiss();
                        attachmentsPopup = null;
                    }
                }
            }, 1500);
             */
        }
    }


    private synchronized void startTimer() {
        timer.removeCallbacksAndMessages(null);
        timer.postDelayed(new Runnable() {
            public void run() {
                /*
                if (!MainApplication.hasNetworkConnectivity() || hidden || getActivity() == null || LinphoneManager.getLcIfManagerNotDestroyedOrNull() == null) {
                    return;
                }
                if (Config.DEBUG) {
                    Log.d(TAG, "ChatFragment.Timer Running");
                }
                for (int i = mListView.getChildCount(); --i >= 0; ) {
                    View v = mListView.getChildAt(i);
                    if (v != null && v instanceof LinearLayout) {
                        View lay = ((LinearLayout) v).getChildAt(0);
                        if (lay != null) {
                            MessageBean mbean = (MessageBean) lay.getTag();
                            if (mbean != null && mbean.state == LinphoneChatMessage.State.Delivered) {
                                Helper.updateTimeStamp((TextView) lay.findViewById(R.id.date_text), mbean.time);
                            }
                        }
                    }
                }
                startTimer();
                */
            }
        }, getTimerDelay());
    }


    private void stopTimer() {
        if (Config.DEBUG) {
            Log.d(TAG, "ChatFragment.stopTimer");
        }
        timer.removeCallbacksAndMessages(null);
    }


    private long getTimerDelay() {
        if (mList.size() > 0) {
            long diff = System.currentTimeMillis() - mList.get(mList.size()-1).time;
            if (diff < 3600000) {
                return 60000;
            }
        }
        return 3600000;
    }




    /************************************
     *             DEBUG                *
     ************************************/

    private final Object debugWaitLock = new Object();
    private final List<DebugBean> dList = new ArrayList<DebugBean>();
    private final List<AsyncTask> tList = new ArrayList<AsyncTask>();
    private List<MessageBean> messageBeans = new ArrayList<MessageBean>();
    private boolean debugSerial = false;
    private long uploadSize = 1048576;
    private boolean isUploadOnly = true;
    private Button runButton;
    private TextView downloadPerSecondTextView;
    private TextView uploadPerSecondTextView;
    private View debugAutomationView;
    private Thread automationThread;
    private Dialog automationDialog;


    private static class DebugBean extends MessageBean {
        String name;
        boolean failed;
        long deliveryTime;
        private DebugBean(String name, String to) {
            this.name = name;
            this.to = to;
        }
    }


    private final void debugSleep(long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch (Exception ex) {
        }
    }


    private final void startDebugUploadTest() {
        tList.clear();
        uploadPerSecondTextView.setText("0.0");
        ((ArrayAdapter) ((HeaderViewListAdapter) mListView.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
        for (DebugBean bean : dList) {
            bean.progressBar.setProgress(0);
            bean.progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.greenprogress));
        }
        new Thread(new Runnable() {
            public void run() {
                for (DebugBean bean : dList) {
                    final DebugBean mb = bean;
                    try {
                        OutputStream os = new BufferedOutputStream(new FileOutputStream(bean.localPath));
                        for (int j = 0; j < uploadSize; j++) {
                            os.write(0);
                        }
                        os.close();
                    } catch (Exception ex) {
                        Log.w(TAG, "Failed " + bean.localPath, ex);
                    }
                    AsyncTask<MessageBean, Integer, Object> task = new UploadMediaAsyncTask() {
                        long startTime;
                        DecimalFormat dformat = new DecimalFormat("#,###.0");
                        @Override
                        protected void onPreExecute() {
                            startTime = System.currentTimeMillis();
                        }
                        @Override
                        protected Object doInBackground(MessageBean... args) {
                            Object obj = super.doInBackground(args);
                            if (obj instanceof String && !isUploadOnly && !isCancelled()) {
                                mb.isDownloading = true;
                                handler.post(new Runnable() {
                                    public void run() {
                                        mb.progressBar.setProgress(0);
                                        mb.progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.blueprogress));
                                    }
                                });
                                new DownloadMediaAsyncTask().doInBackground(mb);
                                /*
                                startTime = System.currentTimeMillis();
                                new DownloadMediaAsyncTask() {
                                    @Override
                                    protected void onProgressUpdate(Integer... progress) {
                                        super.onProgressUpdate(progress);
                                        try {
                                            long timeInSecs = (System.currentTimeMillis() - startTime) / 1000; 
                                            double speedInKBps = (mUploaded / timeInSecs) / 1024D;
                                            downloadPerSecondTextView.setText(dformat.format(speedInKBps));
                                        } catch (ArithmeticException ae) {
                                        }
                                    }
                                }.doInBackground(mb);
                                 */
                            }
                            return obj;
                        }
                        @Override
                        protected void onProgressUpdate(Integer... progress) {
                            super.onProgressUpdate(progress);
                            try {
                                double timeInSecs = (System.currentTimeMillis() - startTime) / 1000D; 
                                double speedInKBps = (mUploaded / timeInSecs) / 1024D;
                                uploadPerSecondTextView.setText(dformat.format(speedInKBps));
                            } catch (Exception ex) {
                                Log.d(TAG, "DIVIDE EXCEPTION");
                            }
                        }
                        @Override
                        protected void onPostExecute(Object result) {
                            mb.failed = result instanceof Exception;
                            ((ArrayAdapter) ((HeaderViewListAdapter) mListView.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
                            boolean isDone = true;
                            for (DebugBean b : dList) {
                                if (b.isUploading || b.isDownloading) {
                                    isDone = false;
                                    break;
                                }
                            }
                            if (isDone) {
                                runButton.performClick();
                            }
                        }
                    };
                    tList.add(task);
                    mb.isUploading = true;
                    if (debugSerial) {
                        task.execute(bean);
                    } else {
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bean);
                        try {
                            Thread.sleep(30);
                        } catch (Exception ex) {
                        }
                    }
                }
                MainApplication.getTaskCache().clear();
            }
        }).start();
    }


    protected final void onDebugMenuItem(final String title) {
        if (title.endsWith("Performance")) {
            onDebugPerformanceMenuItem();
        } else {
            onDebugAutomationMenuItem();
        }
    }


    private final void onDebugPerformanceMenuItem() {

        final int pad = getResources().getDimensionPixelSize(R.dimen.padding10);
        final int blue = getResources().getColor(R.color.acesse_lightblue);

        ArrayAdapter<DebugBean> adapter = new ArrayAdapter<DebugBean>(getActivity(), android.R.layout.simple_list_item_1, dList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                DebugBean bean = dList.get(position);
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getActivity()).inflate(R.layout.upload_row_item, parent, false);
                    view.setBackgroundColor(0xFFCCCCCC);
                }
                TextView tview = (TextView) view.findViewById(R.id.textView1);
                tview.setTextColor(blue);
                tview.setText(bean.name);
                if (bean.progressBar.getParent() != null) {
                    ((LinearLayout) bean.progressBar.getParent()).removeAllViews();
                }
                ((LinearLayout) view.findViewById(R.id.progressBarContainer)).addView(bean.progressBar, -1, -1);
                if (bean.failed) {
                    tview.setTextColor(Color.RED);
                }
                return view;
            }
        };

        final LinearLayout vlay = new LinearLayout(getActivity());
        vlay.setOrientation(LinearLayout.VERTICAL);
        vlay.setBackgroundResource(R.drawable.rounded_bubble);


        LinearLayout hlay = new LinearLayout(getActivity());
        hlay.setOrientation(LinearLayout.HORIZONTAL);
        hlay.setPadding(pad, pad, pad, pad);

        final RadioGroup group1 = new RadioGroup(getActivity());
        {
            group1.setOrientation(RadioGroup.VERTICAL);
            group1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    debugSerial = checkedId == 111;
                }
            });
            RadioButton rb1 = new RadioButton(getActivity());
            rb1.setId(111);
            rb1.setTextSize(14f);
            rb1.setText("Serial");
            rb1.setChecked(true);
            group1.addView(rb1);
            RadioButton rb2 = new RadioButton(getActivity());
            rb2.setId(222);
            rb2.setTextSize(14f);
            rb2.setText("Parallel");
            group1.addView(rb2);
            hlay.addView(group1, new LinearLayout.LayoutParams(-1,-2,1));
        }

        final RadioGroup group2 = new RadioGroup(getActivity());
        {
            group2.setOrientation(RadioGroup.VERTICAL);
            group2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    uploadSize = checkedId == 333 ? 1048576 : 1048576 * 10;
                }
            });
            RadioButton rb1 = new RadioButton(getActivity());
            rb1.setId(333);
            rb1.setTextSize(14f);
            rb1.setText("1 MB");
            rb1.setChecked(true);
            group2.addView(rb1);
            RadioButton rb2 = new RadioButton(getActivity());
            rb2.setId(444);
            rb2.setTextSize(14f);
            rb2.setText("10 MB");
            group2.addView(rb2);
            hlay.addView(group2, new LinearLayout.LayoutParams(-1,-2,1));
        }

        final RadioGroup group3 = new RadioGroup(getActivity());
        {
            group3.setOrientation(RadioGroup.VERTICAL);
            group3.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    isUploadOnly = checkedId == 555;
                }
            });
            RadioButton rb1 = new RadioButton(getActivity());
            rb1.setId(555);
            rb1.setText("Up Only");
            rb1.setTextSize(14f);
            rb1.setChecked(true);
            group3.addView(rb1);
            RadioButton rb2 = new RadioButton(getActivity());
            rb2.setId(666);
            rb2.setTextSize(14f);;
            rb2.setText("Up & Down");
            group3.addView(rb2);
            hlay.addView(group3, new LinearLayout.LayoutParams(-1,-2,1));
        }

        vlay.addView(hlay, new LinearLayout.LayoutParams(-1,-2,1));

        final LinearLayout speedLay = new LinearLayout(getActivity());
        speedLay.setOrientation(LinearLayout.HORIZONTAL);
        speedLay.setPadding(pad*2, 0, 0, pad/2);
        speedLay.setVisibility(View.GONE);
        {

            LinearLayout vlay2 = new LinearLayout(getActivity());
            vlay2.setOrientation(LinearLayout.VERTICAL);

            TextView tview = new TextView(getActivity());
            tview.setText("UPLOAD");
            tview.setTextSize(12f);
            tview.setPadding(0, 0, pad, 0);
            vlay2.addView(tview, new LinearLayout.LayoutParams(-2,-2));

            hlay = new LinearLayout(getActivity());
            hlay.setOrientation(LinearLayout.HORIZONTAL);
            uploadPerSecondTextView = new TextView(getActivity());
            uploadPerSecondTextView.setText("0.0");
            uploadPerSecondTextView.setTextSize(28f);
            hlay.addView(uploadPerSecondTextView, new LinearLayout.LayoutParams(-2,-2));

            tview = new TextView(getActivity());
            tview.setTextSize(10f);
            tview.setText("kbs");
            hlay.addView(tview, new LinearLayout.LayoutParams(-2,-2));
            vlay2.addView(hlay, new LinearLayout.LayoutParams(-2,-2));
            speedLay.addView(vlay2, new LinearLayout.LayoutParams(-1,-2,1));

            /*
            vlay2 = new LinearLayout(getActivity());
            vlay2.setOrientation(LinearLayout.VERTICAL);
            tview = new TextView(getActivity());
            tview.setText("DOWNLOAD");
            tview.setTextSize(12f);
            vlay2.addView(tview, new LinearLayout.LayoutParams(-2,-2));

            hlay = new LinearLayout(getActivity());
            hlay.setOrientation(LinearLayout.HORIZONTAL);
            downloadPerSecondTextView = new TextView(getActivity());
            downloadPerSecondTextView.setText("0.0");
            downloadPerSecondTextView.setTextSize(28f);
            hlay.addView(downloadPerSecondTextView, new LinearLayout.LayoutParams(-2,-2));

            tview = new TextView(getActivity());
            tview.setTextSize(11f);
            tview.setText("kbs");
            hlay.addView(tview, new LinearLayout.LayoutParams(-2,-2));
            vlay2.addView(hlay, new LinearLayout.LayoutParams(-2,-2));

            speedLay.addView(vlay2, new LinearLayout.LayoutParams(-1,-2,1));
             */
        }
        vlay.addView(speedLay, new LinearLayout.LayoutParams(-1,-2,1));

        hlay = new LinearLayout(getActivity());
        hlay.setOrientation(LinearLayout.HORIZONTAL);
        hlay.setPadding(pad, 0, pad, pad);
        final int dividerHeight = mListView.getDividerHeight();

        final Button exitButton = new Button(getActivity());

        runButton = new Button(getActivity());
        runButton.setGravity(Gravity.CENTER);
        runButton.setText("Start");
        runButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                group1.setVisibility(View.GONE);
                group2.setVisibility(View.GONE);
                group3.setVisibility(View.GONE);
                if (runButton.getText().equals("Start")) {
                    speedLay.setVisibility(View.VISIBLE);
                    startDebugUploadTest();
                    exitButton.setVisibility(View.GONE);
                    runButton.setText("Stop");
                } else {
                    for (AsyncTask task : tList) {
                        task.cancel(true);
                    }
                    exitButton.setVisibility(View.VISIBLE);
                    runButton.setText("Start");
                    group1.setVisibility(View.VISIBLE);
                    group2.setVisibility(View.VISIBLE);
                    group3.setVisibility(View.VISIBLE);
                }
            }
        });
        hlay.addView(runButton, new LinearLayout.LayoutParams(-1,-2,1));

        exitButton.setGravity(Gravity.CENTER);
        exitButton.setText("Exit");
        exitButton.setVisibility(View.GONE);
        exitButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                dList.clear();
                tList.clear();
                mListView.setDividerHeight(dividerHeight);
                mListView.addHeaderView(headerView);
                mListView.removeFooterView(vlay);
                mListView.setBackgroundColor(getResources().getColor(R.color.acesse_lightblue));
                setListAdapter(mAdapter);
                chatBarLayout.setVisibility(View.VISIBLE);
                loadHistory();
                // cleanup...
                new AsyncTask<Void, Void, Void>() {
                    protected Void doInBackground(Void... params) {
                        File dir = MainApplication.getChatStorageDirectory();
                        for (File file : dir.listFiles()) {
                            if (file.getName().endsWith(".perf")) {
                                file.delete();
                            }
                        }
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        hlay.addView(exitButton, new LinearLayout.LayoutParams(-1,-2,1));
        vlay.addView(hlay, new LinearLayout.LayoutParams(-1,-2,1));

        mListView.setDividerHeight(pad/4);
        mListView.addFooterView(vlay);
        mListView.removeHeaderView(headerView);
        mListView.setBackgroundColor(0xFFCCCCCC);
        setListAdapter(adapter);
        chatBarLayout.setVisibility(View.GONE);

        /*
        dList.clear();
        for (int i = 0; i < 5; i++) {
            DebugBean bean = new DebugBean("#" + (i+1), toContact.getName());
            bean.progressBar = (ProgressBar) LayoutInflater.from(getActivity()).inflate(R.layout.upload_progressbar, null, false);
            bean.localPath = new File(MainApplication.getChatStorageDirectory(), "test-" + i + ".perf").getPath();
            bean.isUploading = true;
            dList.add(bean);
        }
        */
        ((ArrayAdapter) ((HeaderViewListAdapter) mListView.getAdapter()).getWrappedAdapter()).notifyDataSetChanged();
    }



    protected final void onDebugAutomationMenuItem() {
        if (debugAutomationView == null) {
            debugAutomationView = LayoutInflater.from(getActivity()).inflate(R.layout.debug_automation, null, false);
            ((SeekBar) debugAutomationView.findViewById(R.id.seekBar1)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    ((TextView) debugAutomationView.findViewById(R.id.num_iterations_text)).setText(String.valueOf(progress * 5));
                }
            });
            debugAutomationView.setBackgroundColor(0xFFCCCCCC);
            debugAutomationView.findViewById(R.id.start_button).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (automationThread != null && automationThread.isAlive()) {
                        synchronized (debugWaitLock) {
                            debugWaitLock.notifyAll();
                        }
                        automationThread.interrupt();
                        automationThread = null;
                        ((Button) v).setText("Start");
                    } else {
                        ((Button) v).setText("Stop");
                        ((Button) v).setBackgroundResource(R.drawable.rounded_bubble);
                        startAutomation(null, ((SeekBar) debugAutomationView.findViewById(R.id.seekBar1)).getProgress() * 5);
                        debugAutomationView.findViewById(R.id.close_button).performClick();
                    }
                }
            });
            debugAutomationView.findViewById(R.id.close_button).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    mListView.removeHeaderView(debugAutomationView);
                    mListView.addHeaderView(headerView);
                    mListView.setBackgroundColor(getResources().getColor(R.color.acesse_lightblue));
                    mListView.setDividerHeight(getResources().getDimensionPixelSize(R.dimen.padding10));
                    setListAdapter(mAdapter);
                    chatBarLayout.setVisibility(View.VISIBLE);
                    loadHistory();
                }
            });
        }
        mListView.removeHeaderView(headerView);
        mListView.addHeaderView(debugAutomationView);
        mListView.setBackgroundColor(0xFFCCCCCC);
        mListView.setDividerHeight(0);
        if (messageBeans.size() == 0) {
            //messageBeans.add(new MessageBean(-1, null, null, null, 0, null, LinphoneChatMessage.State.InProgress, true, true));
        }
        ArrayAdapter<MessageBean> adapter = new ArrayAdapter<MessageBean>(getActivity(), android.R.layout.simple_list_item_1, messageBeans) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                MessageBean bean = messageBeans.get(position);
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.automation_row_item, parent, false);
                if (bean.id == -1) {
                    view.setVisibility(View.GONE);
                    return view;
                }
                String type = bean.externalBodyUrl == null ? "TXT" : bean.externalBodyUrl.endsWith(".jpg") ? "JPG" : "MP4";
                ((TextView) view.findViewById(R.id.msg_id)).setText(String.valueOf(bean.id));
                ((TextView) view.findViewById(R.id.msg_type)).setText(type);
                if (bean.deliveryDuration != 0 && bean.deliveryDuration < 90000) {
                    ((TextView) view.findViewById(R.id.delivery_duration)).setText(bean.deliveryDuration + " ms");
                }
                if (bean.uploadDuration != 0 && bean.uploadDuration < 1800000) {
                    ((TextView) view.findViewById(R.id.media_duration)).setText(bean.uploadDuration + " ms");
                } else if (bean.downloadDuration != 0 && bean.downloadDuration < 1500000) {
                    ((TextView) view.findViewById(R.id.media_duration)).setText(bean.uploadDuration + " ms");
                }
                return view;
            }
        };
        setListAdapter(adapter);
        chatBarLayout.setVisibility(View.GONE);
    }



    protected final void startAutomation(final String cmd, final int num) {
        
        if (cmd != null && cmd.equals("stats")) {
            Log.d(TAG, "-----------------------------STATISTICS");
            for (MessageBean bean : messageBeans) {
                String type = bean.externalBodyUrl == null ? "TXT" : bean.externalBodyUrl.endsWith(".jpg") ? "JPG" : "MP4";
                Log.d(TAG, bean.id + " TYPE: " + type + " STATE: " + bean.state + " DELIVERY: " + bean.deliveryDuration + " UPLOAD: " + bean.uploadDuration);
                return;
            }
        };

        messageBeans.clear();

        final TextView tview = (TextView) getActivity().findViewById(R.id.network_unreachable_text);
        tview.setText("Debug Automation Running");
        tview.setVisibility(View.VISIBLE);

        final List<String> runList = new ArrayList<String>();
        if (cmd != null && !cmd.isEmpty()) {
            runList.add(cmd);
        } else {
            if (((CheckBox) debugAutomationView.findViewById(R.id.text_cb)).isChecked()) {
                runList.add("text");
            } 
            if (((CheckBox) debugAutomationView.findViewById(R.id.image_cb)).isChecked()) {
                runList.add("image");
            } 
            if (((CheckBox) debugAutomationView.findViewById(R.id.photo_cb)).isChecked()) {
                runList.add("photo");
            } 
            if (((CheckBox) debugAutomationView.findViewById(R.id.video_cb)).isChecked()) {
                runList.add("video");
            } 
        }

        Runnable runner = new Runnable() {
            public void run() {
                for (int i = 0; i < num && !hidden && getActivity() != null && automationThread != null; i++) {
                    Collections.shuffle(runList);
                    Log.d(TAG, "-----------------------------RUNNING " + (i+1) + " of " + num);
                    final String text = "Text: " + String.valueOf(i+1);
                    final String test = runList.get(0);

                    if (test.equals("image")) {
                        String fileName = Math.random() < .5 ? "ferrari1" : "ferrari2";
                        final File file = new File(MainApplication.getChatStorageDirectory(), fileName + ".jpg");
                        if (!file.exists()) {
                            try {
                                int res = fileName.endsWith("1") ? R.drawable.ferrari1 : R.drawable.ferrari2;
                                BufferedInputStream bis = new BufferedInputStream(getResources().openRawResource(res));
                                OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                                byte[] data = new byte[1024 * 8];
                                int bytesRead = 0;
                                while ((bytesRead = bis.read(data)) > 0) {
                                    os.write(data, 0, bytesRead);
                                }
                                os.close();
                                bis.close();
                            } catch (Exception ex) {
                                Log.w(TAG, "Failed to copy file " + file);
                            }
                        }
                        mAttachPaths.add(file.getAbsolutePath());
                    } else if (test.equals("photo") || test.equals("video")) {
                        Class klass = test.equals("photo") ? ImageCaptureActivity.class : VideoCaptureActivity.class;
                        int request = test.equals("photo") ? REQUEST_PHOTO : REQUEST_VIDEO;
                        startActivityForResult(new Intent(getActivity(), klass).putExtra("AUTOMATION", true), request);
                        // wait for video or photo to be captured...
                        try {
                            synchronized (debugWaitLock) {
                                debugWaitLock.wait(20000);
                                Thread.currentThread().sleep(1000);
                            }
                        } catch (Exception ex) {
                            Log.w(TAG, "Debug Wait Lock Exception. Exiting Test!");
                            break;
                        }
                    }

                    if (getActivity() != null && automationThread != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                if (attachmentsPopup != null) {
                                    attachmentsPopup.dismiss();
                                    attachmentsPopup = null;
                                }
                                if (test.equals("text")) {
                                    editText.setText(text);
                                    sendTextMessage();
                                } else {
                                    sendMediaMessage();
                                }
                                if (mList.size() > 0) {
                                    messageBeans.add(mList.get(mList.size()-1));
                                }
                            }
                        });
                        try {
                            // wait for upload and delivery...
                            synchronized (debugWaitLock) {
                                debugWaitLock.wait(90000);
                            }
                            Thread.currentThread().sleep(1000);
                        } catch (Exception ex) {
                            Log.w(TAG, "Debug Wait Lock Exception. Exiting Test!");
                            break;
                        }
                    }
                }
                Log.d(TAG, "-----------------------------TESTING DONE");
                automationThread = null;
                if (getActivity() != null)
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        onDebugAutomationMenuItem();
                        if (debugAutomationView != null) {
                            ((TextView) debugAutomationView.findViewById(R.id.start_button)).setText("Start");
                        }
                        tview.setVisibility(View.GONE);
                        tview.clearAnimation();
                    }
                });
            }
        };
        automationThread = new Thread(runner);
        automationThread.start();
    }
} 