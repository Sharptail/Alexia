package sg.edu.nyp.alexia;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

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
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RoutingActivity extends Activity {
    private MapView mapView;
    private GraphHopper hopper;
    private LatLng start;
    private LatLng end;
    private String currentArea = "singapore7";
    private String downloadURL;
    private File mapsFolder;
    private volatile boolean prepareInProgress = false;
    private volatile boolean shortestPathRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_routing);

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

        if (!mapsFolder.exists()){
            mapsFolder.mkdirs();
        }

        mapView = (MapView)findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        // Add a MapboxMap
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                //ZOOM TO LOCATION
                LatLng zoomLocation = new LatLng(1.3792949602146791,103.84983998176449);
                CameraPosition position = new CameraPosition.Builder()
                        .target(zoomLocation)
                        .zoom(19) // Sets the zoom
                        .build(); // Creates a CameraPosition from the builder

                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 2000);
                mapboxMap.setOnMapLongClickListener(new MapboxMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng point) {
                        if (!isReady()) {logUser("Load Map or Graph failed!");return;}
                        if (shortestPathRunning) {logUser("Calculation still in progress");return;}

                        if (start != null && end == null) {
                            end = point;
                            shortestPathRunning = true;

                            // Add the marker to the map
                            mapboxMap.addMarker(createMarkerItem(end, R.drawable.end, "Destination", ""));

                            // Calculate Shortest Path
                            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude(),mapboxMap);
                        }else{
                            start = point;
                            end = null;
                            mapboxMap.clear();
                            // Add the marker to the map
                            mapboxMap.addMarker(createMarkerItem(start, R.drawable.start, "Start", ""));

                        }
                    }
                });
            }
        });
        initFiles("singapore7");
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
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
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
        logUser("Loading graph, please wait");
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
                    logUser("Finished loading graph. Press long to define where to start and end the route.");
                }

                finishPrepare();
            }
        }.execute();
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
                    logUser("the route is " + (int) (resp.getDistance() / 100) / 10f
                            + "km long, time:" + resp.getTime() / 60000f + "min, debug:" + time);

                    List<LatLng> points = createPathLayer(resp);

                    if (points.size() > 0) {

                        // Draw polyline on map
                        mapboxMap.addPolyline(new PolylineOptions()
                                .addAll(points)
                                .color(Color.parseColor("#3bb2d0"))
                                .width(2));
                    }
                } else {
                    logUser("Error:" + resp.getErrors());
                }
                shortestPathRunning = false;
            }
        }.execute();
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
    private MarkerViewOptions createMarkerItem(LatLng point, int resource, String title, String snippet){
        IconFactory iconFactory = IconFactory.getInstance(RoutingActivity.this);
        Drawable iconDrawable = ContextCompat.getDrawable(RoutingActivity.this, resource);
        Bitmap bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
        // Scale it to 50 x 50
        Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 80, 80, true));
        Icon icon = iconFactory.fromDrawable(d);

        MarkerViewOptions marker = new MarkerViewOptions()
                .position(point)
                .icon(icon)
                .anchor(0.5f,1)
                .title(title)
                .snippet(snippet);

        return marker;
    }

    // Jian Wei - is to Log a string and Toast at the same time for easier debugging
    private void log(String str) {
        Log.i("GH", str);
    }

    private void log(String str, Throwable t) {
        Log.i("GH", str, t);
    }

    private void logUser(String str) {
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }
}
