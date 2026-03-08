package org.example.incidentresponse.reporting;

import java.time.Instant;

public record TimeRange(Instant from, Instant to) {

    public TimeRange {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
    }
}
