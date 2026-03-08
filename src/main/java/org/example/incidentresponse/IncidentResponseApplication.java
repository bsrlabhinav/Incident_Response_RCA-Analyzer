package org.example.incidentresponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableElasticsearchRepositories
public class IncidentResponseApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentResponseApplication.class, args);
    }
}
