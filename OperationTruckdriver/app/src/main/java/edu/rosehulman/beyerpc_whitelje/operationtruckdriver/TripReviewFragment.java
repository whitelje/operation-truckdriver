package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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
            mParam1 = getArguments().getString("Trip");
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mList = new ArrayList<>();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        Firebase mFirebaseTripRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_TRIPS + "/" + mParam1);
        Firebase something = mFirebaseTripRef.child("points");
        something.addListenerForSingleValueEvent(new DataPointListener());
        return view;
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
