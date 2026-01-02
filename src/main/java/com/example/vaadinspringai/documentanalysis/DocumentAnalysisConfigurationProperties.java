package com.example.vaadinspringai.documentanalysis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.documentanalysis")
public class DocumentAnalysisConfigurationProperties {
    private int maxFileSizeClientCheckMB;
}
