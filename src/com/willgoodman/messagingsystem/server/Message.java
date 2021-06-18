package com.willgoodman.messagingsystem.server;

import java.sql.Timestamp;
import java.util.Date;

public class Message {

  private final Timestamp TIMESTAMP;
  private final String AUTHOR;
  private final String TEXT;

  Message(String sender, String text) {
    this.TIMESTAMP = new Timestamp(new Date().getTime());
    this.AUTHOR = sender;
    this.TEXT = text;
  }

  public String toString() {
    return "From " + this.AUTHOR + " at " + this.TIMESTAMP.toString() + ": " + this.TEXT;
  }
}
