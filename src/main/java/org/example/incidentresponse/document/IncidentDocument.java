package org.example.incidentresponse.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(indexName = "incidents")
@Setting(shards = 3, replicas = 1)
public class IncidentDocument {

    private static final DateTimeFormatter PARTITION_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String severity;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String reporterId;

    @Field(type = FieldType.Keyword)
    private String assigneeId;

    @Field(type = FieldType.Object)
    private Map<String, List<String>> tags = new HashMap<>();

    // Denormalized SLA fields for aggregation queries
    @Field(type = FieldType.Date)
    private Instant acknowledgedAt;

    @Field(type = FieldType.Date)
    private Instant resolvedAt;

    @Field(type = FieldType.Long)
    private Long acknowledgeSlaMs;

    @Field(type = FieldType.Long)
    private Long resolutionSlaMs;

    @Field(type = FieldType.Boolean)
    private Boolean acknowledgeSlaBreached;

    @Field(type = FieldType.Boolean)
    private Boolean resolutionSlaBreached;

    @Field(type = FieldType.Long)
    private Long actualAcknowledgeMs;

    @Field(type = FieldType.Long)
    private Long actualResolutionMs;

    // Denormalized RCA fields for aggregation queries
    @Field(type = FieldType.Keyword)
    private String rcaCategory;

    @Field(type = FieldType.Text)
    private String rcaSummary;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    @Field(type = FieldType.Keyword)
    private String timePartition;

    public IncidentDocument() {}

    public void computeTimePartition() {
        if (createdAt != null) {
            this.timePartition = PARTITION_FMT.format(createdAt.atOffset(ZoneOffset.UTC));
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public Map<String, List<String>> getTags() { return tags; }
    public void setTags(Map<String, List<String>> tags) { this.tags = tags; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getAcknowledgeSlaMs() { return acknowledgeSlaMs; }
    public void setAcknowledgeSlaMs(Long acknowledgeSlaMs) { this.acknowledgeSlaMs = acknowledgeSlaMs; }

    public Long getResolutionSlaMs() { return resolutionSlaMs; }
    public void setResolutionSlaMs(Long resolutionSlaMs) { this.resolutionSlaMs = resolutionSlaMs; }

    public Boolean getAcknowledgeSlaBreached() { return acknowledgeSlaBreached; }
    public void setAcknowledgeSlaBreached(Boolean acknowledgeSlaBreached) { this.acknowledgeSlaBreached = acknowledgeSlaBreached; }

    public Boolean getResolutionSlaBreached() { return resolutionSlaBreached; }
    public void setResolutionSlaBreached(Boolean resolutionSlaBreached) { this.resolutionSlaBreached = resolutionSlaBreached; }

    public Long getActualAcknowledgeMs() { return actualAcknowledgeMs; }
    public void setActualAcknowledgeMs(Long actualAcknowledgeMs) { this.actualAcknowledgeMs = actualAcknowledgeMs; }

    public Long getActualResolutionMs() { return actualResolutionMs; }
    public void setActualResolutionMs(Long actualResolutionMs) { this.actualResolutionMs = actualResolutionMs; }

    public String getRcaCategory() { return rcaCategory; }
    public void setRcaCategory(String rcaCategory) { this.rcaCategory = rcaCategory; }

    public String getRcaSummary() { return rcaSummary; }
    public void setRcaSummary(String rcaSummary) { this.rcaSummary = rcaSummary; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getTimePartition() { return timePartition; }
    public void setTimePartition(String timePartition) { this.timePartition = timePartition; }
}
