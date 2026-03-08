package org.example.incidentresponse.statemachine;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.exception.InvalidStateTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.example.incidentresponse.enums.IncidentStatus.*;

@Component
public class IncidentStateMachine {

    private static final Logger log = LoggerFactory.getLogger(IncidentStateMachine.class);

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            OPEN, Set.of(INVESTIGATING),
            INVESTIGATING, Set.of(FIXED),
            FIXED, Set.of(RCA_PENDING),
            RCA_PENDING, Set.of(CLOSED)
    );

    private final List<TransitionGuard> guards;
    private final List<TransitionAction> actions;

    public IncidentStateMachine(List<TransitionGuard> guards, List<TransitionAction> actions) {
        this.guards = guards;
        this.actions = actions;
    }

    public void transition(IncidentDocument incident, IncidentStatus targetStatus, String userId) {
        IncidentStatus currentStatus = IncidentStatus.valueOf(incident.getStatus());

        Set<IncidentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new InvalidStateTransitionException(currentStatus, targetStatus);
        }

        for (TransitionGuard guard : guards) {
            if (guard.appliesTo(currentStatus, targetStatus)) {
                guard.evaluate(incident);
            }
        }

        log.info("Transitioning incident={} from {} to {} by user={}",
                incident.getId(), currentStatus, targetStatus, userId);

        incident.setStatus(targetStatus.name());

        for (TransitionAction action : actions) {
            action.execute(incident, currentStatus, targetStatus, userId);
        }
    }

    public boolean isValidTransition(IncidentStatus from, IncidentStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
