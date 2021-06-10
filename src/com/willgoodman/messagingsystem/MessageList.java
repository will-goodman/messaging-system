package com.willgoodman.messagingsystem;

import java.util.ArrayList;
import java.util.concurrent.*;
public class MessageList {
  private ConcurrentMap<String,ArrayList<Message>> messageQueue = new ConcurrentHashMap<String,ArrayList<Message>>();
  //Error messages:
  private static final String NOT_FOUND = " not found";
  private static final String ALREADY_EXISTS = " already exists";

  
  public ArrayList<Message> get(String username) {
    if (messageQueue.containsKey(username)) {
      return messageQueue.get(username);
    } else {
      System.out.println(username + NOT_FOUND);
      return null;
    }
  }
  
  public void add(String username) {
    if (!messageQueue.containsKey(username)) {
      messageQueue.put(username, new ArrayList<Message>());
    } else {
      Report.error(username + ALREADY_EXISTS);
    }
  }

  public void remove(String username, int index) {
    if (messageQueue.containsKey(username)) {
      messageQueue.get(username).remove(index);
    } else {
      System.out.println(username + NOT_FOUND);
    }
  }

  public boolean contains(String username) {
    if (messageQueue.containsKey(username)) {
      return true;
    } else {
      return false;
    }
  }
}
