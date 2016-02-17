package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import edu.rosehulman.beyerpc_whitelje.operationtruckdriver.ReviewFragment.OnListFragmentInteractionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
        df = DateFormat.getMediumDateFormat((MainActivity) listener);
        mFirebaseUsersRef = new Firebase(Constants.FIREBASE_URL + Constants.FIREBASE_USERS + "/" + mUid);
        mFirebaseUsersRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                add(dataSnapshot);

            }

            private void add(DataSnapshot dataSnapshot) {
                Trip trip = dataSnapshot.getValue(Trip.class);
                trip.setKey(dataSnapshot.getKey());
                mValues.add(trip);
                Collections.sort(mValues);
                notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

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
