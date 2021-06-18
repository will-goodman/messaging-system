package com.willgoodman.messagingsystem.server;

/**
 * Stores all information required by the server about a user.
 * Including:
 * - a unique username (cannot be changed)
 * - a message queue of messages waiting to be sent to the user when they next login
 */
public class User {

    private final String USERNAME;
    private Inbox inbox;

    /**
     * Constructor.
     *
     * @param username Unique username for the user. Cannot be changed
     */
    public User(String username) {
        this.USERNAME = username;
    }

    public Inbox getInbox() {
        return this.inbox;
    }

}
