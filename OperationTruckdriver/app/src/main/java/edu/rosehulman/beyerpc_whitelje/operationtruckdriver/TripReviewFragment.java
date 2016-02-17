package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trip_review, container, false);
        Button btn = (Button) view.findViewById(R.id.cancel_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCancelButtonClicked();
            }
        });

        mList = new ArrayList<>();

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
    }
}
