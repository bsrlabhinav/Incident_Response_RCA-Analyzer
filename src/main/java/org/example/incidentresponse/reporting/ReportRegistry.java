package org.example.incidentresponse.reporting;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Auto-discovers all ReportDefinition beans and provides lookup by reportType.
 */
@Component
public class ReportRegistry {

    private final Map<String, ReportDefinition> definitions;

    public ReportRegistry(List<ReportDefinition> definitionBeans) {
        this.definitions = definitionBeans.stream()
                .collect(Collectors.toMap(ReportDefinition::getReportType, Function.identity()));
    }

    public Optional<ReportDefinition> get(String reportType) {
        return Optional.ofNullable(definitions.get(reportType));
    }

    public Collection<ReportDefinition> getAll() {
        return definitions.values();
    }

    public boolean exists(String reportType) {
        return definitions.containsKey(reportType);
    }
}
