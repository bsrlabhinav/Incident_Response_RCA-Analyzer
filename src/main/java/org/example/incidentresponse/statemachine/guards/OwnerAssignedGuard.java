package org.example.incidentresponse.statemachine.guards;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.statemachine.TransitionGuard;
import org.springframework.stereotype.Component;

@Component
public class OwnerAssignedGuard implements TransitionGuard {

    @Override
    public boolean appliesTo(IncidentStatus from, IncidentStatus to) {
        return from == IncidentStatus.OPEN && to == IncidentStatus.INVESTIGATING;
    }

    @Override
    public void evaluate(IncidentDocument incident) {
        if (incident.getAssigneeId() == null || incident.getAssigneeId().isBlank()) {
            throw new IllegalStateException(
                    "Cannot transition to INVESTIGATING: incident " + incident.getId() + " has no assigned owner");
        }
    }
}
