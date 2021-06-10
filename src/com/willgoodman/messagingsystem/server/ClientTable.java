package com.willgoodman.messagingsystem.server;

// Each nickname has a different incomming-message queue.

import com.willgoodman.messagingsystem.Report;

import java.util.concurrent.*;

public class ClientTable {

  private ConcurrentMap<String,BlockingQueue<Message>> queueTable
    = new ConcurrentHashMap<String,BlockingQueue<Message>>();
  //Error messages:
  private static final String NICKNAME_ALREADY_EXISTS = "User with that nickname already exists.";

  

  public void add(String nickname) {
        if (!queueTable.containsKey(nickname)) {
            queueTable.put(nickname, new LinkedBlockingQueue<Message>());
        } else {
            Report.error(NICKNAME_ALREADY_EXISTS);
        }
    
  }

  public void changeKey(String newKey, String oldKey) {
    queueTable.put(newKey, queueTable.get(oldKey));
    queueTable.remove(oldKey);
  }

  public boolean contains(String key) {
    if (queueTable.containsKey(key)) {
      return true;
    } else {
      return false;
    }
  }

  public void remove(String nickname) {
    queueTable.remove(nickname);  
  }
  
  // Returns null if the nickname is not in the table:
  public BlockingQueue<Message> getQueue(String nickname) {
    return queueTable.get(nickname);
  }
}
