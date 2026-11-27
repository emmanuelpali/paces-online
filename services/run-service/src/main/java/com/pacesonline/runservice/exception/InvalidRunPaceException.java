package com.pacesonline.runservice.exception;

public class InvalidRunPaceException extends RuntimeException {

    public InvalidRunPaceException() {
        super("Distance and duration must produce a rounded pace between 1 and "
                + Integer.MAX_VALUE + " seconds per kilometre.");
    }
}