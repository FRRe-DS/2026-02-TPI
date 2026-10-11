package ar.edu.utn.frre.m3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "m3.integration")
public record IntegrationProperties(
    String m4BaseUrl,
    String m6BaseUrl,
    int timeoutMs
) {}
