package org.example.incidentresponse.exception;

import java.util.UUID;

public class RcaRequiredException extends RuntimeException {

    public RcaRequiredException(UUID incidentId) {
        super("Root cause analysis is required before closing incident: " + incidentId);
    }
}
