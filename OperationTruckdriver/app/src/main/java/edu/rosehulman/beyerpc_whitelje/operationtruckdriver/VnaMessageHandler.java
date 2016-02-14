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

    private static final int VNA_MSG_ACK =          0;      /* ack */
    private static final int VNA_MSG_FA_J1939 =     1;      /* pgn filter add */
    private static final int VNA_MSG_FD_J1939 =     2;      /* pgn filter delete */
    private static final int VNA_MSG_FA_J1587 =     3;      /* pid filter add */
    private static final int VNA_MSG_FD_J1587 =     4;      /* pid filter delete */
    private static final int VNA_MSG_TX_J1939 =     5;      /* pgn tx */
    private static final int VNA_MSG_RX_J1939 =     6;      /* pgn rx */
    private static final int VNA_MSG_PX_J1939 =     7;      /* pgn tx - periodic */
    private static final int VNA_MSG_TX_J1587 =     8;      /* pid tx */
    private static final int VNA_MSG_RX_J1587 =     9;      /* pid rx */
    private static final int VNA_MSG_PX_J1587 =    10;      /* pid tx - periodic */
    private static final int VNA_MSG_RTC_SET =     11;      /* real-time clock */
    private static final int VNA_MSG_LPW_SET =     13;      /* low pwr mode set */
    private static final int VNA_MSG_CAN_SET =     15;      /* can bits per second */
    private static final int VNA_MSG_UART_SET =    16;      /* uart bits per second */
    private static final int VNA_MSG_CPU_RESET =   17;      /* cpu reset */
    private static final int VNA_MSG_PAMODE_SET =  18;      /* passall mode config */
    private static final int VNA_MSG_TX_CAN =      20;      /* can rx */
    private static final int VNA_MSG_RX_CAN =      21;      /* can rx */
    private static final int VNA_MSG_RX_J1708 =    22;      /* j1708 rx */
    private static final int VNA_MSG_STATS =       23;      /* stat msg - 1 sec */
    private static final int VNA_MSG_ACONN =       25;      /* obd2 auto connect - baud and CAN id */
    private static final int VNA_MSG_FA_CAN_MSK =  26;      /* can id/mask filter add */
    private static final int VNA_MSG_FD_CAN_MSK =  27;      /* can id/mask filter delete */
    private static final int VNA_MSG_PSTATUS =     30;      /* patch status */
    private static final int VNA_MSG_BLVER =       31;      /* bl version */
    private static final int VNA_MSG_FA_I15765 =   40;      /* pid filter add */
    private static final int VNA_MSG_FD_I15765 =   41;      /* pid filter delete */
    private static final int VNA_MSG_TX_I15765 =   42;      /* pid tx */
    private static final int VNA_MSG_RX_I15765 =   43;      /* pid rx */
    private static final int VNA_MSG_PX_I15765 =   44;      /* pid tx - periodic */
    private static final int VNA_MSG_STATS_OBD =   45;      /* stat msg - 1 sec */
    private static final int VNA_MSG_PAGEN =      254;      /* next page of vmsg */
    private static final int VNA_MSG_REQ =        255;      /* request vna_msg */


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
        switch (msgId) {
            case VNA_MSG_STATS:
                // using a J1939 VNA
                // if not initialized send filters for 1939 PGNs
                // otherwise update pkt count
                break;
            case VNA_MSG_STATS_OBD: {
                // using a DashLink
                // if not initialized send filters for OBD PIDs
                // otherwise update pkt count
                Message msg = mHandler.obtainMessage(Constants.MESSAGE_STATS_OBD);
                Bundle bundle = new Bundle();
                long value = (mBuffer[3] << 24) | (mBuffer[4] << 16) | (mBuffer[5] << 8) | mBuffer[6];
                bundle.putLong(Constants.STATS_COUNT, value);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                break;
            }

            case VNA_MSG_RX_J1939:
                // if using J1939 send message
                // should not receive message if not using
                break;
            case VNA_MSG_RX_I15765: {
                // if using OBD send message
                // should not receive if not using
                Message msg = mHandler.obtainMessage(Constants.MESSAGE_RX_OBD);
                Bundle bundle = new Bundle();
                int pid = mBuffer[9];
                bundle.putInt(Constants.OBD_PID, pid);
                int value = ((uByte(mBuffer[10])*256)+uByte(mBuffer[11]))/4;
                bundle.putFloat(Constants.OBD_DATA, value);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                break;
            }
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
}
