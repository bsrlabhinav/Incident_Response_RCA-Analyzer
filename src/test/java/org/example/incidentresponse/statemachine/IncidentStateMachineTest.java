package org.example.incidentresponse.statemachine;

import org.example.incidentresponse.document.IncidentDocument;
import org.example.incidentresponse.enums.IncidentStatus;
import org.example.incidentresponse.exception.InvalidStateTransitionException;
import org.example.incidentresponse.exception.RcaRequiredException;
import org.example.incidentresponse.repository.IncidentAuditEsRepository;
import org.example.incidentresponse.repository.RcaEsRepository;
import org.example.incidentresponse.statemachine.actions.AuditAction;
import org.example.incidentresponse.statemachine.actions.SlaAction;
import org.example.incidentresponse.statemachine.guards.OwnerAssignedGuard;
import org.example.incidentresponse.statemachine.guards.RcaExistsGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentStateMachineTest {

    @Mock private RcaEsRepository rcaRepository;
    @Mock private IncidentAuditEsRepository auditRepository;

    private IncidentStateMachine stateMachine;
    private final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        OwnerAssignedGuard ownerGuard = new OwnerAssignedGuard();
        RcaExistsGuard rcaGuard = new RcaExistsGuard(rcaRepository);
        AuditAction auditAction = new AuditAction(auditRepository);
        SlaAction slaAction = new SlaAction();

        stateMachine = new IncidentStateMachine(
                List.of(ownerGuard, rcaGuard),
                List.of(auditAction, slaAction)
        );
    }

    private IncidentDocument createIncident(IncidentStatus status, String assigneeId) {
        IncidentDocument doc = new IncidentDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setTitle("Test incident");
        doc.setSeverity("HIGH");
        doc.setStatus(status.name());
        doc.setReporterId(UUID.randomUUID().toString());
        doc.setAssigneeId(assigneeId);
        doc.setCreatedAt(Instant.now());
        doc.setAcknowledgeSlaMs(3600000L);
        doc.setResolutionSlaMs(86400000L);
        return doc;
    }

    @Test
    @DisplayName("OPEN -> INVESTIGATING succeeds when owner is assigned")
    void openToInvestigating_withOwner_succeeds() {
        IncidentDocument doc = createIncident(IncidentStatus.OPEN, UUID.randomUUID().toString());
        stateMachine.transition(doc, IncidentStatus.INVESTIGATING, userId);
        assertThat(doc.getStatus()).isEqualTo("INVESTIGATING");
    }

    @Test
    @DisplayName("OPEN -> INVESTIGATING fails when no owner is assigned")
    void openToInvestigating_withoutOwner_fails() {
        IncidentDocument doc = createIncident(IncidentStatus.OPEN, null);
        assertThatThrownBy(() -> stateMachine.transition(doc, IncidentStatus.INVESTIGATING, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no assigned owner");
    }

    @Test
    @DisplayName("INVESTIGATING -> FIXED succeeds and sets SLA resolution fields")
    void investigatingToFixed_succeeds() {
        IncidentDocument doc = createIncident(IncidentStatus.INVESTIGATING, UUID.randomUUID().toString());
        stateMachine.transition(doc, IncidentStatus.FIXED, userId);
        assertThat(doc.getStatus()).isEqualTo("FIXED");
        assertThat(doc.getResolvedAt()).isNotNull();
        assertThat(doc.getActualResolutionMs()).isNotNull();
    }

    @Test
    @DisplayName("FIXED -> RCA_PENDING succeeds")
    void fixedToRcaPending_succeeds() {
        IncidentDocument doc = createIncident(IncidentStatus.FIXED, UUID.randomUUID().toString());
        stateMachine.transition(doc, IncidentStatus.RCA_PENDING, userId);
        assertThat(doc.getStatus()).isEqualTo("RCA_PENDING");
    }

    @Test
    @DisplayName("RCA_PENDING -> CLOSED succeeds when RCA exists")
    void rcaPendingToClosed_withRca_succeeds() {
        IncidentDocument doc = createIncident(IncidentStatus.RCA_PENDING, UUID.randomUUID().toString());
        when(rcaRepository.existsByIncidentId(doc.getId())).thenReturn(true);
        stateMachine.transition(doc, IncidentStatus.CLOSED, userId);
        assertThat(doc.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("RCA_PENDING -> CLOSED fails without RCA")
    void rcaPendingToClosed_withoutRca_fails() {
        IncidentDocument doc = createIncident(IncidentStatus.RCA_PENDING, UUID.randomUUID().toString());
        when(rcaRepository.existsByIncidentId(doc.getId())).thenReturn(false);
        assertThatThrownBy(() -> stateMachine.transition(doc, IncidentStatus.CLOSED, userId))
                .isInstanceOf(RcaRequiredException.class);
    }

    @Test
    @DisplayName("Invalid transition OPEN -> FIXED is rejected")
    void openToFixed_rejected() {
        IncidentDocument doc = createIncident(IncidentStatus.OPEN, UUID.randomUUID().toString());
        assertThatThrownBy(() -> stateMachine.transition(doc, IncidentStatus.FIXED, userId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Invalid backward transition INVESTIGATING -> OPEN is rejected")
    void investigatingToOpen_rejected() {
        IncidentDocument doc = createIncident(IncidentStatus.INVESTIGATING, UUID.randomUUID().toString());
        assertThatThrownBy(() -> stateMachine.transition(doc, IncidentStatus.OPEN, userId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("CLOSED state has no valid transitions")
    void closed_noTransitions() {
        IncidentDocument doc = createIncident(IncidentStatus.CLOSED, UUID.randomUUID().toString());
        assertThatThrownBy(() -> stateMachine.transition(doc, IncidentStatus.OPEN, userId))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("isValidTransition returns correct results")
    void isValidTransition_checks() {
        assertThat(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING)).isTrue();
        assertThat(stateMachine.isValidTransition(IncidentStatus.INVESTIGATING, IncidentStatus.FIXED)).isTrue();
        assertThat(stateMachine.isValidTransition(IncidentStatus.FIXED, IncidentStatus.RCA_PENDING)).isTrue();
        assertThat(stateMachine.isValidTransition(IncidentStatus.RCA_PENDING, IncidentStatus.CLOSED)).isTrue();
        assertThat(stateMachine.isValidTransition(IncidentStatus.OPEN, IncidentStatus.CLOSED)).isFalse();
        assertThat(stateMachine.isValidTransition(IncidentStatus.CLOSED, IncidentStatus.OPEN)).isFalse();
    }
}
