package com.abhistudio.restaurantfinderhere;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import android.util.Log;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Permission Requestor Class for requesting Andorid Permissions
    private PermissionsRequestor permissionsRequestor;
    // View to display Here Maps
    private MapView mapView;
    // Class to search Restaurant along any route
    private  RestaurantFinder restaurantFinder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a MapView instance from layout.
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);

        handleAndroidPermissions();


    }
    private void handleAndroidPermissions() {
        permissionsRequestor = new PermissionsRequestor(this.getParent());
        permissionsRequestor.request(new PermissionsRequestor.ResultListener(){

            @Override
            public void permissionsGranted() {
                // Permissions granted by user then load the Map Scene
                loadMapScene();
            }

            @Override
            public void permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.");
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsRequestor.onRequestPermissionsResult(requestCode, grantResults);
    }

    private void loadMapScene() {
        // Normal Day Map is loaded with MapScheme
        mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapError mapError) {
                if (mapError == null) {
                    // if no error on loading then instatiate the Restaurant search class
                    restaurantFinder = new RestaurantFinder(MainActivity.this, mapView);

                } else {
                    Log.d(TAG, "Loading map failed: mapErrorCode: " + mapError.name());
                }
            }
        });
    }

    /** Function to listen changes to Search query for restaurants
     */
    public void addResRouteButtonClicked(View view) {
        restaurantFinder.addResRouteButtonClicked();
    }

    /** Function to clear the map views
     */
    public void clearMapButtonClicked(View view) {
        restaurantFinder.clearMap();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        Log.d("On Pause:" ,"map on pause called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        Log.d("On Resume:" ,"map on resume called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        Log.d( "On Destroy: ","map on destroy called");
    }
}