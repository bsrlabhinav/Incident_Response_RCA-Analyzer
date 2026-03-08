package org.example.incidentresponse.repository;

import org.example.incidentresponse.document.IncidentDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface IncidentEsRepository extends ElasticsearchRepository<IncidentDocument, String> {

    Page<IncidentDocument> findByStatus(String status, Pageable pageable);

    Page<IncidentDocument> findBySeverity(String severity, Pageable pageable);

    Page<IncidentDocument> findByStatusAndSeverity(String status, String severity, Pageable pageable);

    List<IncidentDocument> findByAssigneeId(String assigneeId);
}
