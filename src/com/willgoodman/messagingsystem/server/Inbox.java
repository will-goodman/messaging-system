package com.willgoodman.messagingsystem.server;

import java.util.LinkedList;

/**
 * Inbox which contains all messages for a given user and tracks the current message
 */
public class Inbox {

    private LinkedList<Message> messages = new LinkedList<>();
    private int currentMessage = 0;

    /**
     * Gets the current message in the inbox
     * @return the current message
     */
    public Message getCurrentMessage() {
        try {
            return this.messages.get(this.currentMessage);
        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            return null;
        }
    }

    /**
     * Sets the current message to the previous message (if one exists)
     */
    public void moveBackwards() {
        if (this.currentMessage > 0) {
            this.currentMessage -= 1;
        }
    }

    /**
     * Sets the current message to the next message (if one exists)
     */
    public void moveForwards() {
        if (this.currentMessage < this.messages.size() - 1) {
            this.currentMessage += 1;
        }
    }

    /**
     * Adds a new message to the inbox and sets it as the current message
     * @param message the message to add to the inbox
     */
    public void addMessage(Message message) {
        this.messages.add(message);
        this.currentMessage = this.messages.size() - 1;
    }

    /**
     * Deletes a message from the inbox
     */
    public void deleteMessage() {
        if (this.messages.size() > 0) {
            this.messages.remove(this.currentMessage);
            if (this.currentMessage > this.messages.size() - 1 && this.currentMessage != 0) {
                this.currentMessage = this.messages.size() - 1;
            }
        }
    }

}
