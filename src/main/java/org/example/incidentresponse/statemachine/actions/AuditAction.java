package org.example.incidentresponse.statemachine.actions;

import org.example.incidentresponse.document.IncidentAuditDocument;
import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.repository.IncidentAuditEsRepository;
import org.example.incidentresponse.statemachine.TransitionAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AuditAction implements TransitionAction {

    private static final Logger log = LoggerFactory.getLogger(AuditAction.class);
    private final IncidentAuditEsRepository auditRepository;

    public AuditAction(IncidentAuditEsRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void execute(IncidentDocument incident, IncidentStatus from, IncidentStatus to, String userId) {
        IncidentAuditDocument audit = new IncidentAuditDocument(
                incident.getId(), userId, "status", from.name(), to.name());
        auditRepository.save(audit);
        log.info("Audit recorded: incident={} status {} -> {} by user={}",
                incident.getId(), from, to, userId);
    }
}
