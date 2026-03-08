package org.example.incidentresponse.statemachine.actions;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.statemachine.TransitionAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Updates denormalized SLA fields on the IncidentDocument during status transitions.
 * The updated document is persisted by the caller (IncidentService).
 */
@Component
@Order(2)
public class SlaAction implements TransitionAction {

    private static final Logger log = LoggerFactory.getLogger(SlaAction.class);

    @Override
    public void execute(IncidentDocument incident, IncidentStatus from, IncidentStatus to, String userId) {
        Instant now = Instant.now();

        if (to == IncidentStatus.INVESTIGATING) {
            incident.setAcknowledgedAt(now);
            long elapsed = Duration.between(incident.getCreatedAt(), now).toMillis();
            incident.setActualAcknowledgeMs(elapsed);

            if (incident.getAcknowledgeSlaMs() != null && elapsed > incident.getAcknowledgeSlaMs()) {
                incident.setAcknowledgeSlaBreached(true);
                log.warn("SLA breached for acknowledgement: incident={} elapsed={}ms threshold={}ms",
                        incident.getId(), elapsed, incident.getAcknowledgeSlaMs());
            }
        } else if (to == IncidentStatus.FIXED) {
            incident.setResolvedAt(now);
            long elapsed = Duration.between(incident.getCreatedAt(), now).toMillis();
            incident.setActualResolutionMs(elapsed);

            if (incident.getResolutionSlaMs() != null && elapsed > incident.getResolutionSlaMs()) {
                incident.setResolutionSlaBreached(true);
                log.warn("SLA breached for resolution: incident={} elapsed={}ms threshold={}ms",
                        incident.getId(), elapsed, incident.getResolutionSlaMs());
            }
        }
    }
}
