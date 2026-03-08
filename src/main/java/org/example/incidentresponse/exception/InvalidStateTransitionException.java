package org.example.incidentresponse.exception;

import org.example.incidentresponse.enums.IncidentStatus;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(IncidentStatus from, IncidentStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }
}
