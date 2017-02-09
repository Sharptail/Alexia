package sg.edu.nyp.alexia;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.PathWrapper;
import com.graphhopper.util.Helper;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.ProgressListener;
import com.graphhopper.util.StopWatch;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.geometry.VisibleRegion;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import sg.edu.nyp.alexia.Class.ATM;
import sg.edu.nyp.alexia.Class.Elevator;
import sg.edu.nyp.alexia.Class.Nearby;
import sg.edu.nyp.alexia.Class.NearbyAdapter;
import sg.edu.nyp.alexia.Class.Room;
import sg.edu.nyp.alexia.Class.RoomAdapter;


public class RoutingActivity extends Activity {
    final int QR_CODE_SCANNER_CODE = 1;
    final int OCR_CAMERA_CODE = 2;
    private AppDrawer appDrawer;
    private ProgressDialog progressDialog;
    private LinearLayout drawerLayout;
    private ViewGroup.LayoutParams drawerParams;
    private RelativeLayout routingLayout;
    private Button expandButton;
    private MapView mapView;
    private GraphHopper hopper;
    private LatLng start;
    private Marker startMarker;
    private LatLng end;
    private String currentArea = "singapore7";
    private String downloadURL;
    private File mapsFolder;
    private volatile boolean prepareInProgress = false;
    private volatile boolean shortestPathRunning = false;
    private Integer selectedLocationIndex = 0;
    private ArrayList<Room> roomList = new ArrayList<>();
    private ArrayList<ATM> atmList = new ArrayList<>();
    private ArrayList<Elevator> elevatorList = new ArrayList<>();
    private List<LatLng> calculatedPoints = new ArrayList<>();
    private List<Polyline> calculatedPolylines = new ArrayList<>();
    private Double calculatedDistance;
    private int userCurrentPos = 0;
    private ArrayList<String> destination_location = new ArrayList<String>();
    private ArrayList<String> destination_coords = new ArrayList<String>();
    private boolean atmShowed = false;
    private int currentLevel = 3;
    private LatLng stairsPoint = new LatLng(1.3792667, 103.8495746);
    private ListView roomListView;
    private SearchView roomSearchView;
    private ListView nearbyListView;
    private SearchView nearbySearchView;
    private boolean isEndFirst = false;
    private LatLng NE_LIMIT = new LatLng(1.3797034, 103.850184);
    private LatLng SW_LIMIT = new LatLng(1.378903, 103.8493619);
    private String preferredElevator = "stairs";
    private LatLng preferredElevatorPoint;
    private TextView currentLevelTV;
    private ImageButton ocrButton;
    private TessBaseAPI mTess;
    private String ocrDatapath = "";
    private CameraPosition newCameraPos;
    private Button startButton;
    private Button nextButton;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_routing);

        appDrawer = new AppDrawer(this);
        drawerLayout = (LinearLayout) findViewById(R.id.bottom_drawer);
        routingLayout = (RelativeLayout) findViewById(R.id.routing_layout);
        expandButton = (Button) findViewById(R.id.expandButton);
        currentLevelTV = (TextView) findViewById(R.id.current_level_text);

        roomListView=(ListView) findViewById(R.id.room_list_view);
        roomSearchView=(SearchView) findViewById(R.id.room_search_view);
        nearbyListView=(ListView) findViewById(R.id.nearby_list_view);
        nearbySearchView=(SearchView) findViewById(R.id.nearby_search_view);

        roomListView = (ListView) findViewById(R.id.room_list_view);
        roomSearchView = (SearchView) findViewById(R.id.room_search_view);
        ocrButton = (ImageButton) findViewById(R.id.ocr_button);
        startButton = (Button) findViewById(R.id.start_button);
        nextButton = (Button) findViewById(R.id.next_button);

        drawerParams = drawerLayout.getLayoutParams();

        //initialize Tesseract API
        String language = "eng";
        ocrDatapath = getFilesDir()+ "/tesseract/";
        mTess = new TessBaseAPI();
        ocrCheckFile(new File(ocrDatapath + "tessdata/"));
        mTess.init(ocrDatapath, language);
        mTess.setVariable("tessedit_char_whitelist", "0123456789");

        roomSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b == true) {
                    expandDrawer();
                    ocrButton.setVisibility(View.VISIBLE);
                } else {
                    collapseDrawer();
                    ocrButton.setVisibility(View.GONE);
                }
            }
        });

        nearbySearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b == true){
                    expandDrawer();
                }else{
                    collapseDrawer();
                }
            }
        });

        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("Routing");

        myRef.child("fyproom").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                roomList.clear();
                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    Room room = roomSnapshot.getValue(Room.class);
                    roomList.add(room);
                }

                for (int i = 0; i < roomList.size(); i++) {
                    destination_location.add(roomList.get(i).getName());
                    destination_coords.add(Double.toString(roomList.get(i).getLat()) + "," + Double.toString(roomList.get(i).getLng()));
                }
                //String [] destination_location = {"Big Room", "Last Room"};
                //final String [] destination_coords = {"1.379166419501388,103.84994486842379","1.3790777965512575,103.84973653167629"};

                final RoomAdapter adapter = new RoomAdapter(RoutingActivity.this, roomList);
                roomListView.setAdapter(adapter);

                roomSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String arg0) {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        // TODO Auto-generated method stub
                        adapter.getFilter().filter(query);
                        return false;
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        myRef.child("nearby").child("atm").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    ATM atm = roomSnapshot.getValue(ATM.class);
                    atmList.add(atm);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        myRef.child("elevator").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    Elevator elevator = roomSnapshot.getValue(Elevator.class);
                    elevatorList.add(elevator);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Jian Wei - set graphhopper maps' folder
        boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >= 19;
        if (greaterOrEqKitkat) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                logUser("GraphHopper is not usable without an external storage!");
                return;
            }
            mapsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/graphhopper/maps/");
        } else
            mapsFolder = new File(Environment.getExternalStorageDirectory(), "/graphhopper/maps/");

        if (!mapsFolder.exists()) {
            mapsFolder.mkdirs();
        }

        mapView = (MapView) findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        // Add a MapboxMap
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                //ZOOM TO LOCATION
                final LatLng zoomLocation = new LatLng(1.3792949602146791, 103.84983998176449);
                CameraPosition position = new CameraPosition.Builder()
                        .target(zoomLocation)
                        .zoom(19) // Sets the zoom
                        .build(); // Creates a CameraPosition from the builder
                mapboxMap.setMinZoom(19);
                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 2000);
                mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng point) {
                        if (!isReady()) {
                            logUser("Load Map or Graph failed!");
                            return;
                        }
                        if (shortestPathRunning) {
                            logUser("Calculation still in progress");
                            return;
                        }

                        if (start != null && end == null) {
                            end = point;
                            shortestPathRunning = true;

                            // Add the marker to the map
                            mapboxMap.addMarker(createMarkerItem(end, R.drawable.end, "Destination", ""));

                            // Calculate Shortest Path
                            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude(), mapboxMap);
                        } else {
                            userCurrentPos = 0;
                            start = point;
                            String test = Double.toString(start.getLatitude()) + "," + Double.toString(start.getLongitude());
                            Log.e("Test", Double.toString(start.getLatitude()) + "," + Double.toString(start.getLongitude()));
                            end = null;
                            mapboxMap.clear();
                            // Add the marker to the map
                            mapboxMap.addMarker(createMarkerItem(start, R.drawable.start, "Start", ""));
                        }
                    }
                });

                mapboxMap.setOnScrollListener(new MapboxMap.OnScrollListener() {
                    @Override
                    public void onScroll() {
                        restrictMapToBoundingBox();
                    }
                });

                mapboxMap.setOnFlingListener(new MapboxMap.OnFlingListener() {
                    @Override
                    public void onFling() {
                        restrictMapToBoundingBox();
                    }
                });

                roomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        appDrawer.switchDrawer(2);
                        selectedLocationIndex = i;
                        if(roomList.get(selectedLocationIndex).getLevel() == currentLevel){
                            String [] coords = destination_coords.get(selectedLocationIndex).split(",");
                            Double lat = Double.parseDouble(coords[0]);
                            Double lng = Double.parseDouble(coords[1]);
                            LatLng point = new LatLng(lat,lng);
                            end = point;

                            // Add the marker to the map
                            mapboxMap.addMarker(createMarkerItem(end, R.drawable.end, "Destination", ""));

                            // Calculate Shortest Path
                            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                                    end.getLongitude(),mapboxMap);
                        }else{
                            appDrawer.switchDrawer(4);
                        }
                    }
                });
            }
        });
        initFiles(currentArea);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    String value = extras.getString("result");
                    Log.e("IntentApoint", value);
                    AppCheckRoute(value);
                }
            }
        }, 5000);

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(RoutingActivity.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    public void easeCameraBackToBoundingBox(){

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                LatLngBounds latLngBounds = new LatLngBounds.Builder()
                        .include(NE_LIMIT) // Northeast
                        .include(SW_LIMIT) // Southwest
                        .build();

                mapboxMap.easeCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0), 100);
            }
        });
    }

    public void restrictMapToBoundingBox() {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                VisibleRegion visibleRegion = mapboxMap.getProjection().getVisibleRegion();

                Double maxLat = NE_LIMIT.getLatitude();
                Double maxLng = NE_LIMIT.getLongitude();
                Double minLat = SW_LIMIT.getLatitude();
                Double minLng = SW_LIMIT.getLongitude();

                if( !(visibleRegion.farLeft.getLatitude() >= minLat && visibleRegion.farLeft.getLatitude() <= maxLat
                        && visibleRegion.farLeft.getLongitude() >= minLng && visibleRegion.farLeft.getLongitude() <= maxLng) ) {
                    easeCameraBackToBoundingBox();
                }
                if( !(visibleRegion.farRight.getLatitude() >= minLat && visibleRegion.farRight.getLatitude() <= maxLat
                        && visibleRegion.farRight.getLongitude() >= minLng && visibleRegion.farRight.getLongitude() <= maxLng) ) {
                    easeCameraBackToBoundingBox();
                }
                if( !(visibleRegion.nearLeft.getLatitude() >= minLat && visibleRegion.nearLeft.getLatitude() <= maxLat
                        && visibleRegion.nearLeft.getLongitude() >= minLng && visibleRegion.nearLeft.getLongitude() <= maxLng) ) {
                    easeCameraBackToBoundingBox();
                }
                if( !(visibleRegion.nearRight.getLatitude() >= minLat && visibleRegion.nearRight.getLatitude() <= maxLat
                        && visibleRegion.nearRight.getLongitude() >= minLng && visibleRegion.nearRight.getLongitude() <= maxLng) ) {
                    easeCameraBackToBoundingBox();
                }
            }
        });
    }

    public void change(View view) {
        mapView.setStyleUrl(getString(R.string.mapbox_url2));
        currentArea = "singapore72";
        loadGraphStorage();
    }

    // Jian Wei - Check if the graphhopper is loaded and preparation is finished
    boolean isReady() {
        if (hopper != null)
            return true;

        if (prepareInProgress) {
            logUser("Preparation still in progress");
            return false;
        }
        logUser("Prepare finished but hopper not ready. This happens when there was an error while loading the files");
        return false;
    }

    // Jian Wei - init
    private void initFiles(String area) {
        prepareInProgress = true;
        currentArea = area;
        downloadingFiles();
    }

    void downloadingFiles() {
        final File areaFolder = new File(mapsFolder, currentArea + "-gh");
        if (downloadURL == null || areaFolder.exists()) {
            loadMap(areaFolder);
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Downloading and uncompressing " + downloadURL);
        dialog.setIndeterminate(false);
        dialog.setMax(100);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.show();

        new GHAsyncTask<Void, Integer, Object>() {
            protected Object saveDoInBackground(Void... _ignore)
                    throws Exception {
                String localFolder = Helper.pruneFileEnd(AndroidHelper.getFileName(downloadURL));
                localFolder = new File(mapsFolder, localFolder + "-gh").getAbsolutePath();
                log("downloading & unzipping " + downloadURL + " to " + localFolder);
                AndroidDownloader downloader = new AndroidDownloader();
                downloader.setTimeout(30000);
                downloader.downloadAndUnzip(downloadURL, localFolder,
                        new ProgressListener() {
                            @Override
                            public void update(long val) {
                                publishProgress((int) val);
                            }
                        });
                return null;
            }

            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                dialog.setProgress(values[0]);
            }

            protected void onPostExecute(Object _ignore) {
                dialog.dismiss();
                if (hasError()) {
                    String str = "An error happened while retrieving maps:" + getErrorMessage();
                    log(str, getError());
                    logUser(str);
                } else {
                    loadMap(areaFolder);
                }
            }
        }.execute();
    }

    // Jian Wei - this was used to download maps and load it before loading the graph
    void loadMap(File areaFolder) {
//        logUser("loading map");

        // Long press receiver
//        mapView.map().layers().add(new LongPressLayer(mapView.map()));
//
//        // Map file source
//        MapFileTileSource tileSource = new MapFileTileSource();
//        tileSource.setMapFile(new File(areaFolder, currentArea + ".map").getAbsolutePath());
//        VectorTileLayer l = mapView.map().setBaseMap(tileSource);
//        mapView.map().setTheme(VtmThemes.DEFAULT);
//        mapView.map().layers().add(new BuildingLayer(mapView.map(), l));
//        mapView.map().layers().add(new LabelLayer(mapView.map(), l));
//
//        // Markers layer
//        itemizedLayer = new ItemizedLayer<>(mapView.map(), (MarkerSymbol) null);
//        mapView.map().layers().add(itemizedLayer);
//
//        // Map position
//        GeoPoint mapCenter = tileSource.getMapInfo().boundingBox.getCenterPoint();
//        mapView.map().setMapPosition(mapCenter.getLatitude(), mapCenter.getLongitude(), 1 << 15);
//
//        setContentView(mapView);
        loadGraphStorage();
    }

    // Jian Wei - to laod the graph storage we created (Very Important)
    void loadGraphStorage() {
        // logUser("loading graph (" + Constants.VERSION + ") ... ");
//        logUser("Loading graph, please wait");
        new GHAsyncTask<Void, Void, Path>() {
            protected Path saveDoInBackground(Void... v) throws Exception {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.load(new File(mapsFolder, currentArea).getAbsolutePath() + "-gh");
                log("found graph " + tmpHopp.getGraphHopperStorage().toString() + ", nodes:" + tmpHopp.getGraphHopperStorage().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute(Path o) {
                if (hasError()) {
                    logUser("An error happened while creating graph:"
                            + getErrorMessage());
                } else {
//                    logUser("Finished loading graph.");
                }

                finishPrepare();
//                openQRScanner();

            }
        }.execute();
    }

    public void openQRScanner(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == -1) {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA
            }, 1234);
        } else {
            Intent intent = new Intent(this, QRCodeScannerActivity.class);
            startActivityForResult(intent, QR_CODE_SCANNER_CODE);
        }
    }

    public void openOCRCamera(View view){
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        startActivityForResult(intent,OCR_CAMERA_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == QR_CODE_SCANNER_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                String[] coords = result.split(",");
                Double lat = Double.parseDouble(coords[0]);
                Double lng = Double.parseDouble(coords[1]);
                if(currentLevel != Integer.parseInt(coords[2]) ){
                    switchLayers(Integer.parseInt(coords[2]));
                }
                final LatLng points = new LatLng(lat, lng);
                start = points;
                if(isEndFirst == false){
                    end = null;
                }

                mapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final MapboxMap mapboxMap) {
                        if(isEndFirst == false){
                            mapboxMap.clear();
                        }
                        userCurrentPos = 0;
                        // Add the marker to the map
                        startMarker = mapboxMap.addMarker(createMarkerItem(start, R.drawable.start, "You Are Here!", ""));
                        //startMarker.showInfoWindow(mapboxMap,mapView);

                        //ZOOM TO LOCATION
                        LatLng zoomLocation = new LatLng(start.getLatitude(), start.getLongitude());
                        CameraPosition position = new CameraPosition.Builder()
                                .target(zoomLocation)
                                .zoom(22) // Sets the zoom
                                .tilt(0)
                                .bearing(0)
                                .build(); // Creates a CameraPosition from the builder
                        mapboxMap.animateCamera(CameraUpdateFactory
                                .newCameraPosition(position), 2000);

                        if(isEndFirst == false){
                            appDrawer.switchDrawer(1);
                        }else if(end != null){
                            appDrawer.switchDrawer(2);
                            // Calculate Shortest Path
                            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                                    end.getLongitude(),mapboxMap);
                        }

                        for (int i = 0; i < atmList.size(); i++) {
                            getDistance(start.getLatitude(), start.getLongitude(), atmList.get(i).getLat(), atmList.get(i).getLng(), i, "ATM");
                        }

                        for (int i = 0; i < elevatorList.size(); i++) {
                            getDistance(start.getLatitude(), start.getLongitude(), atmList.get(i).getLat(), atmList.get(i).getLng(), i, "Elevator");
                        }
                    }
                });
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
        else if(requestCode == OCR_CAMERA_CODE){
            Bitmap image = (Bitmap) data.getExtras().get("data");
            mTess.setImage(image);
            //convert to int only
            roomSearchView.setQuery(mTess.getUTF8Text(),true);
        }
    }

    public void closeDrawer(View view) {
        appDrawer.closeDrawer();
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.clear();
            }
        });
        resetMapView();
    }

    public void toggleDrawer(View view) {
        if (drawerParams.height == (int) getResources().getDimension(R.dimen.drawer_height)) {
            expandDrawer();
        } else {
            collapseDrawer();
        }
    }

    public void expandDrawer(){
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels-100;

//        drawerParams.height = (int) getResources().getDimension(R.dimen.drawer_height2);
        drawerParams.height = height;
        drawerLayout.setLayoutParams(drawerParams);
//        appDrawer.drawerMovement(1,getResources().getDimension(R.dimen.drawer_height2));
        appDrawer.drawerMovement(1,height);
        expandButton.setText("v");
    }

    public void collapseDrawer() {
        drawerParams.height = (int) getResources().getDimension(R.dimen.drawer_height);
        appDrawer.drawerMovement(1, getResources().getDimension(R.dimen.drawer_height));
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        drawerLayout.setLayoutParams(drawerParams);
                    }
                },
                300);
        expandButton.setText("^");
    }

    public void openGoTo(View view) {
        appDrawer.switchDrawer(2);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                final ArrayAdapter<String> adp = new ArrayAdapter<String>(RoutingActivity.this,
                        android.R.layout.simple_spinner_item, destination_location);

                final Spinner sp = new Spinner(RoutingActivity.this);
                sp.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                sp.setAdapter(adp);

                AlertDialog.Builder builder = new AlertDialog.Builder(RoutingActivity.this);
                builder.setView(sp);
                builder.setTitle("Where do you want to go?");
                builder.setMessage("Please choose your destination");
                builder.setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (selectedLocationIndex >= 0) {
                            if (roomList.get(selectedLocationIndex).getLevel() == currentLevel) {
                                String[] coords = destination_coords.get(selectedLocationIndex).split(",");
                                Double lat = Double.parseDouble(coords[0]);
                                Double lng = Double.parseDouble(coords[1]);
                                LatLng point = new LatLng(lat, lng);
                                end = point;

                                // Add the marker to the map
                                mapboxMap.addMarker(createMarkerItem(end, R.drawable.end, "Destination", ""));

                                // Calculate Shortest Path
                                calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                                        end.getLongitude(), mapboxMap);
                            } else {
                                end = stairsPoint;

                                // Calculate Shortest Path
                                calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                                        end.getLongitude(), mapboxMap);
                            }
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mapboxMap.clear();
                        appDrawer.closeDrawer();
                    }
                });


                builder.create().show();

                sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        selectedLocationIndex = i;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        selectedLocationIndex = -1;
                    }
                });
            }
        });
    }

    public void openLayers(View view){
        ArrayList<String> layerList = new ArrayList<>();
        if(currentLevel == 3){
            layerList.add("Level 3 (Current Level)");
        }else{
            layerList.add("Level 3");
        }

        if(currentLevel == 4){
            layerList.add("Level 4 (Current Level)");
        }else{
            layerList.add("Level 4");
        }


        ArrayAdapter<String> adp = new ArrayAdapter<String>(RoutingActivity.this,
                android.R.layout.simple_list_item_1, layerList);

        ListView layersLV = new ListView(this);
        layersLV.setAdapter(adp);

        AlertDialog.Builder builder = new AlertDialog.Builder(RoutingActivity.this);
        builder.setView(layersLV);
        builder.setTitle("Levels");
        builder.setMessage("Please select a level that you want to view");

        final AlertDialog show = builder.show();

        layersLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i){
                    case 0 :
                        switchLayers(3);
                        break;
                    case 1 :
                        switchLayers(4);
                        break;
                }
                show.dismiss();
            }
        });
    }

    public void switchLayers(int level){
        if(level == 3 && currentLevel != 3){
            currentLevel = 3;
            currentLevelTV.setText("Block L Level 3");
            mapView.setStyleUrl(getString(R.string.mapbox_url));
            currentArea = "singapore7";
            loadGraphStorage();
        }else if(level == 4 && currentLevel != 4){
            currentLevel = 4;
            currentLevelTV.setText("Block L Level 4");
            mapView.setStyleUrl(getString(R.string.mapbox_url2));
            currentArea = "singapore72";
            loadGraphStorage();
        }
    }

    public void startRouting(View view){
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                nextButton.setVisibility(View.VISIBLE);
                startButton.setVisibility(View.GONE);
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(newCameraPos), 2000);
            }
        });
    }

    public void nextPos(View view) {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                if (userCurrentPos + 2 < calculatedPoints.size()) {
                    LatLng newPosition = new LatLng(calculatedPoints.get(userCurrentPos + 1).getLatitude(), calculatedPoints.get(userCurrentPos + 1).getLongitude());
                    LatLng nextPosition = new LatLng(calculatedPoints.get(userCurrentPos + 2).getLatitude(), calculatedPoints.get(userCurrentPos + 2).getLongitude());
                    startMarker.setPosition(newPosition);
                    mapboxMap.removePolyline(calculatedPolylines.get(userCurrentPos));

                    Double bearing = getBearing(newPosition, nextPosition);
//                    logUser(Double.toString(bearing));
                    //ZOOM TO LOCATION
                    CameraPosition position = new CameraPosition.Builder()
                            .target(newPosition)
                            .zoom(22) // Sets the zoom
                            .tilt(60)
                            .bearing(bearing)
                            .build(); // Creates a CameraPosition from the builder
                    mapboxMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition(position), 2000);

                    userCurrentPos++;
                } else {
                    if (end == stairsPoint || end == preferredElevatorPoint) {
                        // Change to current level
                        currentLevel = roomList.get(selectedLocationIndex).getLevel();

                        if (currentLevel == 4) {
                            mapboxMap.setStyleUrl(getString(R.string.mapbox_url2));
                            currentArea = "singapore72";
                            loadGraphStorage();
                        }

                        mapboxMap.clear();
                        userCurrentPos = 0;
                        if(preferredElevator == "stairs"){
                            start = stairsPoint;
                        }else if(preferredElevator == "elevator"){
                            start = preferredElevatorPoint;
                        }
                        // Add the marker to the map
                        startMarker = mapboxMap.addMarker(createMarkerItem(start, R.drawable.start, "You Are Here!", ""));
                        String[] coords = destination_coords.get(selectedLocationIndex).split(",");
                        Double lat = Double.parseDouble(coords[0]);
                        Double lng = Double.parseDouble(coords[1]);
                        LatLng point = new LatLng(lat, lng);
                        end = point;

                        // Add the marker to the map
                        mapboxMap.addMarker(createMarkerItem(end, R.drawable.end, "Destination", ""));

                        // Calculate Shortest Path
                        calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                                end.getLongitude(), mapboxMap);
                    } else {
                        logUser("you have reached your destination");
                        isEndFirst = false;
                        appDrawer.closeDrawer();
                        //                    mapboxMap.clear();
                    }
                }
            }
        });
    }

    private double getBearing(LatLng first, LatLng second) {
        double longitude1 = first.getLongitude();
        double longitude2 = second.getLongitude();
        double latitude1 = Math.toRadians(first.getLatitude());
        double latitude2 = Math.toRadians(second.getLatitude());
        double longDiff = Math.toRadians(longitude2 - longitude1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private void resetMapView() {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                //ZOOM TO LOCATION
                final LatLng zoomLocation = new LatLng(1.3792949602146791, 103.84983998176449);
                CameraPosition position = new CameraPosition.Builder()
                        .target(zoomLocation)
                        .zoom(19) // Sets the zoom
                        .tilt(0)
                        .bearing(0)
                        .build(); // Creates a CameraPosition from the builder
                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 2000);
            }
        });
    }

    public void goToNearby(View view){
        appDrawer.switchDrawer(3);

        ArrayList<Nearby> nearbies = new ArrayList<>();
        nearbies.add(new Nearby("ATM",R.drawable.atm));
        nearbies.add(new Nearby("Toilets", R.drawable.toilet));
        nearbies.add(new Nearby("AED", R.drawable.aed));
        nearbies.add(new Nearby("Elevator", R.drawable.elevator));

        final NearbyAdapter adapter=new NearbyAdapter(RoutingActivity.this,nearbies);
        nearbyListView.setAdapter(adapter);

        nearbyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                goToNearbyATM(view);
            }
        });

        nearbySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String arg0) {
                // TODO Auto-generated method stub
                return false;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                // TODO Auto-generated method stub
                adapter.getFilter().filter(query);
                return false;
            }
        });
    }

    public void showATM(View view){
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                if (atmShowed == false) {
                    //ZOOM TO LOCATION
                    LatLng zoomLocation = new LatLng(1.3792949602146791, 103.84983998176449);
                    CameraPosition position = new CameraPosition.Builder()
                            .target(zoomLocation)
                            .zoom(19) // Sets the zoom
                            .tilt(0)
                            .bearing(0)
                            .build(); // Creates a CameraPosition from the builder
                    mapboxMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition(position), 2000);
                    for (int i = 0; i < atmList.size(); i++) {
                        LatLng point = new LatLng(atmList.get(i).getLat(), atmList.get(i).getLng());
                        Marker marker = mapboxMap.addMarker(createMarkerItem(point, R.drawable.atm, atmList.get(i).getName(), ""));
                        atmList.get(i).setMarker(marker);
                    }
                    atmShowed = true;
                } else {
                    //ZOOM TO LOCATION
                    LatLng zoomLocation = new LatLng(start.getLatitude(), start.getLongitude());
                    CameraPosition position = new CameraPosition.Builder()
                            .target(zoomLocation)
                            .zoom(22) // Sets the zoom
                            .tilt(0)
                            .bearing(0)
                            .build(); // Creates a CameraPosition from the builder
                    mapboxMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition(position), 2000);
                    for (int i = 0; i < atmList.size(); i++) {
                        mapboxMap.removeMarker(atmList.get(i).getMarker());
                    }
                    atmShowed = false;
                }
            }
        });
    }

    public void goToNearbyATM(View view) {
        double minDistance = atmList.get(0).getDistance();
        int minIndex = 0;
        for (int i = 1; i < atmList.size(); i++) {
            if (atmList.get(i).getDistance() < minDistance) {
                minDistance = atmList.get(i).getDistance();
                minIndex = i;
            }
        }
        LatLng point = new LatLng(atmList.get(minIndex).getLat(), atmList.get(minIndex).getLng());
        end = point;

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                // Add the marker to the map
                mapboxMap.addMarker(createMarkerItem(end, R.drawable.atm, "Destination", ""));

                // Calculate Shortest Path
                calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                        end.getLongitude(), mapboxMap);

                appDrawer.switchDrawer(2);
            }
        });
    }

    public void preferElevator(View view){
        preferredElevator = "elevator";
        double minDistance = elevatorList.get(0).getDistance();
        int minIndex = 0;
        for (int i = 1; i < elevatorList.size(); i++) {
            if (elevatorList.get(i).getDistance() < minDistance) {
                minDistance = elevatorList.get(i).getDistance();
                minIndex = i;
            }
        }
        LatLng point = new LatLng(elevatorList.get(minIndex).getLat(), elevatorList.get(minIndex).getLng());
        preferredElevatorPoint = point;
        end = point;
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                appDrawer.switchDrawer(2);
                // Calculate Shortest Path
                calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                        end.getLongitude(),mapboxMap);
            }
        });
    }

    public void preferStairs(View view){
        preferredElevator = "stairs";
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                end = stairsPoint;
                appDrawer.switchDrawer(2);

                // Calculate Shortest Path
                calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
                        end.getLongitude(),mapboxMap);
            }
        });
    }

    private void finishPrepare() {
        prepareInProgress = false;
    }

    // Jian Wei - Calculate shortest path
    public void calcPath(final double fromLat, final double fromLon, final double toLat, final double toLon, final MapboxMap mapboxMap) {

        log("calculating path ...");
        new AsyncTask<Void, Void, PathWrapper>() {
            float time;

            protected PathWrapper doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                        setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
                req.getHints().
                        put(Parameters.Routing.INSTRUCTIONS, "false");
                GHResponse resp = hopper.route(req);
                time = sw.stop().getSeconds();
                return resp.getBest();
            }

            protected void onPostExecute(PathWrapper resp) {
                if (!resp.hasErrors()) {
                    log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
                            + toLon + " found path with distance:" + resp.getDistance()
                            / 1000f + ", nodes:" + resp.getPoints().getSize() + ", time:"
                            + time + " " + resp.getDebugInfo());
//                    logUser("the route is " + (int) (resp.getDistance() / 100) / 10f+ "km long, time:" + resp.getTime() / 60000f + "min, debug:" + time);

                    List<LatLng> points = createPathLayer(resp);
                    List<Polyline> polylines = new ArrayList<Polyline>();

                    if (points.size() > 0) {
                        for (int i = 0; i < points.size() - 1; i++) {
                            // Draw polyline on map
                            Polyline polyline = mapboxMap.addPolyline(new PolylineOptions()
                                    .add(points.get(i))
                                    .add(points.get(i + 1))
                                    .color(Color.parseColor("#F27777"))
                                    .width(3));
                            polylines.add(polyline);
                        }
                    }
                    setCalculatedPointsAndPolylines(points, polylines);

                    Double bearing = getBearing(points.get(0), points.get(1));

                    //ZOOM TO LOCATION
                    LatLng zoomLocation = new LatLng(start.getLatitude(), start.getLongitude());
                    newCameraPos = new CameraPosition.Builder()
                            .target(zoomLocation)
                            .zoom(22) // Sets the zoom
                            .tilt(60)
                            .bearing(bearing)
                            .build(); // Creates a CameraPosition from the builder
                    resetMapView();
                    startButton.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.GONE);
                    startMarker.hideInfoWindow();
                } else {
                    logUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
    }

    // Jian Wei - get Distance between 2 points
    public void getDistance(final double fromLat, final double fromLon, final double toLat, final double toLon, final int index, final String type) {
        new AsyncTask<Void, Void, PathWrapper>() {
            float time;

            protected PathWrapper doInBackground(Void... v) {
                StopWatch sw = new StopWatch().start();
                GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                        setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
                req.getHints().
                        put(Parameters.Routing.INSTRUCTIONS, "false");
                GHResponse resp = hopper.route(req);
                time = sw.stop().getSeconds();
                return resp.getBest();
            }

            protected void onPostExecute(PathWrapper resp) {
                if (!resp.hasErrors()) {
                    if(type == "ATM"){
                        setATMDistanceFromAsync(resp.getDistance(), index);
                    }else if(type == "Elevator"){
                        setElevatorDistanceFromAsync(resp.getDistance(), index);
                    }
                } else {
                    logUser("Error:" + resp.getErrors());
                }
            }
        }.execute();
    }

    // Jian Wei - get distance from async task for ATM
    private void setATMDistanceFromAsync(Double distance,int index) {
        calculatedDistance = distance;
        atmList.get(index).setDistance(distance);
    }

    // Jian Wei - get distance from async task for Elevator
    private void setElevatorDistanceFromAsync(Double distance,int index) {
        calculatedDistance = distance;
        elevatorList.get(index).setDistance(distance);
    }

    // Jian Wei - get the calculated points from asynctask
    private void setCalculatedPointsAndPolylines(List<LatLng> points, List<Polyline> polylines) {
        calculatedPoints = points;
        calculatedPolylines = polylines;
    }

    // Jian Wei - Create the shortest path's polyline on the map
    private List<LatLng> createPathLayer(PathWrapper response) {
        List<LatLng> geoPoints = new ArrayList<>();
        PointList pointList = response.getPoints();
        for (int i = 0; i < pointList.getSize(); i++) {
            geoPoints.add(new LatLng(pointList.getLatitude(i), pointList.getLongitude(i)));
        }
        return geoPoints;
    }

    // Jian Wei - Create an Icon object for the marker to use
    private MarkerViewOptions createMarkerItem(LatLng point, int resource, String title, String snippet) {
        IconFactory iconFactory = IconFactory.getInstance(RoutingActivity.this);
        Drawable iconDrawable = ContextCompat.getDrawable(RoutingActivity.this, resource);
        Bitmap bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
        // Scale it to 50 x 50
        Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 80, 80, true));
        Icon icon = iconFactory.fromDrawable(d);

        MarkerViewOptions marker = new MarkerViewOptions()
                .position(point)
                .icon(icon)
                .anchor(0.5f, 1)
                .title(title)
                .snippet(snippet);

        return marker;
    }

    // Jian Wei - is to Log a string and Toast at the same time for easier debugging
    private void log(String str) {
        Log.e("GH", str);
    }

    private void log(String str, Throwable t) {
        Log.e("GH", str, t);
    }

    private void logUser(String str) {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private void ocrCheckFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            ocrCopyFiles();
        }
        if(dir.exists()) {
            String datafilepath = ocrDatapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                ocrCopyFiles();
            }
        }
    }

    private void ocrCopyFiles() {
        try {
            String filepath = ocrDatapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void AppCheckRoute(String room) {
        isEndFirst = true;
        selectedLocationIndex = getIndexByname(room);
//        Log.e("ACR INDEX:" , String.valueOf(acrIndex));
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.clear();
                if (roomList.get(selectedLocationIndex).getLevel() == currentLevel) {
                    String[] coords = destination_coords.get(selectedLocationIndex).split(",");
                    Double lat = Double.parseDouble(coords[0]);
                    Double lng = Double.parseDouble(coords[1]);
                    LatLng point = new LatLng(lat, lng);
                    end = point;

                    // Add the marker to the map
                    mapboxMap.addMarker(createMarkerItem(end, R.drawable.end, "Destination", ""));

                    openQRScanner(roomListView);
                } else {
                    end = stairsPoint;

                    openQRScanner(roomListView);
                }
            }
        });
    }

    public int getIndexByname(String rName) {
        Log.e("rName:" , rName);
        for(int i = 0; i < roomList.size(); i++){
            Log.e("roomList", String.valueOf(roomList.get(i).getName()));
            if (roomList.get(i).getName().equals(rName)) {
                return roomList.indexOf(roomList.get(i));
            }else{

            }
        }
        return -1;
    }
}
