package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import edu.rosehulman.beyerpc_whitelje.operationtruckdriver.ReviewFragment.OnListFragmentInteractionListener;

/**
 * {@link RecyclerView.Adapter} that can display a {@link } and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyReviewItemRecyclerViewAdapter extends RecyclerView.Adapter<MyReviewItemRecyclerViewAdapter.ViewHolder> {

    private final List<Trip> mValues;
    private final OnListFragmentInteractionListener mListener;
    private final Firebase mFirebaseUsersRef;
    private final String mUid;
    java.text.DateFormat df;

    public MyReviewItemRecyclerViewAdapter(OnListFragmentInteractionListener listener) {
        //mValues = items;
        mValues = new ArrayList<>();
        mListener = listener;
        mUid = SharedPreferencesUtils.getCurrentUser((MainActivity) listener);
        df = java.text.DateFormat.getDateTimeInstance();
        mFirebaseUsersRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_USERS + "/" + mUid);
        Firebase trips = mFirebaseUsersRef.child("trips");
        trips.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Firebase trip = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_TRIPS + "/" + ds.getKey());
                    trip.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Trip tripItem = dataSnapshot.getValue(Trip.class);
                            tripItem.setKey(dataSnapshot.getKey());
                            mValues.add(tripItem);
                            Collections.sort(mValues);
                            notifyDataSetChanged();
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
        });

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_reviewitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        Date date = new Date(holder.mItem.getDate());
        holder.mContentView.setText(df.format(date));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem.getKey());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public Trip mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.id);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
