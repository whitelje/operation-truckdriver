package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by whitelje on 1/17/16.
 */
public class BluetoothService extends Service {

    private static final String TAG = "TD_BT";
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 1;
    private static final UUID sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final byte RS232_FLAG = (byte) 0xC0;
    private static final byte RS232_ESCAPE = (byte) 0xDB;
    private static final byte RS232_ESCAPE_FLAG = (byte) 0xDC;
    private static final byte RS232_ESCAPE_ESCAPE = (byte) 0xDD;
    private boolean mConnected;
    private boolean mStats;
    private BluetoothSocket mBluetoothSocket;
    private Context mContext;
    private byte[] mBuffer;
    private int mCount;
    private int mSize;
    private boolean mIsStuffed;
    private boolean mIsInvalid;

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == Activity.RESULT_CANCELED) {
            disconnect();
        }
        switch (requestCode)
        {
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a bluetoothDevice to connect
                if (resultCode == Activity.RESULT_OK)
                {
                    final String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Thread connectThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //disconnect();
                            connectDevice(address, false);
                        }
                    });
                    connectThread.start();
                }
                break;
        }
    }

    private Runnable readRun = new Runnable()
    {
        public void run()
        {
            receiveDataFromBT(mBluetoothSocket);
        }
    };
    private Thread readThread;

    private BluetoothSocket connectDevice(String address, boolean backup)
    {
        // Get the BluetoothDevice object
        mConnected = false;
        mStats = false;
        BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bluetoothDevice;
        if(bAdapter != null) {
            bluetoothDevice = bAdapter.getRemoteDevice(address);
        } else {
            // replace with intent or Snackbar?
            //Toast.makeText(getApplicationContext(), "This device does not have Bluetooth.",
            //        Toast.LENGTH_SHORT).show();
            return null;
        }
        if(backup) {
            try {
                Log.d(TAG,"Reconnect with invoke");
                mBluetoothSocket = invokeConnect(bluetoothDevice);
                mConnected = true;
            } catch (IOException ioe) {
                Log.e(TAG,"",ioe);
            } catch (Exception e) {
                Log.e(TAG,"this was a bad idea",e);
            }
        } else {
            try {
                mBluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(sppUUID);
                mBluetoothSocket.connect();
                mConnected = true;
            } catch (IOException ioex) {
                Log.e(TAG, "Insecure failed, trying reflection");
                try {
                    if (mBluetoothSocket != null) {
                        mBluetoothSocket.close();
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "", ioe);
                }

                try {
                    mBluetoothSocket = invokeConnect(bluetoothDevice);
                    mConnected = true;
                } catch (IOException ioe) {
                    Log.e(TAG, "invoke", ioe);
                } catch (Exception e) {
                    Log.e(TAG, "this was a bad idea", e);
                }
            }
        }
        if (mConnected) {

            //Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();

            mBuffer = new byte[4096];
            mCount = 0;
            if(readThread != null && readThread.isAlive()) {
                readThread.interrupt();
                while(readThread.isAlive()) Thread.yield();
            } else {
                readThread = new Thread(readRun);
                readThread.setPriority(4);
                readThread.start();
            }

        } else {

                    //Toast.makeText(getApplicationContext(), "Bluetooth connection error, try again",
                    //        Toast.LENGTH_SHORT).show();
                    disconnect();

        }

        return (mBluetoothSocket);
    }

    private BluetoothSocket invokeConnect(BluetoothDevice bluetoothDevice)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, IOException {
        Method m = bluetoothDevice.getClass().getMethod("createRfcommSocket", int.class);
        BluetoothSocket bs = (BluetoothSocket) m.invoke(bluetoothDevice, 1);
        bs.connect();
        return bs;
    }

    private void disconnect() {
        mConnected = false;

        try
        {
            if(readThread != null) readThread.interrupt();
            if (mBluetoothSocket != null)
            {
                mBluetoothSocket.close();
                mBluetoothSocket = null;
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "", e);
        }
    }

    private void reconnect() {
        Log.d(TAG, "In reconnect");
        if(mBluetoothSocket != null) {
            final String address = mBluetoothSocket.getRemoteDevice().getAddress();
            mConnected = false;
            try {
                mBluetoothSocket.close();
            } catch (IOException e) { /*That's really too bad dave. */ }
            Thread connectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    connectDevice(address, !mStats);
                }
            });
            connectThread.start();
        } else {
                    //Toast.makeText(getApplicationContext(),
                    //        "Bluetooth connection lost. Please reconnect.",
                    //        Toast.LENGTH_LONG).show();

        }
    }

    private void receiveDataFromBT(BluetoothSocket socket) {
        try {
            byte[] buffer = new byte[1024];
            int buf_len;


            if (socket == null) {
                return;
            }

            InputStream inputStream = socket.getInputStream();

            while (true) {
                try {
                    if(Thread.interrupted() && !mConnected) {
                        inputStream.close();
                        inputStream = null;
                        return;
                    }
                    // Read from the InputStream
                    buf_len = inputStream.read(buffer);
                    Thread.sleep(1);
                    if (buf_len == -1) {
                        inputStream.close();
                        break;
                    }
                    parseMessage(buffer, buf_len);

                } catch (IOException e) {

                    if(!mConnected && Thread.interrupted()) {
                        inputStream.close();
                        inputStream = null;
                        return;
                    }
                    inputStream.close();
                    inputStream = null;
                    reconnect();
                    return;
                } catch (InterruptedException e) {
                    if( !mConnected) {
                        inputStream.close();
                        inputStream = null;
                        Log.e(TAG, "Interrupted read", e);
                        return;
                    }
                }
            }

        } catch (IOException e) {
            Log.e(TAG,"", e);
        }
    }

    public void parseMessage(byte[] buf, int len) {
        for (int i = 0; i < len; i++) {
            processCharFromBus(buf[i]);
        }
    }

    private void processCharFromBus(byte val) {
        try {
            //Is it the start of the message?
            if (val == RS232_FLAG) {
                mIsInvalid = false;
                mIsStuffed = false;
                mSize = -1;
                mCount = 0;
            } else if (!mIsInvalid) {
                if (val == RS232_ESCAPE) {
                    mIsStuffed = true;
                } else {
                    //If previous byte was an escape, then decode current byte
                    if (mIsStuffed) {
                        mIsStuffed = false;
                        if (val == RS232_ESCAPE_FLAG) {
                            val = RS232_FLAG;
                        } else if (val == RS232_ESCAPE_ESCAPE) {
                            val = RS232_ESCAPE;
                        } else {
                            mIsInvalid = true;
                            // Invalid byte after escape, must abort
                            return;
                        }
                    }
                    //At this point data is always unstuffed
                    if (mCount < mBuffer.length) {
                        mBuffer[mCount] = val;
                        mCount++;
                    } else {
                        //Full buffer
                    }

                    //At 2 bytes, we have enough info to calculate a real message length
                    if (mCount == 2) {
                        mSize = ((mBuffer[0] << 8) | mBuffer[1]) + 2;
                    }

                    //Have we received the entire message? If so, is it valid?
                    if (mCount == mSize && val == cksum(mBuffer, mCount -1)) {
                        mCount--; //Ignore the checksum at the end of the message
                        //processPacket(mBuffer);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "processChar", e);
        }
    }

    private void sendCommand(TxStruct command)
    {
        if (mBluetoothSocket != null)
        {
            try
            {
                mBluetoothSocket.getOutputStream().write(command.getBuf(),0,command.getLen());
            }
            catch (IOException e)
            {
                Log.e(TAG, "Send Command Socket Closed", e);
            }
        }
        else
        {
            disconnect();
        }
    }

    private int cksum(byte[] commandBytes)
    {
        int count = 0;

        for (int i = 1; i < commandBytes.length; i++)
        {
            count += uByte(commandBytes[i]);
        }

        return (byte) (~(count & 0xFF) + (byte) 1);
    }

    private int cksum(byte[] data, int numbytes) {
        int count = 0;

        for (int i = 0; i < numbytes; i++) {
            count += uByte(data[i]);
        }
        return (byte) (~(count & 0xFF) + (byte) 1);
    }

    private int uByte(byte b)
    {
        return (int)b & 0xFF;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
