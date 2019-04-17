package me.j360.raft.counter.server;

/**
 * @author: min_xu
 */
public enum CommandType {
    SET, CAS, INC, GET;

    public byte toByte() {
        switch (this) {
            case SET:
                return (byte) 0;
            case CAS:
                return (byte) 1;
            case INC:
                return (byte) 2;
            case GET:
                return (byte) 3;
        }
        throw new IllegalArgumentException();
    }

    public static CommandType parseByte(byte b) {
        switch (b) {
            case 0:
                return SET;
            case 1:
                return CAS;
            case 2:
                return INC;
            case 3:
                return GET;
        }
        throw new IllegalArgumentException();
    }
}
