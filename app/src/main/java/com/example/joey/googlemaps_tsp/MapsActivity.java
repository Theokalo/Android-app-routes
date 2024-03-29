package com.example.joey.googlemaps_tsp;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity {

    private Boolean EMULATOR; // To slow down app. if running on Emulator (see "emulator" in manifest)
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private final int THRESHOLD = 30; // Max number of destinations on the map
    private boolean solveInProgress = false; // Flag for GA_Task or SA_Task being in progress
    private AsyncTask solverTask; // Reference to the GA_Task or SA_Task that is in progress
    private ArrayList<Polyline> polylines = new ArrayList();
    private int publishInterval = 333; // defines publishing rate in milliseconds

    // initialize options drawer
    private SharedPreferences mSharedPreferences;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;

    protected void onCreate(Bundle savedInstanceState) {

        // Setup map
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled(true);

        // Zoom to Current Location
        // Get LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();                                     // Create a criteria object to retrieve provider
        String provider = locationManager.getBestProvider(criteria, true);      // Get the name of the best provider
        Location myLocation = locationManager.getLastKnownLocation(provider);   // Get Current Location
        double latitude = myLocation.getLatitude();                             // Get latitude of the current location
        double longitude = myLocation.getLongitude();                           // Get longitude of the current location
        LatLng latLng = new LatLng(latitude, longitude);                        // Create a LatLng object for the current location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));                 // Show the current location in Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));                     // Zoom in the Google Map

        // Setup options & drawer
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        addDrawerItems(R.array.menuItems, mDrawerList);
        mDrawerList.setOnItemClickListener(new SideDrawerClickListener());

        // Adjust application behaviour based on emulator or device (see "emulator" in manifest)
        ApplicationInfo ai = null;
        try {
            ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            EMULATOR = bundle.getBoolean("emulator", false);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Helper Method
    private void addDrawerItems(int listArr, ListView view) {
        String[] mOptionsNames;
        mOptionsNames = getResources().getStringArray(listArr);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mOptionsNames);
        view.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private class SideDrawerClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    //** Swaps fragments in the main content view */
    private void selectItem(int position) {
        switch (position) {
            case 0:
                Intent about = new Intent(this, About.class);
                startActivity(about);
                break;
            case 1:
                Intent ga = new Intent(this, Genetic_Algorithms.class);
                startActivity(ga);
                break;
            case 2:
                // Options
                Intent myIntent = new Intent(this, SettingsActivity.class);
                startActivity(myIntent);
                break;
        }
    }

    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link com.google.android.gms.maps.SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {

        // Setting a click event handler for the map
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (TourManager.numberOfDestinations() >= THRESHOLD || solveInProgress) {
                    return;
                }
                // Creating a marker
                MarkerOptions markerOptions = new MarkerOptions();

                // Setting the position for the marker
                markerOptions.position(latLng);

                // Setting the title for the marker.
                // This will be displayed on taping the marker
                markerOptions.title(latLng.latitude + " : " + latLng.longitude);

//                // Animating to the touched position
//                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                // Placing a marker on the touched position
                Marker marker = mMap.addMarker(markerOptions);
                // Add the new Destination/Marker to TourManager and to mapMarkers
                TourManager.addDestination(new Destination(marker));
            }
        });

        // Disable clicking on markers
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });

    }
    public void TSP_SA(View v)
    {

            switch (v.getId()) {
                case R.id.graphSAButton:
                    mMap.clear();

                    String url;

                    MarkerOptions markerOptions = new MarkerOptions();

                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();                                     // Create a criteria object to retrieve provider
                    String provider = locationManager.getBestProvider(criteria, true);      // Get the name of the best provider
                    Location myLocation = locationManager.getLastKnownLocation(provider);   // Get Current Location
                    double latitude = myLocation.getLatitude();                             // Get latitude of the current location
                    double longitude = myLocation.getLongitude();                           // Get longitude of the current location
                    LatLng latLng = new LatLng(latitude, longitude);                        // Create a LatLng object for the curren
                    markerOptions.position(latLng).title("My Position");
                    Marker marker = mMap.addMarker(markerOptions);
                    TourManager.addDestination(new Destination(marker));

                    LatLng parthenon = new LatLng(37.9715, 23.7267);
                    markerOptions.position(parthenon).title("Parthenon");
                    marker = mMap.addMarker(markerOptions);
                    TourManager.addDestination(new Destination(marker));

                    LatLng zappeion = new LatLng(37.971133, 23.736506);
                    markerOptions.position(zappeion).title("Zappeion");
                    marker = mMap.addMarker(markerOptions);
                    TourManager.addDestination(new Destination(marker));

                    LatLng zeus = new LatLng(37.969407, 23.732719);
                    markerOptions.position(zeus).title("Temple of Zeus");
                    marker = mMap.addMarker(markerOptions);
                    TourManager.addDestination(new Destination(marker));

                    LatLng acropolis = new LatLng(37.971421, 23.726166);
                    markerOptions.position(acropolis).title("Acropolis");
                    marker = mMap.addMarker(markerOptions);
                    TourManager.addDestination(new Destination(marker));

                    LatLng pan_stadium = new LatLng(37.968519, 23.74089);
                    markerOptions.position(pan_stadium).title("Panathinaiko Stadio");
                    marker = mMap.addMarker(markerOptions);

                    TourManager.addDestination(new Destination(marker));

                    LatLng acropolis_Museum = new LatLng(37.9677153958, 23.723754105);
                    markerOptions.position(acropolis_Museum).title("Acropolis Museum");

                    marker = mMap.addMarker(markerOptions);

                    TourManager.addDestination(new Destination(marker));

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(acropolis_Museum));                 // Show the current location in Google Map
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                    Toast.makeText(MapsActivity.this, "Showing Sights", Toast.LENGTH_LONG).show();
                    break;
            }

    }


        /**
         * This is where we can add markers or lines, add listeners or move the camera. In this case, we
         * just add a marker near Africa.
         * <p/>
         * This should only be called once and when we are sure that {@link #mMap} is not null.
         */


        public void clearMap(View view) {

            // if Async in progress, need to press twice to clear map
            if (solveInProgress) solverTask.cancel(true);
            else {
                mMap.clear();
                TourManager.removeAll();
                System.gc();
            }
        }

        public void graphMap(Tour tour) {

            ArrayList<Marker> markers = new ArrayList<Marker>();
            for (Destination D : tour.getAllDest()) {
                markers.add(D.getMarker());
            }

            if (markers.size() == 0) return;

            // remove existing polylines
            for (Polyline pl : polylines) {
                pl.remove();
            }

            ArrayList<LatLng> latLngs = new ArrayList<>();
            for (Marker M : markers) {
                latLngs.add(M.getPosition());
            }
            latLngs.add(markers.get(0).getPosition());
            PolylineOptions poly = new PolylineOptions().addAll(latLngs);

            poly.color(Color.BLUE);

            polylines.add(mMap.addPolyline(poly));
        }

        /*public void TSP_SA(View view) {
            if (TourManager.numberOfDestinations() == 0 || solveInProgress) return;

            SA_Task task = new SA_Task();
            solverTask = task;
            task.execute();

            System.gc();
        }*/

        public void TSP_GA(View view) {
            if (TourManager.numberOfDestinations() == 0 || solveInProgress) return;

            GA_Task task = new GA_Task();
            solverTask = task;
            task.execute();

            System.gc();
        }

        // solves and displays TSP using GA
        class GA_Task extends AsyncTask<Void, Tour, Population> {

            Tour bestTourSoFar;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                solveInProgress = true;

                //change color of button to indicate processing
                Button GA_button = (Button) findViewById(R.id.graphGAButton);
                GA_button.setBackgroundColor(0xb0FF9933);

                // change text of clear button
                Button button = (Button) findViewById(R.id.clearButton);
                button.setText("STOP");
            }

            @Override
            protected Population doInBackground(Void... voids) {

                // Initialization
                int popSize = Integer.parseInt(mSharedPreferences.getString("popSize", "50"));
                int generations = Integer.parseInt(mSharedPreferences.getString("generations", "200"));

                Population pop = new Population(popSize, true);
                Tour fittest = pop.getFittest();
                publishProgress(fittest);

                long time = System.currentTimeMillis();
                long lastPublishTime = time;

                for (int i = 0; i < generations; i++) {
                    if (isCancelled()) break;

                    time = System.currentTimeMillis();
                    pop = GA.evolvePopulation(pop, mSharedPreferences);
                    bestTourSoFar = new Tour(pop.getFittest());
                    if (time - lastPublishTime > publishInterval) {
                        lastPublishTime = time;
                        publishProgress(pop.getFittest());
                    }

                    if (EMULATOR) {
                        try {
                            Thread.sleep(2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return pop;
            }

            @Override
            protected void onProgressUpdate(Tour... tours) {
                super.onProgressUpdate(tours[0]);
                Tour currentBest = tours[0];
                graphMap(currentBest);
                System.out.println("Current distance: " + currentBest.getDistance());
            }

            @Override
            protected void onPostExecute(Population pop) {
                super.onPostExecute(pop);
                Tour fittest = pop.getFittest();
                graphMap(fittest);
                System.out.println("GA Final distance: " + pop.getFittest().getDistance());

                // Display final distance
                TextView tv1 = (TextView) findViewById(R.id.final_distance);
                int finalDistance = (int) pop.getFittest().getDistance();
                tv1.setText("FINAL DISTANCE: " + finalDistance + " km");

                //change color of button to indicate finish
                Button GA_button = (Button) findViewById(R.id.graphGAButton);
                GA_button.setBackgroundColor(0xb0ffffff);

                // change text of clear button
                Button button = (Button) findViewById(R.id.clearButton);
                button.setText("CLEAR");

                pop = null;
                solveInProgress = false;
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();

                // Display final distance
                TextView tv1 = (TextView) findViewById(R.id.final_distance);
                int finalDistance = (int) bestTourSoFar.getDistance();
                tv1.setText("FINAL DISTANCE: " + finalDistance + " km");

                //change color of button to indicate finish
                Button GA_button = (Button) findViewById(R.id.graphGAButton);
                GA_button.setBackgroundColor(0xb0ffffff);

                // change text of clear button
                Button button = (Button) findViewById(R.id.clearButton);
                button.setText("CLEAR");

                solveInProgress = false;
            }
        }

        // solves and displays TSP using SA
       class SA_Task extends AsyncTask<Void, Void, Void> {

            volatile Tour current;
            volatile Tour best;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                solveInProgress = true;

                //change color of button to indicate processing
                Button GA_button = (Button) findViewById(R.id.graphSAButton);
                GA_button.setBackgroundColor(0xb0FF9933);

                // change text of clear button
                Button button = (Button) findViewById(R.id.clearButton);
                button.setText("STOP");
            }

            @Override
            protected Void doInBackground(Void... voids) {

                // Initialization
                double temp = Double.parseDouble(mSharedPreferences.getString("temperature", "1000000000"));
                double coolingRate = Double.parseDouble(mSharedPreferences.getString("coolingRate", "0.025f"));

                long time = System.currentTimeMillis();
                long lastPublishTime = time;

                current = new Tour();
                current.generateIndividual();
                System.out.println("Initial distance: " + current.getDistance());
                best = new Tour(current);
                publishProgress(); // Initial graph

                while (temp > 1) {
                    if (isCancelled()) break;

                    time = System.currentTimeMillis();

                    Tour newSolution = new Tour(current);
                    newSolution.mutateIndividual();

                    double currentEnergy = current.getDistance();
                    double neighbourEnergy = newSolution.getDistance();

                    // Decide if we should accept the neighbour
                    if (TSP_SA.acceptanceProbability(currentEnergy, neighbourEnergy, temp) > Math.random()) {
                        current = new Tour(newSolution);
                    }

                    // Keep track of the best solution found
                    if (current.getDistance() < best.getDistance()) {
                        best = current;
                    }

                    if (time - lastPublishTime > publishInterval) {
                        lastPublishTime = time;
                        publishProgress();
                    }

                    if (EMULATOR) {
                        try {
                            Thread.sleep(0, 5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    temp *= 1 - coolingRate;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                super.onProgressUpdate(values);
                System.out.println("Current distance: " + best.getDistance());
                graphMap(best);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                graphMap(best);
                System.out.println("SA Final distance: " + best.getDistance());

                // Display final distance
                TextView tv1 = (TextView) findViewById(R.id.final_distance);
                int finalDistance = (int) best.getDistance();
                tv1.setText("FINAL DISTANCE: " + finalDistance + " km");

                //change color of button to indicate finish
                Button GA_button = (Button) findViewById(R.id.graphSAButton);
                GA_button.setBackgroundColor(0xb0ffffff);

                // change text of clear button
                Button button = (Button) findViewById(R.id.clearButton);
                button.setText("CLEAR");

                solveInProgress = false;
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();

                graphMap(best);
                System.out.println("SA onCancel distance: " + best.getDistance());

                // Display final distance
                TextView tv1 = (TextView) findViewById(R.id.final_distance);
                int finalDistance = (int) best.getDistance();
                tv1.setText("FINAL DISTANCE: " + finalDistance + " km");

                //change color of button to indicate finish
                Button GA_button = (Button) findViewById(R.id.graphSAButton);
                GA_button.setBackgroundColor(0xb0ffffff);

                // change text of clear button
                Button button = (Button) findViewById(R.id.clearButton);
                button.setText("CLEAR");

                solveInProgress = false;
            }
        }
    }

