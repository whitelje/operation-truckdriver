package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.Manifest;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.RoadsApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.SnappedPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TripReviewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TripReviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TripReviewFragment extends Fragment {
    // TODO: Rename and change types of parameters
    private String tripID;

    private GoogleMap mGmap;
    private GeoApiContext mContext;
    private static final int PAGE_SIZE_LIMIT = 100;
    private static final int PAGINATION_OVERLAP = 5;
    List<LatLng> mCapturedLocations = new ArrayList<>();
    List<SnappedPoint> mSnappedPoints = new ArrayList<>();

    private OnFragmentInteractionListener mListener;
    private List<DataPoint> mList;
    private TextView startView;
    private TextView endView;
    private TextView distView;
    private TextView timeView;
    private TextView stopsView;
    private TextView mpgView;
    private TextView speedView;

    public TripReviewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TripReviewFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TripReviewFragment newInstance(String tripId) {
        TripReviewFragment fragment = new TripReviewFragment();
        Bundle args = new Bundle();
        args.putString("Trip", tripId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tripID = getArguments().getString("Trip");
        }

        mList = new ArrayList<>();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mList = new ArrayList<>();

        Firebase mFirebaseTripRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_TRIPS + "/" + tripID);
        Firebase something = mFirebaseTripRef.child("points");
        something.addListenerForSingleValueEvent(new DataPointListener());

        LatLng p1 = new LatLng(39.486879, -87.259233);
        LatLng p2 = new LatLng(39.495654, -87.257674);
        LatLng p3 = new LatLng(39.495624, -87.260764);
        LatLng p4 = new LatLng(39.493290, -87.271722);
        LatLng p5 = new LatLng(39.486343, -87.303550);
        LatLng p6 = new LatLng(39.481084, -87.322716);
        LatLng p7 = new LatLng(39.481738, -87.323692);
        mCapturedLocations.add(p1);
        mCapturedLocations.add(p2);
        mCapturedLocations.add(p3);
        mCapturedLocations.add(p4);
        mCapturedLocations.add(p5);
        mCapturedLocations.add(p6);
        mCapturedLocations.add(p7);

        mContext = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));

        SupportMapFragment mMap = SupportMapFragment.newInstance();
        mMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //Do Nothing
                } else {
                    googleMap.getUiSettings().setMapToolbarEnabled(false);
                    googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                    googleMap.getUiSettings().setScrollGesturesEnabled(false);
                    googleMap.getUiSettings().setZoomGesturesEnabled(false);
                    mGmap = googleMap;
                    drawLines();
                }
            }
        });

        FragmentTransaction ft =  getFragmentManager().beginTransaction();
        ft.replace(R.id.trip_review_map_container, mMap, "review");
        ft.commit();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trip_review, container, false);
        startView = (TextView) view.findViewById(R.id.start_city);
        endView = (TextView) view.findViewById(R.id.end_city);
        distView = (TextView) view.findViewById(R.id.tripReview_distance);
        timeView = (TextView) view.findViewById(R.id.tripReview_time);
        stopsView = (TextView) view.findViewById(R.id.tripReview_stops);
        mpgView = (TextView) view.findViewById(R.id.tripReview_mpg);
        speedView = (TextView) view.findViewById(R.id.tripReview_speed);
        Button btn = (Button) view.findViewById(R.id.cancel_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelButtonClicked();
            }
        });

        return view;
    }

    private List<SnappedPoint> getSnapToRoads(GeoApiContext context) throws Exception {
        List<SnappedPoint> snappedPoints = new ArrayList<>();

        int offset = 0;
        while (offset < mCapturedLocations.size()) {
            // Calculate which points to include in this request. We can't exceed the APIs
            // maximum and we want to ensure some overlap so the API can infer a good location for
            // the first few points in each request.
            if (offset > 0) {
                offset -= PAGINATION_OVERLAP;   // Rewind to include some previous points
            }
            int lowerBound = offset;
            int upperBound = Math.min(offset + PAGE_SIZE_LIMIT, mCapturedLocations.size());

            // Grab the data we need for this page.
            LatLng[] page = mCapturedLocations
                    .subList(lowerBound, upperBound)
                    .toArray(new LatLng[upperBound - lowerBound]);

            // Perform the request. Because we have interpolate=true, we will get extra data points
            // between our originally requested path. To ensure we can concatenate these points, we
            // only start adding once we've hit the first new point (i.e. skip the overlap).
            SnappedPoint[] points = RoadsApi.snapToRoads(context, false, page).await();
            boolean passedOverlap = false;
            for (SnappedPoint point : points) {
                if (offset == 0 || point.originalIndex >= PAGINATION_OVERLAP) {
                    passedOverlap = true;
                }
                if (passedOverlap) {
                    snappedPoints.add(point);
                }
            }

            offset = upperBound;
        }

        return snappedPoints;
    }

    private void drawLines() {
        try {
            mSnappedPoints = getSnapToRoads(mContext);

            com.google.android.gms.maps.model.LatLng[] mapPoints =
                    new com.google.android.gms.maps.model.LatLng[mSnappedPoints.size()];
            int i = 0;
            LatLngBounds.Builder bounds = new LatLngBounds.Builder();
            for (SnappedPoint point : mSnappedPoints) {
                mapPoints[i] = new com.google.android.gms.maps.model.LatLng(point.location.lat,
                        point.location.lng);
                bounds.include(mapPoints[i]);
                i += 1;
            }

            mGmap.addPolyline(new PolylineOptions().add(mapPoints).color(Color.BLUE).width(4));
            mGmap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 35));
        } catch(Exception e){
            //do nothing
        }
    }

    public void onCancelButtonClicked() {
        if (mListener != null) {
            mListener.onCancelButtonClicked();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        void onCancelButtonClicked();
    }

    private class DataPointListener implements ValueEventListener {
        FutureTask<String> ft = null;
        ExecutorService exService = Executors.newSingleThreadExecutor();

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                Firebase fb = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_POINTS + "/" + ds.getKey());
                fb.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        DataPoint dp = dataSnapshot.getValue(DataPoint.class);
                        mList.add(dp);
                        // approved by Google employee
                        Collections.sort(mList);
                        if (ft != null) {
                            ft.cancel(true);
                        }
                        UpdateUI call = new UpdateUI();
                        ft = new FutureTask<>(call);
                        exService.execute(ft);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    private class UpdateUI implements Callable<String> {

        @Override
        public String call() throws Exception {
            Thread.sleep(1000);
            updateUI();
            return null;
        }
    }

    private void updateUI() {
        Log.d(Constants.TAG, mList.size() + "");
        Geocoder geocoder = new Geocoder(getContext());
        if(mList.size() < 3) {
            return;
        }
        DataPoint firstPoint = mList.get(1);
        DataPoint lastPoint = mList.get(mList.size() - 1);
        Log.d(Constants.TAG, firstPoint.time + "");
        Log.d(Constants.TAG, lastPoint.time + "");
        try {
            List<Address> firstLocList =
                    geocoder.getFromLocation(firstPoint.getPosLat(), firstPoint.getPosLng(), 1);
            List<Address> lastLocList =
                    geocoder.getFromLocation(lastPoint.getPosLat(), lastPoint.getPosLng(), 1);
            String firstCity = firstLocList.get(0).getLocality();
            String  lastCity =  lastLocList.get(0).getLocality();
            String firstState = firstLocList.get(0).getAdminArea();
            String  lastState =  lastLocList.get(0).getAdminArea();
            final String firstLoc = firstCity + ", " + firstState;
            final String  lastLoc =  lastCity + ", " +  lastState;



            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startView.setText(firstLoc);
                    endView.setText(lastLoc);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        double dist = lastPoint.getDistance();
        final String distanceString = String.format("%.0f mi",dist);

        long timeDelta = lastPoint.getTime() - firstPoint.getTime();
        timeDelta = timeDelta / 1000;
        int seconds = (int) timeDelta % 60;
        int minutes = (int) (timeDelta % 3600) / 60;
        int hours = (int) (timeDelta % 86400) / 3600;
        final String timeString = String.format("%d:%02d:%02d", hours, minutes, seconds);

        double mpg = lastPoint.getTripMpg();
        final String mpgString = mpg+"";

        double avg_speed = dist / timeDelta / 1000;
        final String avgSpeedString = String.format("%d mph", (int) avg_speed);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                distView.setText(distanceString);
                timeView.setText(timeString);
                stopsView.setText("0");
                mpgView.setText(mpgString);
                speedView.setText(avgSpeedString);

            }
        });
    }
}
