package org.example.incidentresponse.repository;

import org.example.incidentresponse.document.IncidentAuditDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface IncidentAuditEsRepository extends ElasticsearchRepository<IncidentAuditDocument, String> {

    List<IncidentAuditDocument> findByIncidentIdOrderByCreatedAtDesc(String incidentId);
}
