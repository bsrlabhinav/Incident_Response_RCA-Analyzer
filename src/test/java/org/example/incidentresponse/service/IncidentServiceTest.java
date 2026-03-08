package org.example.incidentresponse.service;

import org.example.incidentresponse.config.SlaProperties;
import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.dto.*;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.enums.Severity;
import org.example.incidentresponse.exception.IncidentNotFoundException;
import org.example.incidentresponse.repository.IncidentEsRepository;
import org.example.incidentresponse.statemachine.IncidentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private IncidentEsRepository incidentRepository;
    @Mock private IncidentStateMachine stateMachine;
    @Mock private SlaService slaService;
    @Mock private AuditService auditService;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                incidentRepository, stateMachine, slaService, auditService);
    }

    @Test
    @DisplayName("Create incident persists document with OPEN status and initializes SLA")
    void createIncident_success() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "DB outage", "Production DB is down", Severity.CRITICAL,
                UUID.randomUUID(), Map.of("team", List.of("platform")));

        when(incidentRepository.save(any(IncidentDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponse result = incidentService.createIncident(request);

        assertThat(result.title()).isEqualTo("DB outage");
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.severity()).isEqualTo("CRITICAL");

        ArgumentCaptor<IncidentDocument> captor = ArgumentCaptor.forClass(IncidentDocument.class);
        verify(incidentRepository).save(captor.capture());
        IncidentDocument saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("OPEN");
        assertThat(saved.getTimePartition()).isNotNull();

        verify(slaService).initSlaFields(any(IncidentDocument.class));
    }

    @Test
    @DisplayName("Get incident throws IncidentNotFoundException when not found")
    void getIncident_notFound() {
        String id = UUID.randomUUID().toString();
        when(incidentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncident(id))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    @DisplayName("Assign owner updates assigneeId and creates audit entry")
    void assignOwner_success() {
        String incidentId = UUID.randomUUID().toString();
        UUID assigneeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        IncidentDocument doc = new IncidentDocument();
        doc.setId(incidentId);
        doc.setAssigneeId(null);

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(doc));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssignOwnerRequest request = new AssignOwnerRequest(assigneeId, userId);
        IncidentResponse result = incidentService.assignOwner(incidentId, request);

        assertThat(result.assigneeId()).isEqualTo(assigneeId.toString());
        verify(auditService).recordChange(eq(incidentId), eq(userId.toString()),
                eq("assigneeId"), isNull(), eq(assigneeId.toString()));
    }

    @Test
    @DisplayName("Update status delegates to state machine")
    void updateStatus_delegatesToStateMachine() {
        String incidentId = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        IncidentDocument doc = new IncidentDocument();
        doc.setId(incidentId);
        doc.setStatus("INVESTIGATING");

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(doc));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateStatusRequest request = new UpdateStatusRequest(IncidentStatus.FIXED, userId);
        incidentService.updateStatus(incidentId, request);

        verify(stateMachine).transition(doc, IncidentStatus.FIXED, userId.toString());
        verify(incidentRepository).save(doc);
    }
}
