package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by jakesorz on 2/1/16.
 */
public class VnaMessageHandler {
    private static final byte RS232_FLAG = (byte) 0xC0;
    private static final byte RS232_ESCAPE = (byte) 0xDB;
    private static final byte RS232_ESCAPE_FLAG = (byte) 0xDC;
    private static final byte RS232_ESCAPE_ESCAPE = (byte) 0xDD;
    private static final String TAG = "TD_VMH";

    private static final int VNA_MSG_ACK = 0;      /* ack */
    private static final int VNA_MSG_FA_J1939 = 1;      /* pgn filter add */
    private static final int VNA_MSG_FD_J1939 = 2;      /* pgn filter delete */
    private static final int VNA_MSG_FA_J1587 = 3;      /* pid filter add */
    private static final int VNA_MSG_FD_J1587 = 4;      /* pid filter delete */
    private static final int VNA_MSG_TX_J1939 = 5;      /* pgn tx */
    private static final int VNA_MSG_RX_J1939 = 6;      /* pgn rx */
    private static final int VNA_MSG_PX_J1939 = 7;      /* pgn tx - periodic */
    private static final int VNA_MSG_TX_J1587 = 8;      /* pid tx */
    private static final int VNA_MSG_RX_J1587 = 9;      /* pid rx */
    private static final int VNA_MSG_PX_J1587 = 10;      /* pid tx - periodic */
    private static final int VNA_MSG_RTC_SET = 11;      /* real-time clock */
    private static final int VNA_MSG_LPW_SET = 13;      /* low pwr mode set */
    private static final int VNA_MSG_CAN_SET = 15;      /* can bits per second */
    private static final int VNA_MSG_UART_SET = 16;      /* uart bits per second */
    private static final int VNA_MSG_CPU_RESET = 17;      /* cpu reset */
    private static final int VNA_MSG_PAMODE_SET = 18;      /* passall mode config */
    private static final int VNA_MSG_TX_CAN = 20;      /* can rx */
    private static final int VNA_MSG_RX_CAN = 21;      /* can rx */
    private static final int VNA_MSG_RX_J1708 = 22;      /* j1708 rx */
    private static final int VNA_MSG_STATS = 23;      /* stat msg - 1 sec */
    private static final int VNA_MSG_ACONN = 25;      /* obd2 auto connect - baud and CAN id */
    private static final int VNA_MSG_FA_CAN_MSK = 26;      /* can id/mask filter add */
    private static final int VNA_MSG_FD_CAN_MSK = 27;      /* can id/mask filter delete */
    private static final int VNA_MSG_PSTATUS = 30;      /* patch status */
    private static final int VNA_MSG_BLVER = 31;      /* bl version */
    private static final int VNA_MSG_FA_I15765 = 40;      /* pid filter add */
    private static final int VNA_MSG_FD_I15765 = 41;      /* pid filter delete */
    private static final int VNA_MSG_TX_I15765 = 42;      /* pid tx */
    private static final int VNA_MSG_RX_I15765 = 43;      /* pid rx */
    private static final int VNA_MSG_PX_I15765 = 44;      /* pid tx - periodic */
    private static final int VNA_MSG_STATS_OBD = 45;      /* stat msg - 1 sec */
    private static final int VNA_MSG_PAGEN = 254;      /* next page of vmsg */
    private static final int VNA_MSG_REQ = 255;      /* request vna_msg */

    private static final String DEGREE = " \u00b0";
    private static final String CELSIUS = "C";
    private static final String FAHRENHEIT = "F";
    private static final double KM_TO_MI = 0.621371;
    private static final double L_TO_GAL = 0.264172;
    private static final double GAL_TO_L = 3.78541;
    private static final double KPA_TO_PSI = 0.145037738;
    private static final double PSI_TO_KPA = 6.89475729;
    private static final double KW_TO_HP = 1.34102209;
    private static final double MI_TO_KM = 1.60934;
    private static final Integer MAX_16 = 0xffff;
    private static final Integer MAX_32 = 0xffffffff;
    private static final Integer MAX_8 = 0xff;

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
                    if (mCount == mSize && val == cksum(mBuffer, mCount - 1)) {
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
                final Integer pgn = ((mBuffer[4] & 0xFF) << 16) | ((mBuffer[5] & 0xFF) << 8) | (mBuffer[6] & 0xFF);
                Integer astxCnt;
                Double d;
                Integer i;
                StringBuilder sb;
                String[] fields;
                String out;
                Integer length;
                String spnDescription;
                String fmiDescription;
                Message msg = mHandler.obtainMessage(Constants.MESSAGE_RX_J1939);
                Bundle bundle = new Bundle();
                double value = 0;
                switch (pgn) {
                    case 61444:
                        i = ((mBuffer[14] & 0xFF) << 8) | (mBuffer[13] & 0xFF);
                        if (i.equals(MAX_16)) break;
                        //newData.put("RPM", (i * 0.125 + "")); /* SPN 190 */
                        value = i * 0.125;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65214:
                        i = ((mBuffer[11] & 0xFF) << 8) | ((mBuffer[10]) & 0xFF);
                        if (i.equals(MAX_16)) break;
                        d = i * 0.5;
                        value = d * KW_TO_HP; /* SPN 166 */
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65244:
                        i = (((mBuffer[17] & 0xFF) << 24) | ((mBuffer[16] & 0xFF) << 16) | ((mBuffer[15] & 0xFF) << 8) | ((mBuffer[14] & 0xFF)));
                        if (i.equals(MAX_32)) break;
                        value = i * 0.05; /* SPN 235 */
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65209:
                        i = (((mBuffer[25] & 0xFF) << 24) | ((mBuffer[24] & 0xFF) << 16) | ((mBuffer[23] & 0xFF) << 8) | ((mBuffer[22] & 0xFF)));
                        if (i.equals(MAX_32)) break;
                        value = i * 0.5 * L_TO_GAL; /* SPN 1004 */
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65217:
                        i = (((mBuffer[13] & 0xFF) << 24) | ((mBuffer[12] & 0xFF) << 16) | ((mBuffer[11] & 0xFF) << 8) | ((mBuffer[10] & 0xFF)));
                        if (i.equals(MAX_32)) break;
                        value = i * 0.005 * KM_TO_MI; /* SPN 917 */
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);

                        i = (((mBuffer[17] & 0xFF) << 24) | ((mBuffer[16] & 0xFF) << 16) | ((mBuffer[15] & 0xFF) << 8) | ((mBuffer[14] & 0xFF)));
                        if (i.equals(MAX_32)) break;
                        value = i * 0.005 * KM_TO_MI; /* SPN 918 */
                        msg = mHandler.obtainMessage(Constants.MESSAGE_RX_J1939);
                        bundle = new Bundle();
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65226:
                        //packet[10] bits 1-2: protect lamp, bits 3-4: amber lamp, bits 5-6: red lamp, bits 7-8: MIL
                        //packet[11] reserved lamp
                        length = (mBuffer[1] - 11) / 4;
                        for (i = 0; i < length; i++) {
                            Integer weird = (mBuffer[14 + i * 4] & 0xff);
                            Integer spn = (mBuffer[12 + i * 4] & 0xff);
                            spn |= ((mBuffer[13 + i * 4] & 0xff) << 8);
                            spn |= ((weird & 0b1110_0000) << 11);
                            Integer fmi = weird & 0b00011111;
                            Integer oc = (mBuffer[15 + i * 4] & 0xff) & 0x7f;
                            msg = mHandler.obtainMessage(Constants.MESSAGE_RX_J1939);
                            bundle = new Bundle();
                            bundle.putInt(Constants.J1939_SPN, spn);
                            bundle.putInt(Constants.J1939_FMI, fmi);
                            bundle.putInt(Constants.J1939_OC, oc);
                            bundle.putBoolean(Constants.J1939_ACTIVE, true);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                        break;

                    case 65227:
                        length = (mBuffer[1] - 11) / 4;
                        for (i = 0; i < length; i++) {
                            Integer weird = (mBuffer[14 + i * 4] & 0xff);
                            Integer spn = (mBuffer[12 + i * 4] & 0xff);
                            spn |= ((mBuffer[13 + i * 4] & 0xff) << 8);
                            spn |= ((weird & 0b1110_0000) << 11);
                            Integer fmi = weird & 0b00011111;
                            Integer oc = (mBuffer[15 + i * 4] & 0xff) & 0x7f;

                            msg = Message.obtain();
                            bundle = new Bundle();
                            bundle.putInt(Constants.J1939_SPN, spn);
                            bundle.putInt(Constants.J1939_FMI, fmi);
                            bundle.putInt(Constants.J1939_OC, oc);
                            bundle.putBoolean(Constants.J1939_ACTIVE, false);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                        break;

                    case 65253:
                        i = (((mBuffer[13] & 0xFF) << 24) | ((mBuffer[12] & 0xFF) << 16) | ((mBuffer[11] & 0xFF) << 8) | ((mBuffer[10] & 0xFF)));
                        if (i.equals(MAX_32)) break;
                        value = i * 0.05;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 247 */
                        break;

                    case 65255:
                        i = (((mBuffer[17] & 0xFF) << 24) | ((mBuffer[16] & 0xFF) << 16) | ((mBuffer[15] & 0xFF) << 8) | ((mBuffer[14] & 0xFF)));
                        if (i.equals(MAX_32)) break;
                        value = i * 0.05;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 248 */
                        break;

                    case 65257:
                        i = (((mBuffer[13] & 0xFF) << 24) | ((mBuffer[12] & 0xFF) << 16) | ((mBuffer[11] & 0xFF) << 8) | ((mBuffer[10] & 0xFF)));
                        if (i.equals(MAX_32)) break;
                        value = i * 0.5 * L_TO_GAL;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65259:
                        if (mBuffer[8] == 0) {
                            sb = new StringBuilder();
                            astxCnt = 0;
                            for (int loop = 10; loop < mBuffer.length; loop++) {
                                sb.append((char) mBuffer[loop]);
                                if (mBuffer[loop] == '*') {
                                    astxCnt++;
                                    if (astxCnt == 4) break;
                                }
                            }
                            fields = sb.toString().split("\\*");
                            bundle.putInt(Constants.J1939_PGN, pgn);
                            bundle.putString(Constants.J1939_MAKE, fields[0]); /* SPN 586 */
                            bundle.putString(Constants.J1939_MODEL, fields[1]); /* SPN 587 */
                            bundle.putString(Constants.J1939_SERIAL, fields[2]); /* SPN 588 */
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                        break;

                    case 65260:
                        sb = new StringBuilder();
                        for (int loop = 10; loop < mBuffer.length; loop++) {
                            sb.append((char) mBuffer[loop]);
                            if (mBuffer[loop] == '*') {
                                break;
                            }
                        }
                        fields = sb.toString().split("\\*");
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putString(Constants.J1939_VIN, fields[0]);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65261:
                        i = mBuffer[10] & 0xff;
                        if (i.equals(MAX_8)) break;
                        value = i * KM_TO_MI;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65262:
                        i = (mBuffer[10] & 0xFF);
                        if (i.equals(MAX_8)) break;
                        value = (i - 40) * 9 / 5.0 + 32;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 110 */
                        break;

                    case 65263:
                        i = (mBuffer[13] & 0xFF);
                        if (i.equals(MAX_8)) break;
                        value = i * 4 * KPA_TO_PSI;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 100 */
                        break;

                    case 65265:
                        i = (mBuffer[15] & 0xFF);
                        if (i.equals(MAX_8)) break;
                        value = i * KM_TO_MI;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 86 */
                        break;

                    case 65266:
                        i = (((mBuffer[11] & 0xFF) << 8) | ((mBuffer[10]) & 0xFF));
                        if (i.equals(MAX_16)) break;
                        value = i * 0.05 * L_TO_GAL;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 183 */
                        i = (((mBuffer[15] & 0xFF) << 8) | ((mBuffer[14]) & 0xFF));
                        if (i.equals(MAX_16)) break;
                        msg = mHandler.obtainMessage(Constants.MESSAGE_RX_J1939);
                        bundle = new Bundle();
                        d = i / 512.0;
                        value = d * 2.35215;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);
                        break;

                    case 65270:
                        i = (mBuffer[11] & 0xFF);
                        if (i.equals(MAX_8)) break;
                        value = i * 2 * KPA_TO_PSI;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 102 */
                        i = (mBuffer[12] & 0xFF);
                        if (i.equals(MAX_8)) break;

                        msg = mHandler.obtainMessage(Constants.MESSAGE_RX_J1939);
                        bundle = new Bundle();
                        value = (i - 40) * 9 / 5.0 + 32;
                        bundle.putInt(Constants.J1939_PGN, pgn);
                        bundle.putDouble(Constants.J1939_VALUE, value);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg); /* SPN 105 */
                        break;
                }
                break;
            case VNA_MSG_RX_I15765: {
                // if using OBD send message
                // should not receive if not using
                msg = mHandler.obtainMessage(Constants.MESSAGE_RX_OBD);
                bundle = new Bundle();
                int pid = mBuffer[9];
                bundle.putInt(Constants.OBD_PID, pid);
                value = ((uByte(mBuffer[10]) * 256) + uByte(mBuffer[11])) / 4;
                bundle.putDouble(Constants.OBD_DATA, value);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                break;
            }
        }

    }

    private int cksum(byte[] commandBytes) {
        int count = 0;

        for (int i = 1; i < commandBytes.length; i++) {
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

    private int uByte(byte b) {
        return (int) b & 0xFF;
    }
}
