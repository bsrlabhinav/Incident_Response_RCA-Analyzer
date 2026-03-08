package org.example.incidentresponse.statemachine;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;

public interface TransitionGuard {

    boolean appliesTo(IncidentStatus from, IncidentStatus to);

    void evaluate(IncidentDocument incident);
}
