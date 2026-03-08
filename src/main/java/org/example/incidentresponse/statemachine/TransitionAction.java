package org.example.incidentresponse.statemachine;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;

public interface TransitionAction {

    void execute(IncidentDocument incident, IncidentStatus from, IncidentStatus to, String userId);
}
