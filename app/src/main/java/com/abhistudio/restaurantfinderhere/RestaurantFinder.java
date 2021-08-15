package com.abhistudio.restaurantfinderhere;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoCorridor;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.Point2D;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.gestures.GestureState;
import com.here.sdk.gestures.LongPressListener;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolygon;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapView;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;

import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Section;
import com.here.sdk.routing.SectionNotice;
import com.here.sdk.routing.Waypoint;
import com.here.sdk.search.CategoryQuery;
import com.here.sdk.search.Place;
import com.here.sdk.search.PlaceCategory;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.sdk.search.TextQuery;

import java.util.ArrayList;
import java.util.List;

/**
 *  RestaurantFinder class for finding restaurants along a route
 * using the HERE SDK for Android (Explore Edition).
 */
public class RestaurantFinder {

    // Variables for calculating the center of a GeoCircle with a list of GeoCoordinates
    private static double pi = Math.PI / 180;
    private static double xpi = 180 / Math.PI;

    private Context context;
    private MapView mapView;
    private List<Waypoint> wayPoints = new ArrayList<>();
    private List<MapMarker> mapMarkers = new ArrayList<>();
    private List<MapPolyline> mapPolylines = new ArrayList<>();
    private List<MapPolygon> mapPolygons = new ArrayList<>();
    private RoutingEngine routingEngine;
    private SearchEngine searchEngine;
    private GeoCoordinates startGeoCoordinates;
    private GeoCoordinates destinationGeoCoordinates;
    private List<String> chargingStationsIDs = new ArrayList<>();
    private  List<GeoCoordinates> mapCoordinates;


    /**
     * Constructor for RestaurantFinder Class
     * Here, RoutingEngine a route from A to B with a number of waypoints in between
     * and SearchEngine for search, geocoding and suggestions of HERE services from HERE SDK is instantiated
     * */
    public RestaurantFinder(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        MapCamera camera = mapView.getCamera();
        double distanceInMeters = 1000 * 10;
        camera.lookAt(new GeoCoordinates(52.520798, 13.409408), distanceInMeters);

        try {
            routingEngine = new RoutingEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of RoutingEngine failed: " + e.error.name());
        }

        try {
            // Add search engine to search for places along a route.
            searchEngine = new SearchEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of SearchEngine failed: " + e.error.name());
        }
        // Gesture Listener for placing the Source and Destination Map Markers along which routing has to be done
        setGestureMapMarkers();
    }

    /**
     * Function for Gesture Listener for Map Markers
     * By Long pressing on anywhere on the map, the function helps add markers on the map for Routing
     * */
    public void setGestureMapMarkers(){
        mapView.getGestures().setLongPressListener(new LongPressListener() {
            @Override
            public void onLongPress(@NonNull GestureState gestureState, @NonNull Point2D point2D) {
                if(gestureState == GestureState.BEGIN){

                    // adding map marker with the user input touch point as (point2D)
                    addCircleMapMarker(mapView.viewToGeoCoordinates(point2D), R.drawable.green_dot," ");

                    /* Add the WayPoint to the mapview with the selected marker geo coordinates to the wayPoints list
                       later used for calculation of routing
                    */
                    wayPoints.add(new Waypoint(mapView.viewToGeoCoordinates(point2D)));
                }

            }
        });
    }

    /**
     * Listener for Restaurant search along a route Button
     * Calculates Car route based on start and destination coordinates selected by user through setGestureMapMarkers function
     * routingEngine -> takes 3 parameters (List<WayPoints>, CarOptions, RouteCallback)
     */
    public void addResRouteButtonClicked() {

        // Creating a car option
        CarOptions carOptions = new CarOptions();
        //carOptions.routeOptions.alternatives = 0;

        routingEngine.calculateRoute(wayPoints, carOptions, new CalculateRouteCallback() {
            @Override
            public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> list) {

                if (routingError != null) {
                    showDialog("Error while calculating a route: ", routingError.toString());
                    return;
                }
                // When routingError is null, routes is guaranteed to contain at least one route.
                Route route = list.get(0);
                // below function show route on the map with this function
                showRouteOnMap(route);
                // shows warning for route that can not be calculated
                logRouteViolations(route);
                // below function searchs and adds restaurants along the given route
                searchAlongARoute(route);

            }
        });
    }
    // A route may contain several warnings, for example, when a certain route option could not be fulfilled.
    // An implementation may decide to reject a route if one or more violations are detected.
    private void logRouteViolations(Route route) {
        List<Section> sections = route.getSections();
        for (Section section : sections) {
            for (SectionNotice notice : section.getSectionNotices()) {
                Log.d("RouteViolations", "This route contains the following warning: " + notice.code);
            }
        }
    }
    /**
     * showRouteOnMap Function takes the Route parameter and draws the route from point A to B
     * */
    private void showRouteOnMap(Route route) {
        // Show route as polyline.
        // GeoPolyline is a list of geographic coordinates representing the vertices of a polyline.
        GeoPolyline routeGeoPolyline;
        // we must instantiate the GeoPolyline in try-catch block to avoid errors for
        // when we have less than two vertices for showing route
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch (InstantiationErrorException e) {
            // It should never happen that a route polyline contains less than two vertices.
            return;
        }

        float widthInPixels = 20;
        // Selecting the width and color of the route
        MapPolyline routeMapPolyline = new MapPolyline(routeGeoPolyline,
                widthInPixels,
                Color.valueOf(0, 0.56f, 0.54f, 0.63f)); // RGBA

        // Adding the routeMapPolyline to the MapVIew
        mapView.getMapScene().addMapPolyline(routeMapPolyline);
        mapPolylines.add(routeMapPolyline);

    }

    /**
     * Main Function for searching the places along a given route
     * Perform a search for Restaurants along the found route.
     * Function searchAlongARoute, takes Route as parameter and
     * plots the Map Markers on the MapView at the found Geo-Coordinates
     * along the given route
    */
    private void searchAlongARoute(Route route) {

        // We specify here that we only want to include results
        // within a max distance of x meters from any point of the route.

        int halfWidthInMeters = 200;

        /**
         *                    Algorithm for finding restaurants along a route
         * We are calculating a center Geo Coordinate for a Geo Circle around the given route
         * and take radius for the geo circle as the length of the given route to cover the
         * restauarant coordinates around the given route.
         * Geo Circle represents a circle area in 2D space
        */
        // newCenter -> calculates the center based on the list of Geo Coordinates
        GeoCoordinates center = newCenter(route.getPolyline());
        double radius = route.getLengthInMeters();
        GeoCircle geoCircle = new GeoCircle(center,radius);

        /*
          We are performing TextQuery with the query text "restaurants" and around the geoCircle
          that we got from above calculation
        * */
        TextQuery textQuery = new TextQuery("restaurants", geoCircle);

        // maximum no. of items to search along the route
        int maxItems = 30;
        // Search Options with language code and maxItems
        SearchOptions searchOptions = new SearchOptions(LanguageCode.EN_US, maxItems);


        /**
         * Here, we are using the Search Engine instantiated in the constructor
         * SearchEngine.search take 3 parameters (TextQuery, SearchOptions, SearchCallback)
         */
        searchEngine.search(textQuery, searchOptions, new SearchCallback() {
            // After the search is completed we come to this block
            @Override
            public void onSearchCompleted(SearchError searchError, List<Place> items) {
                if (searchError != null) {
                    if (searchError == SearchError.POLYLINE_TOO_LONG) {
                        // Increasing halfWidthInMeters will result in less precise results with the benefit of a less
                        // complex route shape.
                        Log.d("Search", "Route too long or halfWidthInMeters too small.");
                    } else {
                        Log.d("Search", "No Restaurants found along the route. Error: " + searchError);
                    }
                    return;
                }

                // If error is null, it is guaranteed that the items will not be null.
                Log.d("Search","Search along route found " + items.size() + " Restaurants:");
                /**
                 * We have got a list of search restaurants around our Geo Circle as List of Places
                 * Place represents a location object, such as a country, a city, a point of interest (POI) etc.
                 * We loop every place instance and check for the given below condition and if that condition matches
                 * we add those instance of place (restaurant) on the MapView along the route

                **/
                for (Place place : items) {


                    /**
                     * To check for restaurants along the route we take all the coordinates along the
                     * given route using (route.getPolyline()) and calculate the distance
                     * between the place and the coordinates along the given route and if it
                     * is less than equal to halfWidthInMeters, we plot it on the map
                     * */
                    for(GeoCoordinates geo: route.getPolyline()){

                        //Log.d("Distance from polyline: ",String.valueOf(place.getGeoCoordinates().distanceTo(geo)));
                        if(place.getGeoCoordinates().distanceTo(geo) <= halfWidthInMeters){

                            // This is our desired restaurant instances
                            // We are ploting our instance using marker drawable and also pinning the title of restaurant with it
                            addCircleMapMarker(place.getGeoCoordinates(), R.drawable.marker, place.getTitle());

                            //Log.d("Filtered Distance from the polyline-> ",String.valueOf(place.getGeoCoordinates().distanceTo(geo)));

                        }
                    }
                }
            }

        });

    }
    /**
     *  clearMap(), clearRoute(), removePins(), clearWaypointMapMarker() Functions are used to clear the MapView
     *  when clearMapButtonClicked button is triggered
     * */
    public void clearMap() {
        clearWaypointMapMarker();
        clearRoute();
        wayPoints.clear();
        removePins();
    }
    // unpining all pins added to the views
    public void removePins(){
        List<MapView.ViewPin> viewPins = mapView.getViewPins();
        for(MapView.ViewPin p : viewPins){
            p.unpin();
        }
    }

    private void clearWaypointMapMarker() {
        for (MapMarker mapMarker : mapMarkers) {
            mapView.getMapScene().removeMapMarker(mapMarker);
        }
        mapMarkers.clear();
    }

    private void clearRoute() {
        for (MapPolyline mapPolyline : mapPolylines) {
            mapView.getMapScene().removeMapPolyline(mapPolyline);
        }
        mapPolylines.clear();
    }
    /**
     * Function addCircleMapMarker adds the marker points on the MapView
     * This function takes 3 arguments -> (GeoCoordinates,resourceId of Drawable, String Tittle for pinning)
     * */
    private void addCircleMapMarker(GeoCoordinates geoCoordinates, int resourceId, String title) {


        // MapImage represents a drawable resource that can be used by a MapMarker to be shown on the map
        MapImage mapImage = MapImageFactory.fromResource(context.getResources(), resourceId);
        // MapMarker is used to draw images on the map
        MapMarker mapMarker = new MapMarker(geoCoordinates, mapImage);
        /*
        getMapScene() -> gets the map scene associated with this map view
        This can be used to request different map schemes to be displayed in the map view,
         and to add and remove map items from the map.

         */
        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkers.add(mapMarker);

        // Initializing views
        TextView textView = new TextView(context.getApplicationContext());
        textView.setText(title.toString());
        textView.setTextSize(8);

        mapView.pinView(textView,geoCoordinates);

        // anchoring map marker of better view
        mapMarker.setAnchor(new Anchor2D(0.5,1.0));
    }

    /**
     * Simple Dialog for to show alert dialogs
     * */
    private void showDialog(String title, String message) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    /**
     * Function to calculate the center GeoCoordinates of a list of GeoCoordinates
     * for creating a Geo Circle
     * */
    public GeoCoordinates newCenter(List<GeoCoordinates> geoCoordinates) {

        /**
         *        Algorithm for center calculation of Geo Circle
         *
         * 1. Convert each lat/long pair into a unit-length 3D vector.
         * 2. Sum each of those vectors
         * 3. Normalise the resulting vector
         * 4. Convert back to spherical coordinates
         * */
        if (geoCoordinates.size() == 1) {
            return geoCoordinates.get(0);
        }
        double x = 0, y = 0, z = 0;

        for (GeoCoordinates c : geoCoordinates) {
            double latitude = c.latitude * pi, longitude = c.longitude* pi;
            double cl = Math.cos(latitude);
            x += cl * Math.cos(longitude);
            y += cl * Math.sin(longitude);
            z += Math.sin(latitude);
        }

        int total = geoCoordinates.size();

        x = x / total;
        y = y / total;
        z = z / total;

        double centralLongitude = Math.atan2(y, x);
        double centralSquareRoot = Math.sqrt(x * x + y * y);
        double centralLatitude = Math.atan2(z, centralSquareRoot);

        return new GeoCoordinates(centralLatitude * xpi, centralLongitude * xpi);
    }
}
