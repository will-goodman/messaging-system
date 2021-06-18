package com.willgoodman.messagingsystem.server;

import java.util.LinkedList;

public class Inbox {

    private LinkedList<Message> messages = new LinkedList<>();
    private int currentMessage = 0;

    public Message getCurrentMessage() {
        try {
            return this.messages.get(this.currentMessage);
        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            return null;
        }
    }

    public void moveBackwards() {
        if (this.currentMessage > 0) {
            this.currentMessage -= 1;
        }
    }

    public void moveForwards() {
        if (this.currentMessage < this.messages.size() - 1) {
            this.currentMessage += 1;
        }
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        this.currentMessage = this.messages.size() - 1;
    }

    public void deleteMessage() {
        if (this.messages.size() > 0) {
            this.messages.remove(this.currentMessage);
            if (this.currentMessage > this.messages.size() - 1 && this.currentMessage != 0) {
                this.currentMessage = this.messages.size() - 1;
            }
        }
    }

}
