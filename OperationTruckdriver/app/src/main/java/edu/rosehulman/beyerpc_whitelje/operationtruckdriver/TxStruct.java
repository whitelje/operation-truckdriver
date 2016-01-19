package edu.rosehulman.beyerpc_whitelje.operationtruckdriver;

/**
 * Created by jakesorz on 1/18/16.
 */
class TxStruct {
    private byte[] buf;
    private int len;

    public TxStruct() {
        buf = new byte[10];
        len = 0;
    }

    public TxStruct(int bufSize, int len) {
        buf = new byte[bufSize];
        this.len = len;
    }

    public TxStruct(byte[] buf, int len) {
        this.buf = buf;
        this.len = len;
    }

    public void setLen(int length) {
        len = length;
    }

    public int getLen() {
        return len;
    }

    public void setBuf(int pos, byte data) {
        if( pos >= 0 && pos < buf.length ) buf[pos] = data;
    }

    public byte[] getBuf() {
        return buf;
    }
}
