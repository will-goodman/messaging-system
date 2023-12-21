package com.willgoodman.messagingsystem.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

/**
 * Inbox which contains all messages for a given user and tracks the current message
 */
public class Inbox {

    private static final Logger LOGGER = LogManager.getLogger(Inbox.class);
    private LinkedList<Message> messages = new LinkedList<>();
    private int currentMessage = 0;

    /**
     * Gets the current message in the inbox
     * @return the current message
     */
    public Message getCurrentMessage() {
        LOGGER.info("Running Inbox.getCurrentMessage()");
        try {
            Message currentMessage = this.messages.get(this.currentMessage);
            LOGGER.debug(String.format("Current Message: %s", currentMessage));
            return currentMessage;
        } catch (IndexOutOfBoundsException | NullPointerException ex) {
            LOGGER.warn("Empty inbox");
            return null;
        }
    }

    /**
     * Sets the current message to the previous message (if one exists)
     */
    public void moveBackwards() {
        LOGGER.info("Running Inbox.moveBackwards()");
        if (this.currentMessage > 0) {
            this.currentMessage -= 1;
        }
        LOGGER.debug(String.format("Current message: %d", this.currentMessage));
    }

    /**
     * Sets the current message to the next message (if one exists)
     */
    public void moveForwards() {
        LOGGER.info("Running Inbox.moveForwards()");
        if (this.currentMessage < this.messages.size() - 1) {
            this.currentMessage += 1;
        }
        LOGGER.debug(String.format("Current message: %d", this.currentMessage));
    }

    /**
     * Adds a new message to the inbox and sets it as the current message
     * @param message the message to add to the inbox
     */
    public void addMessage(Message message) {
        LOGGER.info("Running Inbox.addMessage()");
        this.messages.add(message);
        this.currentMessage = this.messages.size() - 1;
        LOGGER.debug(String.format("Added new message: %s", message.toString()));
    }

    /**
     * Deletes a message from the inbox
     */
    public void deleteMessage() {
        LOGGER.info("Running Inbox.deleteMessage()");
        if (this.messages.size() > 0) {
            LOGGER.debug(String.format("Deleting: %s", this.getCurrentMessage()));
            this.messages.remove(this.currentMessage);
            if (this.currentMessage > this.messages.size() - 1 && this.currentMessage != 0) {
                this.currentMessage = this.messages.size() - 1;
            }
            LOGGER.debug(String.format("New Current Message: %s", this.getCurrentMessage()));
        }
    }

}
