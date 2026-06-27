package com.example.amqp_client;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class AmqpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmqpClientApplication.class, args);
    }

}

@Controller
@ResponseBody
class ClientController {

    private final MessageChannel requests;

    ClientController(MessageChannel requests) {
        this.requests = requests;
    }

    @GetMapping("/process")
    Map<String, Object> begin(@RegisteredOAuth2AuthorizedClient("spring") OAuth2AuthorizedClient client) {
        var token = client.getAccessToken().getTokenValue();
        IO.println("token: " + token);
        var message = MessageBuilder
                .withPayload(Map.of("customerId", UUID.randomUUID().toString()))
                .setHeader("jwt", token)
                .build();
        var sent = this.requests.send(message);
        return Map.of("sent", sent);
    }
}

@Configuration
class EmailRequestsIntegrationFlowConfiguration {

    private static final String DESTINATION_NAME = "emails";

    @Bean
    IntegrationFlow emailRequestsIntegrationFlow(
            MessageChannel requests, AmqpTemplate template) {

        var outboundAmqpAdapter = Amqp
                .outboundAdapter(template)
                .routingKey(DESTINATION_NAME);

        return IntegrationFlow
                .from(requests)// <2>
                .transform(new ObjectToJsonTransformer()) // <3>
                .handle(outboundAmqpAdapter) // <4>
                .get();
    }

    @Bean
    DirectChannelSpec requests() {
        return MessageChannels.direct();
    }
}
