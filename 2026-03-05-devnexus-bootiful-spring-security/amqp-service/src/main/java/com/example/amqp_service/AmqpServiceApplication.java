package com.example.amqp_service;

import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.util.Assert;

@SpringBootApplication
public class AmqpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmqpServiceApplication.class, args);
    }

}

class Constants {

    public final static String REQUESTS_MESSAGE_CHANNEL = "requests";

    public static final String RABBITMQ_DESTINATION_NAME = "emails";

    public static final String AUTHORIZATION_HEADER_NAME = "jwt";

}

@Configuration
class IntegrationConfiguration {

    @Bean
    IntegrationFlow inboundAmqpRequestsIntegrationFlow(
            @Qualifier(Constants.REQUESTS_MESSAGE_CHANNEL) MessageChannel requests,
            ConnectionFactory connectionFactory) {
        var inboundAmqpAdapter = Amqp
                .inboundAdapter(connectionFactory, Constants.RABBITMQ_DESTINATION_NAME);
        return IntegrationFlow
                .from(inboundAmqpAdapter)
                .channel(requests)
                .get();
    }

    @Bean
    IntegrationFlow requestsIntegrationFlow(
            @Qualifier(Constants.REQUESTS_MESSAGE_CHANNEL) MessageChannel requests) {

        var log = LoggerFactory.getLogger(getClass());

        return IntegrationFlow
                .from(requests)//
                .handle((payload, headers) -> {
                    log.info("----");
                    headers.forEach((key, value) -> log.info("{}={}", key, value));
                    return null;
                })//
                .get();
    }

    // <.>
    @Bean(Constants.REQUESTS_MESSAGE_CHANNEL)
    DirectChannelSpec requests(JwtAuthenticationProvider jwtAuthenticationProvider) {
        // <.>
        var jwtAuthInterceptor = new JwtAuthenticationInterceptor(
                Constants.AUTHORIZATION_HEADER_NAME, jwtAuthenticationProvider);
        // <.>
        var securityContextChannelInterceptor = new SecurityContextChannelInterceptor(
                Constants.AUTHORIZATION_HEADER_NAME);
        // <.>
        var authorizationChannelInterceptor = new AuthorizationChannelInterceptor(
                AuthenticatedAuthorizationManager.authenticated());
        return MessageChannels
                .direct()
                .interceptor(
                        jwtAuthInterceptor,
                        securityContextChannelInterceptor,
                        authorizationChannelInterceptor
                );
    }

    @Bean
    JwtAuthenticationProvider jwtAuthenticationProvider(JwtDecoder decoder) {
        return new JwtAuthenticationProvider(decoder);
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }
}

class JwtAuthenticationInterceptor implements ChannelInterceptor {

    // <.>
    private final JwtAuthenticationProvider authenticationProvider;

    // <.>
    private final String headerName;

    JwtAuthenticationInterceptor(String headerName, JwtAuthenticationProvider ap) {
        this.headerName = headerName;
        this.authenticationProvider = ap;
    }

    @Override
    public org.springframework.messaging.Message<?> preSend(
            org.springframework.messaging.Message<?> message, @NonNull MessageChannel channel) {
        var token = (String) message.getHeaders().get(headerName);
        Assert.hasText(token, "the token must be non-empty!");

        var authentication = this.authenticationProvider
                .authenticate(new BearerTokenAuthenticationToken(token));

        if (authentication != null && authentication.isAuthenticated()) {
            var upt = UsernamePasswordAuthenticationToken.authenticated(authentication.getName(),
                    null, AuthorityUtils.NO_AUTHORITIES);
            return org.springframework.messaging.support.MessageBuilder
                    .fromMessage(message)
                    .setHeader(headerName, upt)
                    .build();
        }
        return org.springframework.messaging.support.MessageBuilder
                .fromMessage(message)
                .setHeader(headerName, null)
                .build();
    }
}

@Configuration
class AmqpConfiguration {

    @Bean
    Queue queue() {
        return QueueBuilder.durable(Constants.RABBITMQ_DESTINATION_NAME).build();
    }

    @Bean
    Exchange exchange() {
        return ExchangeBuilder.directExchange(Constants.RABBITMQ_DESTINATION_NAME).build();
    }

    @Bean
    Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with(Constants.RABBITMQ_DESTINATION_NAME).noargs();
    }

}
