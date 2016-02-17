package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.Manifest;
import android.content.Context;
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

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.RoadsApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.SnappedPoint;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mList = new ArrayList<>();

        Firebase mFirebaseTripRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_TRIPS + "/" + tripID);
        Firebase something = mFirebaseTripRef.child("points");
        something.addListenerForSingleValueEvent(new DataPointListener());

        mContext = new GeoApiContext().setApiKey(getString(R.string.google_maps_key));
        final com.google.android.gms.maps.model.LatLng pos = new  com.google.android.gms.maps.model.LatLng(39.50, -98.35);

        CameraPosition camera = new CameraPosition(pos, 3, 0, 0);
        GoogleMapOptions options = new GoogleMapOptions().camera(camera);
        SupportMapFragment mMap = SupportMapFragment.newInstance(options);
        mMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //Do Nothing
                } else {
                    googleMap.getUiSettings().setMapToolbarEnabled(false);
                    googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                    googleMap.getUiSettings().setScrollGesturesEnabled(true);
                    googleMap.getUiSettings().setZoomGesturesEnabled(true);
                    mGmap = googleMap;
                }
            }
        });

        FragmentTransaction ft =  getFragmentManager().beginTransaction();
        ft.replace(R.id.trip_review_map_container, mMap, "review");
        ft.commit();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trip_review, container, false);
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
            SnappedPoint[] points = RoadsApi.snapToRoads(context, true, page).await();
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
            mGmap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 35));
        } catch(Exception e){
            Log.d("ERR", e.getMessage());
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
     * <p/>
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
            for(DataSnapshot ds : dataSnapshot.getChildren()) {
                Firebase fb = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_POINTS + "/" + ds.getKey());
                fb.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        DataPoint dp = dataSnapshot.getValue(DataPoint.class);
                        mList.add(dp);
                        Collections.sort(mList);
                        if(ft != null) {
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

        getActivity().runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        for (DataPoint dp : mList) {
                            mCapturedLocations.add(new LatLng(dp.getPosLat(), dp.getPosLng()));
                        }
                        drawLines();
                    }
                }
        );
    }
}
