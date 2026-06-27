package com.example.mcp_service;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer.mcpServerOAuth2;

@SpringBootApplication
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer
    ) {
        return http -> http
                .with(mcpServerOAuth2(), a -> a.authorizationServer(issuer));
    }
}

record DogAdoptionSchedule(String dogName, Instant when, String user) {
}

@Component
class McpService {

    @McpTool(description = "schedule an appointment to pick up or adopt a dog from a Pooch Palace location")
    DogAdoptionSchedule scheduleAppointment(
            @McpToolParam(description = "the name of the dog") String dogName) {
        var security = Objects.requireNonNull(SecurityContextHolder.getContext()).getAuthentication();
        var instant = Instant.now().plus(3, ChronoUnit.DAYS);
        var dogAdoptionSchedule = new DogAdoptionSchedule(dogName, instant, Objects.requireNonNull(security).getName());
        IO.println("==> " + dogAdoptionSchedule + " for " + dogName);
        return dogAdoptionSchedule;
    }
}