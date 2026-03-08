package org.example.incidentresponse.reporting;

import java.util.Map;

/**
 * A single row in the Shifu response, representing one unique combination of dimension values
 * and the computed measurements for that combination.
 */
public record ShifuBucket(
        Map<String, String> dimensions,
        Map<String, Object> measurements,
        long docCount
) {}
