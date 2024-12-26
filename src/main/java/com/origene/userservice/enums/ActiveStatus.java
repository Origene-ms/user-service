package com.origene.userservice.enums;

public enum ActiveStatus {
    ACTIVE('A'),
    INACTIVE('I'),
    DELETED('D'),
    UNVERIFIED('U');

    private final char value;

    ActiveStatus(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }
}
