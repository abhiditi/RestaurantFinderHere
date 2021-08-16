# RestaurantFinderHere
 finding restaurants along a route using the HERE SDK Android (Explore Edition).
## 
<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/7/7a/3D_Convex_Hull.tiff/lossless-page1-330px-3D_Convex_Hull.tiff.png" height="500">

### The Algo

 1. The idea is to create a Geo Circle around the route by taking route distance as radius and center as the center of all geocoordinates along the route
and then search for places around the Geo Circle boundary
 2. The Geo-Coordinates around the Geo Circle is filtered by considering a distance of x meters away from the route geo-coordinates along the route
 3. Finally, we get the places along a chosen route with the places that are on the route or x meters away from route

### MapView
Create MapView instance
<img src="https://raw.githubusercontent.com/abhiditi/RestaurantFinderHere/main/app/images/device-2021-08-15-223047.png" height="500">

### LongPress to select source and destination coordinates
<img src="https://raw.githubusercontent.com/abhiditi/RestaurantFinderHere/main/app/images/device-2021-08-15-223112.png" height="500">

### Drawing route according to map marker selected on MapView
After Selecting source and destination coordinates by long pressing, Press the **Restaurant** Button
Route is drawn between source and destination
<img src="https://raw.githubusercontent.com/abhiditi/RestaurantFinderHere/main/app/images/device-2021-08-15-223129.png" height="500">

## Searching Restaurant along the chosen route
Restaurants along the route under x meters away from route along it gets populated automatically
<img src="https://raw.githubusercontent.com/abhiditi/RestaurantFinderHere/main/app/images/device-2021-08-15-223148.png" height="500">

