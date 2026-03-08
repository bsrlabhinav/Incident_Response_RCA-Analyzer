package org.example.incidentresponse.repository;

import org.example.incidentresponse.document.IncidentEvidenceDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface IncidentEvidenceEsRepository extends ElasticsearchRepository<IncidentEvidenceDocument, String> {

    List<IncidentEvidenceDocument> findByIncidentIdOrderByUploadedAtDesc(String incidentId);
}
