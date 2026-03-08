package org.example.incidentresponse.repository;

import org.example.incidentresponse.document.RootCauseAnalysisDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface RcaEsRepository extends ElasticsearchRepository<RootCauseAnalysisDocument, String> {

    Optional<RootCauseAnalysisDocument> findByIncidentId(String incidentId);

    boolean existsByIncidentId(String incidentId);
}
