package sg.edu.nyp.alexia;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Window;

import com.graphhopper.GraphHopper;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class RoutingActivity extends Activity {
    private MapView mapView;
    private GraphHopper hopper;
    private LatLng start;
    private LatLng end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_routing);

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
//                        Log.e("coord",point.toString());
//                        if (!isReady()) {
//                            logUser("Load Map or Graph failed!");
//                            return;
//                        }
//
//                        if (shortestPathRunning) {
//                            logUser("Calculation still in progress");
//                            return;
//                        }

                        if (start != null && end == null) {
                            end = point;
//                            shortestPathRunning = true;

                            // Add the marker to the map
                            mapboxMap.addMarker(createMarkerItem(end, R.drawable.end, "Destination", ""));

                            // Calculate Shortest Path
//                            calcPath(start.getLatitude(), start.getLongitude(), end.getLatitude(),
//                                    end.getLongitude(),mapboxMap);

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

    private MarkerViewOptions createMarkerItem(LatLng point, int resource, String title, String snippet){
        // Create an Icon object for the marker to use
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
}
