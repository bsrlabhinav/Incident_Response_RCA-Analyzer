package org.example.incidentresponse.service;

import org.example.incidentresponse.config.SlaProperties;
import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.dto.SlaResponse;
import org.example.incidentresponse.enums.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SlaService {

    private static final Logger log = LoggerFactory.getLogger(SlaService.class);
    private final SlaProperties slaProperties;

    public SlaService(SlaProperties slaProperties) {
        this.slaProperties = slaProperties;
    }

    /**
     * Populates the SLA threshold fields on the denormalized incident document.
     */
    public void initSlaFields(IncidentDocument incident) {
        Severity severity = Severity.valueOf(incident.getSeverity());
        SlaProperties.SlaThreshold threshold = slaProperties.getThresholdForSeverity(severity);

        incident.setAcknowledgeSlaMs(threshold.getAcknowledgeMs());
        incident.setResolutionSlaMs(threshold.getResolutionMs());
        incident.setAcknowledgeSlaBreached(false);
        incident.setResolutionSlaBreached(false);

        log.info("SLA fields initialized: incident={} ackThreshold={}ms resThreshold={}ms",
                incident.getId(), threshold.getAcknowledgeMs(), threshold.getResolutionMs());
    }

    public SlaResponse getSlaMetrics(IncidentDocument incident) {
        return new SlaResponse(
                incident.getId(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getAcknowledgeSlaMs(),
                incident.getResolutionSlaMs(),
                incident.getAcknowledgeSlaBreached() != null && incident.getAcknowledgeSlaBreached(),
                incident.getResolutionSlaBreached() != null && incident.getResolutionSlaBreached(),
                incident.getActualAcknowledgeMs(),
                incident.getActualResolutionMs()
        );
    }
}
