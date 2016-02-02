package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jakesorz on 2/1/16.
 */
public class VnaMessageHandler {
    private static final byte RS232_FLAG = (byte) 0xC0;
    private static final byte RS232_ESCAPE = (byte) 0xDB;
    private static final byte RS232_ESCAPE_FLAG = (byte) 0xDC;
    private static final byte RS232_ESCAPE_ESCAPE = (byte) 0xDD;
    private static final String TAG = "TD_VMH";

    private byte[] mBuffer;
    private int mCount;
    private int mSize;
    private boolean mIsStuffed;
    private boolean mIsInvalid;

        private final Handler mHandler;

        VnaMessageHandler(Handler handler) {
            mHandler = handler;
            mBuffer = new byte[Constants.MAX_MESSAGE_SIZE];
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
                        processPacket(mBuffer);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "processChar", e);
        }
    }

    private void processPacket(byte[] mBuffer) {
        int msgId = mBuffer[2];

        // need to get msgIds from adapter source

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
}
