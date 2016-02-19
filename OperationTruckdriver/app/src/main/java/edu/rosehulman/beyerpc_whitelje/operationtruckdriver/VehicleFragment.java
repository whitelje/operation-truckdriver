package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.BreakIterator;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VehicleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VehicleFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class VehicleFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private String mVIN;
    private String mEngineMake;
    private String mEngineModel;
    private double mEngineTemp;
    private double mOdometer;
    private double mEngineHours;
    private double mOilPSI;
    private TextView mEngineTempView;
    private TextView mOdometerView;
    private TextView mOilPressureView;
    private TextView mEngineHoursView;
    private TextView mVinView;
    private TextView mEngineMakeView;
    private TextView mEngineModelView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment VehicleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static VehicleFragment newInstance() {
        VehicleFragment fragment = new VehicleFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    public VehicleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vehicle, container, false);
        Button okay = (Button) view.findViewById(R.id.ok_btn);
        okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed();
            }
        });

        getTextViews(view);

        return view;
    }

    private void getTextViews(View view) {
        mEngineTempView = (TextView) view.findViewById(R.id.vehicleFrag_engine_temp);
        mOdometerView = (TextView) view.findViewById(R.id.vehicleFrag_odometer);
        mOilPressureView = (TextView) view.findViewById(R.id.vehicleFrag_oil_psi);
        mEngineHoursView = (TextView) view.findViewById(R.id.vehicleFrag_engine_hours);
        mVinView = (TextView) view.findViewById(R.id.vehicleFrag_VIN);
        mEngineMakeView = (TextView) view.findViewById(R.id.vehicleFrag_engine_make);
        mEngineModelView = (TextView) view.findViewById(R.id.vehicleFrag_engine_model);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onCloseVehicleFragmentClicked();
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

    public void updateLabel(int pgn, double value) {
        switch(pgn) {
            case 65262:
                // engine temp
                mEngineTemp = value;
                mEngineTempView.setText(value + " F");
                break;
            case 65217:
                // odo
                mOdometer = value;
                mOdometerView.setText(value + " MI");
                break;
            case 65263:
                // oil pressure
                mOilPSI = value;
                mOilPressureView.setText(value + " PSI");
                break;
            case 65253:
                // engine hours
                mEngineHours = value;
                mEngineHoursView.setText(value + "Hours");
                break;
        }

    }

    public void updateLabel(int pgn, String make, String model, String serial) {
        mEngineMake = make;
        mEngineMakeView.setText(make);
        mEngineModel = model;
        mEngineModelView.setText(model);
    }

    public void updateLabel(String vin) {
        // vin
        mVIN = vin;
        mVinView.setText(vin);
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
        void onCloseVehicleFragmentClicked();
    }
}
