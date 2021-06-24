package com.willgoodman.messagingsystem.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stores all information required by the server about a user.
 * Including:
 * - a unique username (cannot be changed)
 * - a message queue of messages waiting to be sent to the user when they next login
 */
public class User {

    private static final Logger LOGGER = LogManager.getLogger(User.class);
    private final String USERNAME;
    private Inbox inbox = new Inbox();

    /**
     * Constructor.
     *
     * @param username Unique username for the user. Cannot be changed
     */
    public User(String username) {
        LOGGER.info("Constructing User");
        this.USERNAME = username;
    }

    /**
     * Gets the user's inbox
     *
     * @return the user's inbox
     */
    public Inbox getInbox() {
        LOGGER.info("Running User.getInbox()");
        return this.inbox;
    }

}
