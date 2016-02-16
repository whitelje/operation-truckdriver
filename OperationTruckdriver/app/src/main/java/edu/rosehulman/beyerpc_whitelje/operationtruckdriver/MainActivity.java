package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
    implements TripReviewFragment.OnFragmentInteractionListener,
                VehicleFragment.OnFragmentInteractionListener,
        ReviewFragment.OnListFragmentInteractionListener

{
    BluetoothService mBluetoothService;
    private String mConnectedDeviceName;
    private VnaMessageHandler mVnaMessageHandler;
    private boolean blah = false;
    private boolean logged_in = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            if (mBluetoothService != null) {
                mBluetoothService.start();
            }
        }
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a bluetoothDevice to connect
                if (resultCode == Activity.RESULT_OK) {
                    final String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    mBluetoothService.connectDevice(address);
                }
                break;
            case LoginActivity.REQUEST_LOGIN:
                logged_in = true;
//                if(mBluetoothService != null &&
//                        mBluetoothService.getState() == BluetoothService.STATE_NONE) {
//                    Intent intent = new Intent(this, DeviceListActivity.class);
//                    startActivityForResult(intent, REQUEST_CONNECT_DEVICE_INSECURE);
//                }
                break;
        }
    }


    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVnaMessageHandler = new VnaMessageHandler(mHandler);
        mBluetoothService = new BluetoothService(this, mHandler);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        if (!logged_in) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LoginActivity.REQUEST_LOGIN);
        } else {
//            Intent intent = new Intent(this, DeviceListActivity.class);
//            startActivityForResult(intent, REQUEST_CONNECT_DEVICE_INSECURE);
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, new MainActivityFragment());
        ft.commit();
//        ft.replace(R.id.container, new TripFragment());
//        ft.commit();
    }

    private void setStatus(int resId) {
        AppCompatActivity activity = this;

        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {
        AppCompatActivity activity = this;
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }


    private final Handler mHandler = new MyHandler(this);

    public void start_listener(View view) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, TripFragment.newInstance());
        ft.addToBackStack(null);
        ft.commit();
    }

    public void review_listener(View view) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, ReviewFragment.newInstance(1));
        ft.addToBackStack(null);
        ft.commit();
    }

    public void vehicle_listener(View view) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, VehicleFragment.newInstance());
        ft.addToBackStack("main");
        ft.commit();
    }

    // TripReviewFragment.OnInteractionListener
    @Override
    public void onCancelButtonClicked() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container, ReviewFragment.newInstance(1));
        ft.commit();
        fm.popBackStack();
    }

    @Override
    public void onListFragmentInteraction(ReviewItem item) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, TripReviewFragment.newInstance(item));
        ft.addToBackStack("review");
        ft.commit();

    }

    @Override
    public void onCloseVehicleFragmentClicked() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, MainActivityFragment.newInstance());
        ft.commit();
    }

    class MyHandler extends Handler {

        private final Activity mActivity;

        MyHandler(Activity activity) {
            mActivity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    mVnaMessageHandler.parseMessage(readBuf, msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != mActivity) {
                        Toast.makeText(mActivity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != mActivity) {
                        Toast.makeText(mActivity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_STATS_OBD:
                    if(!blah) {
                        byte[] buf = new byte[7];
                        buf[0] = (byte) 0xC0;
                        buf[1] = (byte) 0;
                        buf[2] = 4;
                        buf[3] = 0x40;
                        buf[4] = (byte) 0;
                        buf[5] = 0x0C;
                        buf[6] = 0x74;
                        mBluetoothService.write(buf);
                        blah = true;
                    }
                    break;
                case Constants.MESSAGE_RX_OBD:
                    // write to firebase
                    // write to trip view
                    break;
            }
        }
    }

}
