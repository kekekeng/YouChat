package com.acesse.youchat;

import com.crittercism.app.Crittercism;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class AcesseTabFragment extends Fragment{
    
    private TextView learnMoreTextView;
    private TextView becomeConsultantTextView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.acesse_tab_layout, container, false);
        
        Crittercism.leaveBreadcrumb("AcesseTab : onCreateView");
        
        learnMoreTextView = (TextView) view.findViewById(R.id.learn_more_text_view);
        becomeConsultantTextView = (TextView) view.findViewById(R.id.become_consultant_text_view);
        
        learnMoreTextView.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://marketing.acesse.com")));
            }
        });
        
        
        becomeConsultantTextView.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://marketing.acesse.com/public/careers.php")));
            }
        });
        return view;
    }
    
    

}
