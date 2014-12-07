package com.acesse.youchat;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;

import com.acesse.youchat.contentprovider.YouChatContactContentProvider;
import com.crittercism.app.Crittercism;



public class ContactDAO {

    /*
    private static final String TAG = "YOUC";
    private static ContactDAO instance;
    private static final int DEFAULT_CACHE_SIZE = 1024 * 1024 * 3; // 3 mb
    private ContentResolver contentResolver;
    //private final List<Contact> contacts = new ArrayList<Contact>();
    //private Contact accountContact, registrarContact;
    private String acctId;
    private Bitmap contactImagePlaceholderBitmap;
    private LruCache<Integer, Bitmap> mBitmapCache;


    public Contact getAccountUser() {
        return accountContact;
    }

    public Contact getRegistrarContact() {
        return registrarContact;
    }


    public String getAccountFullName() {
        if (accountContact == null){
            return null;
        }else {
            return accountContact.getFirstName() + " " + accountContact.getLastName();
        }
    }

    public void setAccountContact(Contact accountContact) {
        this.accountContact = accountContact;
    }


    public synchronized static final ContactDAO getInstance() {
        if (instance == null){
            if (Config.DEBUG) {
                Log.d(TAG, "ContactDAO: instance is null creating:ContactDAO");
            }
            instance = new ContactDAO();
            instance.contentResolver = MainApplication.getInstance().getContentResolver();
        }
        return instance;
    }

*/

    /**
     * Initialize contacts DAO by reading contacts from DB, putting them into memory for 
     * fast access, and establish presence.  NOTE: this init must be called from a thread or
     * asynctask.
     * 
     * @param youchatId
     */
    /*
    public Object init(String acctId) {

        this.acctId = acctId;

        mBitmapCache = new LruCache<Integer, Bitmap>(DEFAULT_CACHE_SIZE) {
            @Override
            protected int sizeOf(Integer key, Bitmap bmap) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? bmap.getAllocationByteCount() : bmap.getByteCount();
            }
            @Override
            protected void entryRemoved(boolean evicted, Integer key, Bitmap oldBitmap, Bitmap newBitmap) {
                if (!oldBitmap.isRecycled()) {
                    oldBitmap.recycle();
                }
            }
        };

        if (Config.DEBUG) {
            Log.d(TAG,"ContactDAO.init " + acctId);
        }

        registrarContact = new Contact("registrar", "registrar", Uri.parse("android.resource://com.acesse.youchat/drawable/ic_launcher"));
        registrarContact.setFirstName("YouChat");
        registrarContact.setLastName("Admin");

        getContactsFromDatabase(acctId);
        // we're already in a thread so execute synchronously...
        //
        if (getContactCount() > 0){
            new GetAccountsAsyncTask().doInBackground();
        }

        for (Contact c: contacts) {
            addPresence(c);
            try {
                // wait to allow presence for each user
                Thread.sleep(100);
            } catch (Exception ex) {
            }
        }
        //Populate account information for logged in user
        Object res = new GetAccountAsyncTask().doInBackground(acctId);
        accountContact = new Contact("acct", acctId);
        if (res instanceof Exception) {
            Log.e(TAG, "Error fetching Account Details");
            // set at least the first name to the account id...
            accountContact.setFirstName(acctId);
        } else if (res instanceof Contact) {
            Contact c = (Contact) res;
            accountContact.setFirstName(c.getFirstName());
            accountContact.setLastName(c.getLastName());
            accountContact.setCompany(c.getCompany());
            accountContact.setEmail(c.getEmail());
            accountContact.setPhotoUri(c.getPhotoUri());

            if (accountContact.getPhotoUri() != null && getProfilePhoto(accountContact.getPhotoUri().toString()) == null) {
                fetchProfilePhoto(accountContact.getPhotoUri().toString());
            }
        }
        return res;
    }


    public List<Contact> getContacts(){
        return contacts;
    }

    public int getContactCount() {
        return contacts.size();
    }

    public void addContact(Contact c){
        addContactInDatabase(c);
        contacts.add(c);
        addPresence(c);
    }

    public void addPresence(Contact c) {
        try {
            LinphoneFriend lf = LinphoneCoreFactory.instance().createLinphoneFriend("sip:"+ c.getName() + "@"+ Config.DOMAIN_XMPP );
            lf.edit();
            lf.enableSubscribes(true);
            lf.setIncSubscribePolicy(SubscribePolicy.SPAccept);
            lf.done();
            LinphoneManager.getLc().addFriend(lf);
            c.setFriend(lf);
        } catch (LinphoneCoreException ex) {
            Log.w(TAG, "Failed to establish presence", ex);
        }
    }

    public void removeContact(Contact c){
        removeContactFromDatabase(c);
        contacts.remove(c);
        removePresence(c);
    }

    public void removePresence(Contact c) {
        if (c.getFriend() != null) {
            LinphoneManager.getLc().removeFriend(c.getFriend());
            c.setFriend(null);
        }
    }

    public void updateContact(Contact c){
        updateContactInDatabase(c);
    }

    public Contact getContact(String name) {
        for (Contact contact : contacts) {
            if (contact.getName().equalsIgnoreCase(name)) {
                return contact;
            }
        }
        return null;
    }

    public void addContactInDatabase(Contact c){
        ContentValues values = new ContentValues();
        values.put(YouChatContactsTable.COLUMN_YOUCHATID, acctId);
        values.put(YouChatContactsTable.COLUMN_CONTACT,c.getName());
        if (Config.DEBUG) {
            Log.d(TAG,"ContactDAO: addContacts:id:" + values.toString());
        }
        instance.contentResolver.insert(YouChatContactContentProvider.CONTENT_URI,values);
    }

    public void removeContactFromDatabase(Contact c){
        String   selection     = YouChatContactsTable.COLUMN_YOUCHATID +"=? AND " + YouChatContactsTable.COLUMN_CONTACT + "=? ";
        String[] selectionArgs = new String[]{ acctId, c.getName()};
        if (Config.DEBUG) {
            Log.d(TAG,"ContactDAO removeContact:id");
        }
        instance.contentResolver.delete(YouChatContactContentProvider.CONTENT_URI, selection, selectionArgs);
        File dir = new File(MainApplication.getInstance().getFilesDir(), "contact");
        File file = new File(dir, c.getName());
        file.delete();
    }

    public void updateContactInDatabase(Contact c){
        String   selection     = YouChatContactsTable.COLUMN_YOUCHATID +"=? AND " + YouChatContactsTable.COLUMN_CONTACT + "=? ";
        String[] selectionArgs = new String[]{ acctId, c.getName()};
        ContentValues values = new ContentValues();
        if(c.getPhotoUri() != null){
            values.put(YouChatContactsTable.COLUMN_CONTACT_PICTURE_URL, c.getPhotoUri().toString());
        }
        values.put(YouChatContactsTable.COLUMN_LAST_MODIFIED_DATE, c.getLastModifiedDate());
        //ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //values.put(YouChatContactsTable.COLUMN_CONTACT_PICTURE,  c.getPhoto().compress(CompressFormat.JPEG, 100, baos));
        instance.contentResolver.update(YouChatContactContentProvider.CONTENT_URI, values, selection, selectionArgs);

    }

    private List<Contact> getContactsFromDatabase(String youchatid){
        contacts.clear();
        String[] projection    = {YouChatContactsTable.COLUMN_ID,YouChatContactsTable.COLUMN_YOUCHATID, YouChatContactsTable.COLUMN_CONTACT, YouChatContactsTable.COLUMN_CONTACT_PICTURE_URL };
        String   selection     = YouChatContactsTable.COLUMN_YOUCHATID +"=?";
        String[] selectionArgs = new String[]{youchatid};
        Cursor cur = instance.contentResolver.query(YouChatContactContentProvider.CONTENT_URI, projection, selection, selectionArgs,null);
        if (Config.DEBUG) {
            Log.d(TAG,"ContactDAO: getContacts called");
        }
        if (cur != null) {
            for(cur.moveToFirst();!cur.isAfterLast();cur.moveToNext()){
                Contact contact = new Contact(cur.getString(0), cur.getString(2));
                if(cur.getString(3) != null){
                    contact.setPhotoUri(Uri.parse(cur.getString(3)));
                }
                if (Config.DEBUG) {
                    Log.d(TAG,"ContactDAO: getContacts:id:" + cur.getString(0) + " youchatId:" + cur.getString(1) + " contact:" + cur.getString(2));
                }
                contacts.add(contact);
            }
            cur.close();
        }
        return contacts;
    }



    public void updateContact(JSONObject jobj) {

        final String aid = jobj.optString("account_id");
        boolean isNewContact = false;
        String previousModifiedDate = null;


        Contact contact = getContact(aid);

        if (contact == null) {
            //
            // User may have added this contact from another app.
            //
            contact = new Contact(UUID.randomUUID().toString(), aid);
            contact.setEmail(jobj.optString("profile_email"));
        }

        if(contact.getLastModifiedDate() != null && !contact.getLastModifiedDate().isEmpty())
        {
            previousModifiedDate = contact.getLastModifiedDate();
        }

        contact.setFirstName(jobj.optString("first_name", ""));

        contact.setLastName(jobj.optString("last_name", ""));

        contact.setCompany(jobj.optString("company", ""));

        contact.setLastModifiedDate(jobj.optString("last_modified_date", ""));

        final String pic_url = jobj.optString("profile_picture_url");

        if (pic_url != null && !pic_url.isEmpty()) {
            contact.setPhotoUri(Uri.parse(pic_url));
        }

        if (pic_url != null && !pic_url.isEmpty()) {
            File file = new File(MainApplication.getInstance().getFilesDir() + "/contact", pic_url.substring(pic_url.lastIndexOf("/")+1));
            if (!file.exists()) {
                fetchProfilePhoto(pic_url);
            }
        }

        if (isNewContact) {
            addContact(contact);
        } else if (contact.getLastModifiedDate() != null && !contact.getLastModifiedDate().equals(previousModifiedDate)) {
            updateContact(contact);
        }
    }


    public LruCache<Integer, Bitmap> getBitmapCache() {
        return mBitmapCache;
    }


    public final Bitmap getPlaceholderBitmap() {
        Resources resources = MainApplication.getInstance().getResources();
        if (contactImagePlaceholderBitmap == null) {
            contactImagePlaceholderBitmap = BitmapFactory.decodeResource(resources, R.drawable.contact_image_placeholder);
        }
        return contactImagePlaceholderBitmap;
    }


    public final Bitmap getProfilePhoto(Uri photoUri) {
        if (photoUri == null) {
            return getPlaceholderBitmap();
        }
        return getProfilePhoto(photoUri.toString());
    }


    public final Bitmap getProfilePhoto(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty() || mBitmapCache == null) {
            return getPlaceholderBitmap();
        }
        Bitmap bmap = mBitmapCache.get(photoUrl.hashCode());
        if (bmap != null) {
            return bmap;
        }

        File dir = new File(MainApplication.getInstance().getFilesDir(), "contact");
        File file = new File(dir, photoUrl.substring(photoUrl.lastIndexOf("/")+1));
        if (Config.DEBUG) {
            Log.d(TAG, "ContactDAO.getProfilePhoto " + file + (file.exists() ? " EXISTS" : " DOES NOT EXIST"));
        }
        InputStream in = null;
        try {
            if (file.exists()) {
                bmap = MainApplication.getImageThumbnail(file.getAbsolutePath(), false);
            } else if (photoUrl.startsWith("android.resource")) {
                bmap = MainApplication.createRoundedBitmap(BitmapFactory.decodeStream(in = instance.contentResolver.openInputStream(Uri.parse(photoUrl))));
            }
            if (bmap != null) {
                mBitmapCache.put(photoUrl.hashCode(), bmap);
                return bmap;
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to decode profile photo", ex);
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
            }
        }
        return getPlaceholderBitmap();
    }


    public void fetchProfilePhoto(String photoUrl) {
        if (Config.DEBUG) {
            Log.d(TAG, "FETCHING PROFILE PHOTO " + photoUrl);
        }
        File file = new File(MainApplication.getContactStorageDirectory(), photoUrl.substring(photoUrl.lastIndexOf("/")+1));
        String pathName = file.getAbsolutePath();
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            long startTime = System.currentTimeMillis();
            URL url = new URL(photoUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            in = conn.getInputStream();

            if (Config.DEBUG) {
                Log.d(TAG, "STORING PROFILE PHOTO " + pathName);
            }

            OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
            byte[] data = new byte[1024 * 8];
            int bytesRead = 0;
            while ((bytesRead = in.read(data)) > 0) {
                os.write(data, 0, bytesRead);
            }
            os.close();

            if (Config.DEBUG) {
                Log.d(TAG, "PHOTO SIZE: " + file.length() + " FETCH TIME: " + (System.currentTimeMillis()-startTime) + " ms");
            }
        } catch (Exception ex) {
            Log.w(TAG, "Failed to fetch|write profile photo " + photoUrl);
            return;
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
            }
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = Helper.getSampleSize(pathName, 400);
            opts.inDither = false;
            opts.inScaled = false;
            //opts.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bmap = BitmapFactory.decodeFile(pathName, opts);
            bmap = Helper.rotateImage(pathName, bmap);

            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            bmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();
            
            bmap.recycle();

        } catch (Exception ex) {
            Log.w(TAG, "Failed to store profile photo " + pathName);
        }
    }


    public static void cleanup(){
        if (Config.DEBUG) {
            Log.d(TAG, "ContactDAO.cleanup");
        }
        //Release resources
        if (instance != null) {
            for (Contact c: instance.contacts) {
                instance.removePresence(c);
            }
            instance.contacts.clear();
            instance.mBitmapCache.evictAll();
        }
        instance = null;
    }


    public void testdb(){
        Contact c = new Contact("1","jingle1");
        Contact c1 = new Contact("2","jingle2");
        ContactDAO.getInstance().addContact(c);
        ContactDAO.getInstance().addContact(c1);
        ContactDAO.getInstance().getContactsFromDatabase(acctId);
        ContactDAO.getInstance().removeContact(c1);
        ContactDAO.getInstance().getContactsFromDatabase(acctId);
    }

*/
}
