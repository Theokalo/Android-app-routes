package com.example.joey.googlemaps_tsp;


import android.app.Activity;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.EditText;
import android.widget.TextView;

public class Genetic_Algorithms extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.genetic_algorithms);

        TextView ga = (TextView) findViewById(R.id.ga);
        TextView ga1 = (TextView) findViewById(R.id.ga1);
    }
}
