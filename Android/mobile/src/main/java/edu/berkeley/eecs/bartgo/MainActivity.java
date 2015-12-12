package edu.berkeley.eecs.bartgo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends Activity implements OnMapReadyCallback {
    ////////////////////////////////////////////////////////////////////////////////
    // GLOBAL VARS
    ////////////////////////////////////////////////////////////////////////////////
    //  Context c = getBaseContext();
    static final String orange = "#FB9D50";
    static final String blue = "#335E7F";
    static final String grey = "#BCD0D1";
    GoogleApiClient mApiClient;
    Station originStation;

    BartService mBService;
    boolean mBound = false;

    static String currList = "Favorites";
    static final String TAG_DEBUG = "tag_debug";

    ArrayList<String> allStationsList;
    ArrayList<String> favoritesList;
    ArrayList<Integer> favoritesImageList;

    HashMap<String, Integer> fullHash = new HashMap<String, Integer>();
    HashMap<Integer, String> allStationsHash = new HashMap<Integer, String>();
    HashMap<Integer, String> favoritesHash = new HashMap<Integer, String>();
    HashMap<String, LatLng> stationLatLngMap;

    ArrayList<Station> stationList;




    ////////////////////////////////////////////////////////////////////////////////
    // OVERRIDDEN METHODS (GENERAL)
    ////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting menu bar properties
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#1e2a37"));

        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mApiClient);
                        if (mLastLocation != null) {
                            setOrigin(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                        }
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                    }
                })
                .build();
        mApiClient.connect();
        // Query for navigation if toggle switch was checked.
        // Else, notice that the navInstructions ArrayList remains null.
        // As such, a null check on it can also be used to determine whether or not
        //   navigation has been requested.

        // Capturing UI Views
        final ListView listView = (ListView) findViewById(R.id.listView);
        final FrameLayout mapFrame = (FrameLayout) findViewById(R.id.mapFrame);
        final Button allStationsButton = (Button) findViewById(R.id.allStations);
        final Button favoritesButton = (Button) findViewById(R.id.favoritesButton);
        final Button mapButton = (Button) findViewById(R.id.mapButton);
        final TextView underlineFavorites = (TextView) findViewById(R.id.underlineFavorites);
        final TextView underlineAll = (TextView) findViewById(R.id.underlineAll);
        final TextView underlineMap = (TextView) findViewById(R.id.underlineMap);

        underlineAll.setBackgroundColor(Color.parseColor(blue));
        underlineMap.setBackgroundColor(Color.parseColor(blue));
        allStationsButton.setTextColor(Color.parseColor(grey));
        mapButton.setTextColor(Color.parseColor(grey));

        // Bart service initialization
        mBService = new BartService();
        stationList = mBService.getStations();

        // Station Latitude-Longitude data structure
        stationLatLngMap = getStationLatLngMap();

        // UI data structures
        createAllStationsHash();
        createFavoritesHash();
        listView.setAdapter(setFavoriteStations());

        // Generate mapFragment for Map tab
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.mapFrag);
        mapFragment.getMapAsync(this);

        // Generate UI Spinners and OnClickListeners
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String dest = "";
                if (MainActivity.currList == "All") {
                    dest = allStationsHash.get(position);
                }
                else {
                    dest = favoritesHash.get(position);
                }

                // Prepare extras from data (ie. the selected station)
                LatLng destLatLng = stationLatLngMap.get(dest);
                Double destLat = destLatLng.latitude;
                Double destLng = destLatLng.longitude;
                // Dummy origin data
                Double origLat = Double.parseDouble(originStation.getLatitude());
                Double origLng = Double.parseDouble(originStation.getLongitude());

//                // Prepare extras from data (ie. the selected station)
//                Double destLat = Double.parseDouble(mBService.lookupStationByName(dest).getLatitude());
//                Double destLng = Double.parseDouble(mBService.lookupStationByName(dest).getLongitude());
//                // Dummy origin data
//                Double origLat = 37.875173;
//                Double origLng = -122.260172;

                // Create post-selection intent and put extras
                Intent postSelection = new Intent();
                postSelection.setClass(view.getContext(), postSelection.class);

                postSelection.putExtra("destName", dest);
                postSelection.putExtra("destLat", destLat);
                postSelection.putExtra("destLng", destLng);
                postSelection.putExtra("origLat", origLat);
                postSelection.putExtra("origLng", origLng);
                postSelection.putExtra("origStation", originStation.getAbbreviation());
                startActivityForResult(postSelection, 1);
            }
        });

        allStationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currList = "All";
                listView.setAdapter(setAllStations());
                mapFrame.setVisibility(View.INVISIBLE);
                underlineAll.setBackgroundColor(Color.parseColor(orange)); // orange
                underlineMap.setBackgroundColor(Color.parseColor(blue));  // blue
                underlineFavorites.setBackgroundColor(Color.parseColor(blue)); // blue
                allStationsButton.setTextColor(Color.parseColor(orange));  // orange
                favoritesButton.setTextColor(Color.parseColor(grey));
                mapButton.setTextColor(Color.parseColor(grey));
            }
        });

        favoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currList = "Favorites";
                listView.setAdapter(setFavoriteStations());
                mapFrame.setVisibility(View.INVISIBLE);
                underlineFavorites.setBackgroundColor(Color.parseColor(orange));
                        underlineAll.setBackgroundColor(Color.parseColor(blue));
                underlineMap.setBackgroundColor(Color.parseColor(blue));
                favoritesButton.setTextColor(Color.parseColor(orange));
                allStationsButton.setTextColor(Color.parseColor(grey));
                mapButton.setTextColor(Color.parseColor(grey));
            }
        });

        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.setAdapter(clearStations());
                mapFrame.setVisibility(View.VISIBLE);
                underlineMap.setBackgroundColor(Color.parseColor(orange));
                underlineAll.setBackgroundColor(Color.parseColor(blue));
                underlineFavorites.setBackgroundColor(Color.parseColor(blue));
                mapButton.setTextColor(Color.parseColor(orange));
                favoritesButton.setTextColor(Color.parseColor(grey));
                allStationsButton.setTextColor(Color.parseColor(grey));
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BartService.class);
        ComponentName mService = startService(intent);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent intent = new Intent(this, BartService.class);
        stopService(intent);
        unbindService(mConnection);
//        ComponentName mService = startService(intent);
    }
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BartService.LocalBinder binder = (BartService.LocalBinder) service;
            BartService mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    ////////////////////////////////////////////////////////////////////////////////
    // MAP PREPARATION
    ////////////////////////////////////////////////////////////////////////////////
    /**
     * Returns a HashMap between station names and
     * latitude-longitude coordinates.
     *
     * TODO--INTEGRATION:  CHANGE TO ACCEPT INPUT FROM NICK'S API DATA
     *
     * @return          A HashMap<String, LatLng> station mapping
     */
    public HashMap<String, LatLng> getStationLatLngMap() {
        HashMap<String,LatLng> stationMap = new HashMap<>();

        for (Station s : mBService.getStations()) {
            Double sLat = Double.parseDouble(s.getLatitude());
            Double sLng = Double.parseDouble(s.getLongitude());
//            Log.d(TAG_DEBUG, "***** meeeeeep!");
            stationMap.put(s.getName(), new LatLng(sLat, sLng));
        }
//        Log.d(TAG_DEBUG, "*****" + stationMap.get("12th St. Oakland City Center"));
//        Log.d(TAG_DEBUG, "*****" + stationMap.get("West Oakland"));

        return stationMap;
    }

    public String generateSnippet(String dest) {
        Station destStation = mBService.lookupStationByName(dest);
        Station origStation;
        if (originStation != null) {
            origStation = originStation;
        } else {
            origStation = mBService.lookupStationByAbbreviation("DBRK");
        }
        DateFormat df = new SimpleDateFormat("hh:mma", Locale.US);
        Date now = Calendar.getInstance(TimeZone.getDefault()).getTime();
        Trip mTrip = mBService.generateTrip(origStation, destStation, df.format(now));

        float fare = mTrip.getFare();
        DecimalFormat decim = new DecimalFormat("0.00");
        String fareOneWay = decim.format(fare);
        String fareRoundTrip = decim.format(2*fare);

        String mSnippet = "$" + fareOneWay + " | $" + fareRoundTrip;
        return mSnippet;
    }

    // Sets origStation to be the nearest station
    public void setOrigin(double latitude, double longitude) {
        Location userLoc = new Location("User");
        userLoc.setLatitude(latitude);
        userLoc.setLongitude(longitude);
        Set<Map.Entry<String, LatLng>> entries = stationLatLngMap.entrySet();
        Iterator<Map.Entry<String, LatLng>> iter = entries.iterator();
        Double bestDist = Double.MAX_VALUE;

        while (iter.hasNext()) {
            Map.Entry<String, LatLng> entry = iter.next();
            LatLng val = entry.getValue();
            String stationName = entry.getKey();
            Location stationLoc = new Location("Station");
            stationLoc.setLatitude(val.latitude);
            stationLoc.setLongitude(val.longitude);
            Double currDist = (double) userLoc.distanceTo(stationLoc);
            if (currDist < bestDist) {
                currDist = bestDist;
                originStation = mBService.lookupStationByName(entry.getKey());
            }
        }
    }



    ////////////////////////////////////////////////////////////////////////////////
    // MAP GENERATION
    ////////////////////////////////////////////////////////////////////////////////
    /**
     * Generates a scaled bitmap icon  from an R.drawable element.
     *
     * @param   resId   The image id to be used as the icon.  Of the form
     *                  "R.drawable.image_name"
     * @param   scale   The "down-scaling" factor.  Ie. width and height are scaled
     *                  by a factor of 1 / scale.
     * @return          The scaled bitmap.
     */
    public Bitmap generateIcon(int resId, int scale) {
        Bitmap b = BitmapFactory.decodeResource(getResources(), resId);
        Bitmap bScaled = Bitmap.createScaledBitmap(b, b.getWidth() / scale, b.getHeight() / scale, false);
        return bScaled;
    }

    /**
     * Sets map camera zoom and places markers upon the given map's readiness.
     * Tap: display station name and station details.
     * Long-Tap: launch turn-by-turn navigation (NavActivity) to selected station.
     *
     * TODO--INTEGRATION:  REPLACE DUMMY STATION DETAILS SNIPPET WITH ACTUAL INFO FROM NICK'S API CALLS
     * TODO--INTEGRATION:  REPLACE DUMMY ORIGIN LAT/LNG DATA (SEE onMarkerDragStart())
     * TODO                WITH ACTUAL CURRENT POS CALCULATED IN PATRICK'S MAIN ACTIVITY.
     *
     * @param   map     The GoogleMap instance to display.
     */
    @Override
    public void onMapReady(GoogleMap map) {
//        Log.d(TAG_DEBUG, "***** MEEEP! MAP IS READY");
        // Set camera zoom
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(37.804697, -122.201255), (float) 9.5));

        // Iterate through all stations in the stationLatLngMap,
        // generating a Marker for each
        Set<Map.Entry<String, LatLng>> entries = stationLatLngMap.entrySet();
        Iterator<Map.Entry<String, LatLng>> iter = entries.iterator();

        for (int i = 0; i < stationLatLngMap.size(); i++) {
            Map.Entry<String, LatLng> entry = iter.next();
            LatLng val = entry.getValue();
            String stationName = entry.getKey();

            // Log.d(TAG_DEBUG, "*****" + val);

            Marker mMarker = map.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(generateIcon(R.drawable.marker_bartgo_logo_round, 2)))
                    .anchor(0.5f, 1.0f) /*Anchors the marker on the bottom center */
                    .position(val)
                    .title(stationName + " BART")
                    /*.snippet("ETA:  50 min | $6.50 | $13.00")*/
                    .snippet(generateSnippet(stationName))
                    .draggable(true));
//            Log.d(TAG_DEBUG, "***** Marker added at " + val);
//            Log.d(TAG_DEBUG, "***** Marker added: " + mMarker);
            map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                // Simulate long-click functionality
                @Override
                public void onMarkerDragStart(Marker marker) {
                    Intent postSelectionIntent = new Intent(getBaseContext(), postSelection.class);

                    // Retrieve destination based on marker being long-tapped on
                    String stationKey = marker.getTitle();
                    int len = stationKey.length();
                    stationKey = stationKey.substring(0, len - 5);
                    LatLng stationLatLng = getStationLatLng(stationKey);

                    // Origin data
                    Double origLat = Double.parseDouble(originStation.getLatitude());
                    Double origLng = Double.parseDouble(originStation.getLongitude());
                    // Dest data
                    Double destLat = stationLatLng.latitude;
                    Double destLng = stationLatLng.longitude;

                    // Pass origin/destination extras
                    postSelectionIntent.putExtra("origLat", origLat);
                    postSelectionIntent.putExtra("origLng", origLng);
                    postSelectionIntent.putExtra("destLat", destLat);
                    postSelectionIntent.putExtra("destLng", destLng);
                    postSelectionIntent.putExtra("destName", stationKey);
                    postSelectionIntent.putExtra("origStation", originStation.getAbbreviation());
                    startActivity(postSelectionIntent);

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    // Nothing special to do
                }

                @Override
                public void onMarkerDrag(Marker marker) {
                    // Do special to do
                }
            });
        }

    }





    ////////////////////////////////////////////////////////////////////////////////
    // MAP GENERATION:  LAT-LON RETRIEVAL                        (HELPER METHODS) //
    ////////////////////////////////////////////////////////////////////////////////
    /**
     * Returns an array containing all station names.
     *
     * @return          A String[] of all station names, retrieved from global HashMap.
     */
    public String[] getStations() {
        return (String[]) stationLatLngMap.keySet().toArray();
    }

    /**
     * Returns a station latitutde-longitude coordinates
     *
     * @param   name    The station's name.
     * @return          The station's latitude and longitude, as a LatLng.
     */
    public LatLng getStationLatLng(String name) {
        return stationLatLngMap.get(name);
    }







    ////////////////////////////////////////////////////////////////////////////////
    // MOBILE UI:  CUSTOM LIST GENERATOR
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * A custom ArrayAdapter class for displaying stations to select from.
     */
    public class CustomList extends ArrayAdapter<String> {

        private final Activity context;
        private final ArrayList<String> web;
        private final ArrayList<Integer> imageId;
        public CustomList (Activity context,
                           ArrayList<String> web, ArrayList<Integer> imageId) {
            super(context, R.layout.simple_list_item_1, web);
            this.context = context;
            this.web = web;
            this.imageId = imageId;

        }
        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView= inflater.inflate(R.layout.simple_list_item_1, null, true);
            TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);

            final ImageView imageView = (ImageView) rowView.findViewById(R.id.img);
            txtTitle.setText(web.get(position));

            imageView.setImageResource(imageId.get(position));
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currList == "All") {
                        boolean notStarred = true;
                        boolean imgChanged = false;
                        String temp = "";
                        for (String station : favoritesList) {
                            if (allStationsHash.get(position) == station) {
                                temp = station;
                                imageView.setImageResource(R.drawable.graystar);
                                imgChanged = true;
                                notStarred = false;
                            }
                        }
                        if (temp != "") {
                            favoritesList.remove(temp);
                        }
                        if (imgChanged == false && notStarred) {
                            favoritesList.add(allStationsHash.get(position));
                            imageView.setImageResource(R.drawable.star);
                            imgChanged = true;
                        }
                    }
                }
            });
            return rowView;
        }
    }






    ////////////////////////////////////////////////////////////////////////////////
    // MOBILE UI:  CREATION METHODS
    ////////////////////////////////////////////////////////////////////////////////
    /**
     * Generates station list with which to populate the "All Stations" tab's Spinner.
     *
     * TODO--INTEGRATION:  CHANGE TO ACCEPT INPUT FROM NICK'S API DATA
     */
    public void createAllStationsHash() {
        int len = stationLatLngMap.size();
        allStationsList = new ArrayList<>(len);

        for (Station s : mBService.getStations()) {
            allStationsList.add(s.getName());
        }

        /* TODO--DELETE
        // Hard-coded station data
        allStationsList =  new ArrayList<>(45);
        allStationsList.add("12th St. Oakland City Center");
        allStationsList.add("16th St. Mission");
        allStationsList.add("19th St. Oakland");
        allStationsList.add("24th St. Mission");
        allStationsList.add("Ashby");
        allStationsList.add("Balboa Park");
        allStationsList.add("Bay Fair");
        allStationsList.add("Castro Valley");
        allStationsList.add("Civic Center/UN Plaza");
        allStationsList.add("Coliseum");
        allStationsList.add("Colma");
        allStationsList.add("Concord");
        allStationsList.add("Daly City");
        allStationsList.add("Downtown Berkeley");
        allStationsList.add("Dublin/Pleasanton");
        allStationsList.add("El Cerrito del Norte");
        allStationsList.add("El Cerrito Plaza");
        allStationsList.add("Embarcadero");
        allStationsList.add("Fremont");
        allStationsList.add("Fruitvale");
        allStationsList.add("Glen Park");
        allStationsList.add("Hayward");
        allStationsList.add("Lafayette");
        allStationsList.add("Lake Merritt");
        allStationsList.add("MacArthur");
        allStationsList.add("Millbrae");
        allStationsList.add("Montgomery St.");
        allStationsList.add("North Berkeley");
        allStationsList.add("North Concord/Martinez");
        allStationsList.add("Oakland Int'l Airport");
        allStationsList.add("Orinda");
        allStationsList.add("Pittsburg/Bay Point");
        allStationsList.add("Pleasant Hill/Contra Costa Centre");
        allStationsList.add("Powell St.");
        allStationsList.add("Richmond");
        allStationsList.add("Rockridge");
        allStationsList.add("San Bruno");
        allStationsList.add("San Francisco Int'l Airport");
        allStationsList.add("San Leandro");
        allStationsList.add("South Hayward");
        allStationsList.add("South San Francisco");
        allStationsList.add("Union City");
        allStationsList.add("Walnut Creek");
        allStationsList.add("West Dublin/Pleasanton");
        allStationsList.add("West Oakland"); */
        Collections.sort(allStationsList);
        int count = 0;
        for (String station:allStationsList) {
            allStationsHash.put(count, station);
            fullHash.put(station, count);
            count++;
        }
    }

    /**
     * Generates a sample station list with which to populate the "Favorites" tab's Spinner.
     */
    public void createFavoritesHash() {
        // Hardcoded (intended as default?) data
        favoritesList =  new ArrayList<>(45);
        favoritesList.add("12th St. Oakland City Center");
        favoritesList.add("Civic Center/UN Plaza");
        favoritesList.add("Coliseum");
        favoritesList.add("Embarcadero");
        favoritesList.add("Montgomery St.");
        favoritesList.add("Rockridge");
        favoritesList.add("San Francisco Int'l Airport");
        favoritesList.add("Walnut Creek");
        favoritesList.add("West Dublin/Pleasanton");
        Collections.sort(favoritesList);
        int count = 0;
        for (String station:favoritesList) {
            favoritesHash.put(count, station);
            count++;
        }
    }

    /**
     * Sets the station list to display under the "All Stations" tab as an ArrayAdapter.
     *
     * @return      A CustomList ArrayAdapter of stations to display.
     */
    public CustomList setAllStations(){
        favoritesImageList = new ArrayList<Integer>(Collections.nCopies(45, R.drawable.graystar));
        Collections.sort(favoritesList);
        int count = 0;
        for (String station:favoritesList) {
            favoritesHash.put(count, station);
            count++;
        }
        for (String station:favoritesHash.values()) {
            favoritesImageList.set(fullHash.get(station), R.drawable.star);
        }

        CustomList adapter = new CustomList(MainActivity.this, allStationsList, favoritesImageList);
        return adapter;
    }

    /**
     * Sets the station list to display under the "Favorites" tab as an ArrayAdapter.
     *
     * @return      An ArrayAdapter of stations to display.
     */
    public ArrayAdapter<String> setFavoriteStations(){
        favoritesImageList = new ArrayList<Integer>(favoritesList.size());
        Collections.sort(favoritesList);
        int count = 0;
        while (count < favoritesList.size()){
            favoritesImageList.add(R.drawable.star);
            count++;
        }
        count = 0;
        for (String station:favoritesList) {
            favoritesHash.put(count, station);
            count++;
        }

        CustomList adapter = new CustomList(MainActivity.this, favoritesList, favoritesImageList);
        return adapter;
    }

    /**
     * Clears the station list to display (?)
     *
     * @return      The cleared ArrayAdapter of stations.
     */
    public ArrayAdapter<String> clearStations(){
        ArrayList<String> list =  new ArrayList<>(1);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1, list);
        return listAdapter;
    }

}
