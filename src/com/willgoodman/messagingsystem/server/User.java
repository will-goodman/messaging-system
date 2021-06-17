package com.willgoodman.messagingsystem.server;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Stores all information required by the server about a user.
 * Including:
 * - a unique username (cannot be changed)
 * - a message queue of messages waiting to be sent to the user when they next login
 */
public class User {

    private final String USERNAME;
    private Queue<Message> inbox = new LinkedList<Message>();

    /**
     * Constructor.
     *
     * @param username Unique username for the user. Cannot be changed
     */
    public User(String username) {
        this.USERNAME = username;
    }

}
