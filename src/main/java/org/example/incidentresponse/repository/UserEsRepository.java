package org.example.incidentresponse.repository;

import org.example.incidentresponse.document.UserDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface UserEsRepository extends ElasticsearchRepository<UserDocument, String> {

    Optional<UserDocument> findByDisplayName(String displayName);
}
