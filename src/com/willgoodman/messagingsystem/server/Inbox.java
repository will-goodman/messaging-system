package com.willgoodman.messagingsystem.server;

import java.util.LinkedList;

public class Inbox {

    private LinkedList<Message> messages = new LinkedList<>();
    private int currentMessage = 0;

    public Message getCurrentMessage() {
        try {
            return this.messages.get(this.currentMessage);
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    public Message getPreviousMessage() {
        if (this.currentMessage > 0) {
            this.currentMessage -= 1;
            return this.getCurrentMessage();
        } else {
            return null;
        }
    }

    public Message getNextMessage() {
        if (this.currentMessage < this.messages.size() - 1) {
            this.currentMessage += 1;
            return this.getCurrentMessage();
        } else {
            return null;
        }
    }

    public void addMessage(Message message) {
        this.messages.add(message);
    }

    public void deleteMessage() {
        this.messages.remove(this.currentMessage);
        if (this.currentMessage > this.messages.size() - 1 && this.currentMessage != 0) {
            this.currentMessage = this.messages.size() - 1;
        }
    }

}
