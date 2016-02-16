package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jakesorz on 2/15/16.
 */
public class ReviewItem implements Parcelable {
    private final String mName;
    protected final String mDesc;

    protected ReviewItem(Parcel in) {
        mName = in.readString();
        mDesc = in.readString();
    }

    ReviewItem() {
        mName = "yadda";
        mDesc = "yadda";

    }

    public static final Creator<ReviewItem> CREATOR = new Creator<ReviewItem>() {
        @Override
        public ReviewItem createFromParcel(Parcel in) {
            return new ReviewItem(in);
        }

        @Override
        public ReviewItem[] newArray(int size) {
            return new ReviewItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mDesc);

    }
}
