package org.example.incidentresponse.statemachine.guards;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.exception.RcaRequiredException;
import org.example.incidentresponse.repository.RcaEsRepository;
import org.example.incidentresponse.statemachine.TransitionGuard;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RcaExistsGuard implements TransitionGuard {

    private final RcaEsRepository rcaRepository;

    public RcaExistsGuard(RcaEsRepository rcaRepository) {
        this.rcaRepository = rcaRepository;
    }

    @Override
    public boolean appliesTo(IncidentStatus from, IncidentStatus to) {
        return from == IncidentStatus.RCA_PENDING && to == IncidentStatus.CLOSED;
    }

    @Override
    public void evaluate(IncidentDocument incident) {
        if (!rcaRepository.existsByIncidentId(incident.getId())) {
            throw new RcaRequiredException(UUID.fromString(incident.getId()));
        }
    }
}
