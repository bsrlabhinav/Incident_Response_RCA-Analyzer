package org.example.incidentresponse.reporting;

import java.util.List;
import java.util.Map;

/**
 * The output of the Shifu reporting engine.
 */
public record ShifuResponse(
        String reportType,
        TimeRange timeRange,
        long totalHits,
        Map<String, Object> totals,
        List<ShifuBucket> buckets
) {}
