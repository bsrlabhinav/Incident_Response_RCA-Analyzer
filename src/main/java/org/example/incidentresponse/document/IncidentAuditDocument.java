package org.example.incidentresponse.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "incident_audits")
public class IncidentAuditDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String incidentId;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Keyword)
    private String field;

    @Field(type = FieldType.Text)
    private String oldValue;

    @Field(type = FieldType.Text)
    private String newValue;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    public IncidentAuditDocument() {}

    public IncidentAuditDocument(String incidentId, String userId, String field, String oldValue, String newValue) {
        this.incidentId = incidentId;
        this.userId = userId;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
