package com.pacesonline.runservice.exception;

public class RunNotFoundException extends RuntimeException {

    public RunNotFoundException() {
        super("Run not found.");
    }
}
