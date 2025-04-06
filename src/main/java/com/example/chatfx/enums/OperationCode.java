package com.example.chatfx.enums;

public enum OperationCode {
    MESSAGE(0),
    REGISTRATION(1),
    LOGIN(2),
    RECONNECT(3),
    ACCESS_GRANTED(4),
    ACCESS_DENIED(5),
    USERS_LIST(6),
    IMAGE(7);

    private final int code;

    OperationCode(int code) {
        this.code = code;
    }

    public int value() {
        return code;
    }

    public String stringValue() {
        return String.valueOf(code);
    }

    public static OperationCode fromValue(int value) {
        for (OperationCode opCode : OperationCode.values()) {
            if (opCode.code == value)
                return opCode;
        }
        return null;
    }

    public static OperationCode fromValue(String value) {
        return fromValue(Integer.parseInt(value));
    }
}