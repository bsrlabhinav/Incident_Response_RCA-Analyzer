package org.example.incidentresponse.config;

import org.example.incidentresponse.enums.Severity;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "incident.sla")
public class SlaProperties {

    private SlaThreshold critical = new SlaThreshold();
    private SlaThreshold high = new SlaThreshold();
    private SlaThreshold medium = new SlaThreshold();
    private SlaThreshold low = new SlaThreshold();

    public SlaThreshold getThresholdForSeverity(Severity severity) {
        return switch (severity) {
            case CRITICAL -> critical;
            case HIGH -> high;
            case MEDIUM -> medium;
            case LOW -> low;
        };
    }

    public SlaThreshold getCritical() { return critical; }
    public void setCritical(SlaThreshold critical) { this.critical = critical; }

    public SlaThreshold getHigh() { return high; }
    public void setHigh(SlaThreshold high) { this.high = high; }

    public SlaThreshold getMedium() { return medium; }
    public void setMedium(SlaThreshold medium) { this.medium = medium; }

    public SlaThreshold getLow() { return low; }
    public void setLow(SlaThreshold low) { this.low = low; }

    public static class SlaThreshold {
        private long acknowledgeMs = 3600000;
        private long resolutionMs = 86400000;

        public long getAcknowledgeMs() { return acknowledgeMs; }
        public void setAcknowledgeMs(long acknowledgeMs) { this.acknowledgeMs = acknowledgeMs; }

        public long getResolutionMs() { return resolutionMs; }
        public void setResolutionMs(long resolutionMs) { this.resolutionMs = resolutionMs; }
    }
}
