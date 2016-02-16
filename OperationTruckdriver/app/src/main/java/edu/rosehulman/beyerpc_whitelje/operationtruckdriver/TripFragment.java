package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TripFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TripFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TripFragment extends Fragment implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    public static final int MAP_UPDATE_INTERVAL = 10000;
    public static final int MAP_MIN_UPDATE_INTERVAL = 7500;
    public static final int WRITE_INTERVAL = 30000;

    // TODO: Rename and change types of parameters
    private String mParam1;

    private OnFragmentInteractionListener mListener;
    private GoogleMap mGmap;
    private LocationRequest mLocRequest;
    private GoogleApiClient mLocationClient;
    private long mLastWrite;

    public TripFragment() {
        // Required empty public constructor
    }

    public static TripFragment newInstance() {
        TripFragment fragment = new TripFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    private Location getMyLocation() {
        // Get location from GPS if it's available
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //do nothing
        }
        Location myLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        // Location wasn't found, check the next most accurate place for the current location
        if (myLocation == null) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            // Finds a provider that matches the criteria
            String provider = lm.getBestProvider(criteria, true);
            // Use the provider to get the last known location
            myLocation = lm.getLastKnownLocation(provider);
        }

        return myLocation;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLocationClient = new GoogleApiClient.Builder(getContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Inflate the layout for this fragment
        Location loc = getMyLocation();
        final LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
        CameraPosition camera = new CameraPosition(pos, 15, 0, 0);

        GoogleMapOptions options = new GoogleMapOptions().camera(camera);
        SupportMapFragment mMap = SupportMapFragment.newInstance(options);
        mMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //Do Nothing
                } else {
                    googleMap.setMyLocationEnabled(true);
                    googleMap.getUiSettings().setMapToolbarEnabled(false);
                    googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                    googleMap.getUiSettings().setScrollGesturesEnabled(false);
                    googleMap.getUiSettings().setZoomGesturesEnabled(false);
                    mGmap = googleMap;
//                    drawLines();
                }
            }
        });

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.trip_map_container, mMap);
        ft.commit();

        return inflater.inflate(R.layout.fragment_trip, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    public void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        long currentTime = System.currentTimeMillis();
        Log.d("LOCHANGE", "location changed: " + location.getLatitude() + ", " + location.getLongitude());
        LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
        mGmap.moveCamera(CameraUpdateFactory.newLatLng(loc));

        if(currentTime - mLastWrite > WRITE_INTERVAL){
            //TODO: Write to Database
            mLastWrite = currentTime;
            Log.d("LOG", "Write to Firebase");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocRequest = LocationRequest.create()
                .setInterval(MAP_UPDATE_INTERVAL)
                .setFastestInterval(MAP_MIN_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Do Nothing
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("SUS", "GoogleApiClient connection has been suspend");
    }

    @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("APICLIENT", "GoogleApiClient connection has failed");
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

//    //TODO: MOVE THIS TO PROPER FRAGMENT
////    mContext = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));
////    LatLng p1 = new LatLng(39.486879, -87.259233);
////    LatLng p2 = new LatLng(39.495654, -87.257674);
////    LatLng p3 = new LatLng(39.495624, -87.260764);
////    LatLng p4 = new LatLng(39.493290, -87.271722);
////    LatLng p5 = new LatLng(39.486343, -87.303550);
////    LatLng p6 = new LatLng(39.481084, -87.322716);
////    LatLng p7 = new LatLng(39.481738, -87.323692);
////    mCapturedLocations.add(p1);
////    mCapturedLocations.add(p2);
////    mCapturedLocations.add(p3);
////    mCapturedLocations.add(p4);
////    mCapturedLocations.add(p5);
////    mCapturedLocations.add(p6);
////    mCapturedLocations.add(p7);
//
////    private GoogleMap mGmap;
//    private GeoApiContext mContext;
//    private static final int PAGE_SIZE_LIMIT = 100;
//    private static final int PAGINATION_OVERLAP = 5;
//    List<LatLng> mCapturedLocations = new ArrayList<>();
//    List<SnappedPoint> mSnappedPoints = new ArrayList<>();
//
//    private List<SnappedPoint> getSnapToRoads(GeoApiContext context) throws Exception {
//        List<SnappedPoint> snappedPoints = new ArrayList<>();
//
//        int offset = 0;
//        while (offset < mCapturedLocations.size()) {
//            // Calculate which points to include in this request. We can't exceed the APIs
//            // maximum and we want to ensure some overlap so the API can infer a good location for
//            // the first few points in each request.
//            if (offset > 0) {
//                offset -= PAGINATION_OVERLAP;   // Rewind to include some previous points
//            }
//            int lowerBound = offset;
//            int upperBound = Math.min(offset + PAGE_SIZE_LIMIT, mCapturedLocations.size());
//
//            // Grab the data we need for this page.
//            LatLng[] page = mCapturedLocations
//                    .subList(lowerBound, upperBound)
//                    .toArray(new LatLng[upperBound - lowerBound]);
//
//            // Perform the request. Because we have interpolate=true, we will get extra data points
//            // between our originally requested path. To ensure we can concatenate these points, we
//            // only start adding once we've hit the first new point (i.e. skip the overlap).
//            SnappedPoint[] points = RoadsApi.snapToRoads(context, false, page).await();
//            boolean passedOverlap = false;
//            for (SnappedPoint point : points) {
//                if (offset == 0 || point.originalIndex >= PAGINATION_OVERLAP) {
//                    passedOverlap = true;
//                }
//                if (passedOverlap) {
//                    snappedPoints.add(point);
//                }
//            }
//
//            offset = upperBound;
//        }
//
//        return snappedPoints;
//    }
//
//    private void drawLines() {
//        try {
//            mSnappedPoints = getSnapToRoads(mContext);
//
//            com.google.android.gms.maps.model.LatLng[] mapPoints =
//                    new com.google.android.gms.maps.model.LatLng[mSnappedPoints.size()];
//            int i = 0;
//            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
//            for (SnappedPoint point : mSnappedPoints) {
//                mapPoints[i] = new com.google.android.gms.maps.model.LatLng(point.location.lat,
//                        point.location.lng);
//                bounds.include(mapPoints[i]);
//                i += 1;
//            }
//
//            mGmap.addPolyline(new PolylineOptions().add(mapPoints).color(Color.BLUE).width(4));
//            mGmap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 2));
//        } catch(Exception e){
//            //do nothing
//        }
//
//    }

}
