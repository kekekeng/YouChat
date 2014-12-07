package com.acesse.youchat;

import com.crittercism.app.Crittercism;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class AboutActivity extends Activity {

    private static final String TAG = "YOUC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about_layout);
        
        getActionBar().setTitle(getResources().getString(R.string.menu_about));
        
        Crittercism.leaveBreadcrumb("AboutActivity : OnCreate");
        
        ((TextView) findViewById(R.id.env_text_view)).setText(Config.DOMAIN_NODEJS);

        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionString = "Version " + pinfo.versionName + " (" + pinfo.versionCode + ") ";
            versionString += Config.DEBUG ? "DEBUG " : "";
            versionString += Config.DOMAIN_XMPP.substring(0, Config.DOMAIN_XMPP.indexOf("-"));
            ((TextView) findViewById(R.id.version_text_view)).setText(versionString);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "FAILED TO GET PACKAGE INFO");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.you_chat_main, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
            	Crittercism.leaveBreadcrumb("AboutActivity: Logout");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                prefs.edit().remove("pass").commit();
                //stopService(new Intent(AboutActivity.this, YouChatService.class));
                finishAffinity();
                startActivity(new Intent(getApplicationContext(), YouChatLoginActivity.class).putExtra("logout", true));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    } 
    
}
