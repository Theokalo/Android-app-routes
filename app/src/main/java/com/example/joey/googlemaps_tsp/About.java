package com.example.joey.googlemaps_tsp;

import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class About extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.about);

        TextView mLink = (TextView) findViewById(R.id.link);
        mLink.setText(Html.fromHtml(getString(R.string.link)));
        /*if (mLink != null) {*/
            mLink.setMovementMethod(LinkMovementMethod.getInstance());
        //}
    }
    public void onBackPressed() {
        finish();
    }
}
