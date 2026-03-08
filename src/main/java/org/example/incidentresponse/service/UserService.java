package org.example.incidentresponse.service;

import org.example.incidentresponse.document.UserDocument;
import org.example.incidentresponse.dto.CreateUserRequest;
import org.example.incidentresponse.dto.UserResponse;
import org.example.incidentresponse.repository.UserEsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserEsRepository userRepository;

    public UserService(UserEsRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(CreateUserRequest request) {
        UserDocument doc = new UserDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setDisplayName(request.displayName());
        doc.setEmail(request.email());
        doc.setRole(request.role() != null ? request.role() : "ENGINEER");
        doc.setCreatedAt(Instant.now());
        doc = userRepository.save(doc);
        log.info("User created: id={} name={}", doc.getId(), doc.getDisplayName());
        return toResponse(doc);
    }

    public List<UserResponse> listUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(this::toResponse)
                .toList();
    }

    public Optional<UserResponse> getUser(String id) {
        return userRepository.findById(id).map(this::toResponse);
    }

    /**
     * Resolves a user ID — if it matches an existing user, returns it.
     * If not found and it looks like a display name, auto-creates a user.
     * Used by the UI to support "type a name, get a user" workflow.
     */
    public UserResponse resolveOrCreate(String nameOrId) {
        Optional<UserDocument> byId = userRepository.findById(nameOrId);
        if (byId.isPresent()) return toResponse(byId.get());

        Optional<UserDocument> byName = userRepository.findByDisplayName(nameOrId);
        if (byName.isPresent()) return toResponse(byName.get());

        long count = userRepository.count();
        String displayName = nameOrId.isBlank()
                ? String.format("user-%03d", count + 1)
                : nameOrId;

        return createUser(new CreateUserRequest(displayName, null, "ENGINEER"));
    }

    private UserResponse toResponse(UserDocument doc) {
        return new UserResponse(doc.getId(), doc.getDisplayName(), doc.getEmail(),
                doc.getRole(), doc.getCreatedAt());
    }
}
