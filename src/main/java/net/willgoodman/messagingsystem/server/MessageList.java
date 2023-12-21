package net.willgoodman.messagingsystem.server;

import net.willgoodman.messagingsystem.Message;
import net.willgoodman.messagingsystem.Report;

import java.util.ArrayList;
import java.util.concurrent.*;
public class MessageList {
  private ConcurrentMap<String,ArrayList<Message>> messageQueue = new ConcurrentHashMap<String,ArrayList<Message>>();
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

  /*private ConcurrentHashMap userQueue(String username) {
    if (messageQueue.containsKey(username)) {
      return messageQueue.get(username);
    } else {
      net.willgoodman.messagingsystem.Report.error("No user exists by that name");
      return null;
    }
  }*/
}
