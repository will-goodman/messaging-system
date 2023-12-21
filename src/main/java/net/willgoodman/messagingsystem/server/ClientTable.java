package net.willgoodman.messagingsystem.server;// Each nickname has a different incomming-message queue.

import net.willgoodman.messagingsystem.Message;
import net.willgoodman.messagingsystem.Report;

import java.util.concurrent.*;

public class ClientTable {

  private ConcurrentMap<String,BlockingQueue<Message>> queueTable
    = new ConcurrentHashMap<String,BlockingQueue<Message>>();

  // The following overrides any previously existing nickname, and
  // hence the last client to use this nickname will get the messages
  // for that nickname, and the previously exisiting clients with that
  // nickname won't be able to get messages. Obviously, this is not a
  // good design of a messaging system. So I don't get full marks:

  public void add(String nickname) {
        if (!queueTable.containsKey(nickname)) {
            queueTable.put(nickname, new LinkedBlockingQueue<Message>());
        } else {
            Report.error("User with that nickname already exists.");
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

  //MY CODE
  public void remove(String nickname) {
    queueTable.remove(nickname);  
  }
  //END OF MY CODE

  // Returns null if the nickname is not in the table:
  public BlockingQueue<Message> getQueue(String nickname) {
    return queueTable.get(nickname);
  }
}
