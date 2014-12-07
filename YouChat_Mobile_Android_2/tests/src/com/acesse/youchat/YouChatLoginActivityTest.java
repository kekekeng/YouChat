package com.acesse.youchat;

import com.acesse.youchat.YouChatLoginActivity;

import android.test.ActivityInstrumentationTestCase2;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.ViewAsserts;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

//import com.vogella.android.test.simpleactivity.R;



/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.acesse.youchat.YouChatLoginActivityTest \
 * com.acesse.youchat.tests/android.test.InstrumentationTestRunner
 */
public class YouChatLoginActivityTest extends ActivityInstrumentationTestCase2<YouChatLoginActivity> {

    private static final String TAG = "YOUC";

    private YouChatLoginActivity activity;
    
    
    public YouChatLoginActivityTest() {
        super("com.acesse.youchat", YouChatLoginActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(false);
        activity = getActivity();
    }


    public void testLoginActivity() throws Exception {

        ActivityMonitor monitor = getInstrumentation().addMonitor(YouChatHomeActivity.class.getName(), null, false);

        TextView userText = (TextView) activity.findViewById(R.id.username_text);
        userText.setText("bjorn2");
        TextView passText = (TextView) activity.findViewById(R.id.password_text);
        passText.setText("aaaa1111");

        Thread.sleep(2000);

        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                activity.findViewById(R.id.login_button).performClick();
            }
        });
        getInstrumentation().waitForIdleSync();
        YouChatHomeActivity startedActivity = (YouChatHomeActivity) monitor.waitForActivityWithTimeout(60000);
        assertNotNull(startedActivity);

        this.sendKeys(KeyEvent.KEYCODE_BACK);
    }


    public void testCreateAccountActivity() throws Exception {

        Thread.sleep(2000);

        ActivityMonitor monitor = getInstrumentation().addMonitor(YouChatCreateAccountActivity.class.getName(), null, false);

        final TextView view = (TextView) activity.findViewById(R.id.create_account_button);
        getInstrumentation().runOnMainSync(new Runnable() {
            public void run() {
                view.performClick();
            }
        });

        getInstrumentation().waitForIdleSync();

        // wait 2 seconds for the start of the activity
        YouChatCreateAccountActivity startedActivity = (YouChatCreateAccountActivity) monitor.waitForActivityWithTimeout(3000);
        Log.d(TAG, "STARTED ACTIVITY: " + startedActivity);

        assertNotNull(startedActivity);

        TextView textView = (TextView) startedActivity.findViewById(R.id.create_account_button);

        // check that the TextView is on the screen
        ViewAsserts.assertOnScreen(startedActivity.getWindow().getDecorView(), textView);
        // validate the text on the TextView
        assertEquals("Text incorrect", "Create Account", textView.getText().toString());

        this.sendKeys(KeyEvent.KEYCODE_BACK);
        
        Thread.sleep(2000);
    }
}