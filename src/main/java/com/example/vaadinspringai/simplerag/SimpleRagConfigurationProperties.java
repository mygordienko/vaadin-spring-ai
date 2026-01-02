package com.example.vaadinspringai.simplerag;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.simplerag")
public class SimpleRagConfigurationProperties {
    private DataSize maxFileSizeClientCheck;
}
