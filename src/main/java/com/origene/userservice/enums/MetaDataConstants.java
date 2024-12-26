package com.origene.userservice.enums;

public enum MetaDataConstants {
  ACTIVE('A'),
  INACTIVE('I'),
  DELETED('D'),
  UNVERIFIED('U');

  private final char value;

  MetaDataConstants(char value) {
    this.value = value;
  }

  public char getValue() {
    return value;
  }
}