package org.example.incidentresponse.service;

import org.example.incidentresponse.document.IncidentEvidenceDocument;
import org.example.incidentresponse.dto.CreateEvidenceRequest;
import org.example.incidentresponse.dto.EvidenceResponse;
import org.example.incidentresponse.repository.IncidentEvidenceEsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);
    private final IncidentEvidenceEsRepository evidenceRepository;

    public EvidenceService(IncidentEvidenceEsRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    public EvidenceResponse attachEvidence(String incidentId, CreateEvidenceRequest request) {
        IncidentEvidenceDocument doc = new IncidentEvidenceDocument();
        doc.setIncidentId(incidentId);
        doc.setFileName(request.fileName());
        doc.setFileUrl(request.fileUrl());
        doc.setDescription(request.description());
        doc.setUploadedBy(request.uploadedBy().toString());
        doc.setUploadedAt(Instant.now());

        doc = evidenceRepository.save(doc);
        log.info("Evidence attached: incident={} file={}", incidentId, request.fileName());
        return toResponse(doc);
    }

    public List<EvidenceResponse> getEvidence(String incidentId) {
        return evidenceRepository.findByIncidentIdOrderByUploadedAtDesc(incidentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private EvidenceResponse toResponse(IncidentEvidenceDocument doc) {
        return new EvidenceResponse(doc.getId(), doc.getIncidentId(), doc.getFileName(),
                doc.getFileUrl(), doc.getDescription(), doc.getUploadedBy(), doc.getUploadedAt());
    }
}
