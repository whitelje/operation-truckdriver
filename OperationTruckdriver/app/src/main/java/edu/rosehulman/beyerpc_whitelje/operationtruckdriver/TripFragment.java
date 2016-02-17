package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.Button;
import android.widget.TextView;

import com.cardiomood.android.controls.gauge.SpeedometerGauge;
import com.firebase.client.Firebase;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


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
    public static final int MAP_UPDATE_INTERVAL = 5000;
    public static final int MAP_MIN_UPDATE_INTERVAL = 2500;
//    public static final int WRITE_INTERVAL = 30000;
    public static final int WRITE_INTERVAL = 5000;

    private String mTripId;
    private OnFragmentInteractionListener mListener;
    private GoogleMap mGmap;
    private LocationRequest mLocRequest;
    private GoogleApiClient mLocationClient;
    private long mLastWrite;
    private long mStartTime;
    private double mRPM = -1.0;
    private double mMPG = -1.0;
    private double mEngineTemp = -1.0;
    private double mOilPSI = -1.0;
    private double mOdometer = -1.0;
    private double mDistance = 0;
    private Firebase mTripFirebaseRef;
    private Firebase mTripPointFirebaseRef;
    private SpeedometerGauge mSpeedometer;
    private SpeedometerGauge mTimeGauge;
    private TextView mRPMView;
    private TextView mAverageMPGView;
    private TextView mEngineTempView;
    private TextView mOilPressureView;
    private TextView mOdometerView;
    private TextView mSpeedometerText;
    private TextView mTimeGaugeText;
    private TextView mDistanceView;


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
        setupFirebase();

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
                }
            }
        });

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.trip_map_container, mMap);
        ft.commit();

        View v = inflater.inflate(R.layout.fragment_trip, container, false);
        AddSpeedometerWidgets(v);
        getTextViews(v);

        Button end_btn = (Button) v.findViewById(R.id.tripFrag_endBtn);
        end_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                end_trip();
            }
        });

        return v;
    }

    private void getTextViews(View v) {
        mRPMView = (TextView) v.findViewById(R.id.tripFrag_rpm);
        mAverageMPGView = (TextView) v.findViewById(R.id.tripFrag_MPG);
        mEngineTempView = (TextView) v.findViewById(R.id.tripFrag_engine_temp);
        mOilPressureView = (TextView) v.findViewById(R.id.tripFrag_oil_psi);
        mOdometerView = (TextView) v.findViewById(R.id.tripFrag_odometer);
        mSpeedometerText = (TextView) v.findViewById(R.id.speedometer_text);
        mTimeGaugeText = (TextView) v.findViewById(R.id.time_gauge_text);
        mDistanceView = (TextView) v.findViewById(R.id.tripFrag_distance);
    }

    private void setupFirebase() {
        long currentTime = System.currentTimeMillis();

        mStartTime = currentTime;
        Firebase tripRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_TRIPS);
        tripRef = tripRef.push();
        mTripId = tripRef.getKey();
        Trip t = new Trip();
        t.setDate(currentTime);
        tripRef.setValue(t);

        String userId = SharedPreferencesUtils.getCurrentUser((MainActivity) mListener);
        Firebase UserFirebaseRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_USERS + "/" + userId);
        Map<String, Object> map = new HashMap<>();
        map.put(mTripId, true);
        UserFirebaseRef.child("trips").updateChildren(map);

        mTripFirebaseRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_TRIPS + "/" + mTripId);
        mTripPointFirebaseRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_POINTS);
    }

    private void AddSpeedometerWidgets(View v) {
        mSpeedometer = (SpeedometerGauge) v.findViewById(R.id.speedometer);
        mSpeedometer.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });

        mSpeedometer.setMaxSpeed(120);
        mSpeedometer.setMajorTickStep(20);
        mSpeedometer.setMinorTicks(1);

        // Configure value range colors
        mSpeedometer.addColoredRange(15, 50, Color.GREEN);
        mSpeedometer.addColoredRange(50, 75, Color.YELLOW);
        mSpeedometer.addColoredRange(75, 120, Color.RED);

        mSpeedometer.setSpeed(0, true);


        mTimeGauge = (SpeedometerGauge) v.findViewById(R.id.time_speedometer);
        mTimeGauge.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });

        mTimeGauge.setMaxSpeed(8);
        mTimeGauge.setMajorTickStep(1);
        mTimeGauge.setMinorTicks(3);

        // Configure value range colors
        mTimeGauge.addColoredRange(0, 5, Color.GREEN);
        mTimeGauge.addColoredRange(5, 7, Color.YELLOW);
        mTimeGauge.addColoredRange(7, 8, Color.RED);

        mTimeGauge.setSpeed(0, true);
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
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
            CalculateDistance(currentTime - mLastWrite);

            mLastWrite = currentTime;
            Log.d("LOG", "Write to Firebase");
            Firebase tripPoint = mTripPointFirebaseRef.push();
            DataPoint dp = new DataPoint();
            dp.setPosLat(location.getLatitude());
            dp.setPosLng(location.getLongitude());
            dp.setEngineTemp(mEngineTemp);
            dp.setTripMpg(mMPG);
            dp.setRpm(mRPM);
            dp.setOilPressure(mOilPSI);
            dp.setVehicleSpeed(mSpeedometer.getSpeed());
            dp.setOdometer(mOdometer);
            dp.setTime(currentTime - mStartTime);
            dp.setDistance(mDistance);
            tripPoint.setValue(dp);

            Map<String,Object> map = new HashMap<>();
            map.put(tripPoint.getKey(),true);
            mTripFirebaseRef.child("points").updateChildren(map);
        }
    }

    private void CalculateDistance(long time) {
        if(time > 1000000) return;

        time = time / 1000; //ms -> s
        double speed = mSpeedometer.getSpeed() / 3600; //mph -> mps

        mDistance += speed * time;
        mDistanceView.setText((int) mDistance + " MI");
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

    public void updateLabel(int pgn, double value) {
        long totalTime = System.currentTimeMillis() - mStartTime;
        int hrs = (int) TimeUnit.MILLISECONDS.toHours(totalTime);
        int mins = (int) TimeUnit.MILLISECONDS.toMinutes(totalTime);
        mTimeGauge.setSpeed(totalTime / (1000*60*60));
        mTimeGaugeText.setText(hrs + "hours " + mins + " mins");

        switch(pgn) {
            case 61444:
                mRPM = value;
                mRPMView.setText(value + "");
                break;
            case 65266:
                // MPG
                mMPG = value;
                mAverageMPGView.setText(value + "");
                break;
            case 65262:
                // Engine Temp
                mEngineTemp = value;
                mEngineTempView.setText(value + " F");
                break;
            case 65263:
                // Oil Pressure
                mOilPSI = value;
                mOilPressureView.setText(value + " PSI");
                break;
            case 65261:
                // Speed
                mSpeedometerText.setText(value + " MPH");
                mSpeedometer.setSpeed(value, true);
                break;
            case 65217:
                // Odometer
                mOdometer = value;
                mOdometerView.setText(value + " MI");
                break;
        }
    }

    public void end_trip() {
        mListener.onEndTripClicked();
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
        void onEndTripClicked();
    }
}
