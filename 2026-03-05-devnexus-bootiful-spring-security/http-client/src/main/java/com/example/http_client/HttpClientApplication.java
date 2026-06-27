package com.example.http_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.ClientAttributes.clientRegistrationId;

@SpringBootApplication
public class HttpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(HttpClientApplication.class, args);
    }

}


@Controller
@ResponseBody
class DefaultRestClientController {

    private final RestClient restClient;

    private final RestClient interceptorRestClient;

    DefaultRestClientController(OAuth2AuthorizedClientManager authorizedClientManager,
                                RestClient.Builder restClient) {
        this.restClient = restClient.build();

        this.interceptorRestClient = restClient
                .requestInterceptor(new OAuth2ClientHttpRequestInterceptor(authorizedClientManager))
                .build();

    }

    record Response(String name) {
    }

    @GetMapping("/interceptor")
    Response interceptor () {
        return this.interceptorRestClient
                .get()
                .uri("http://localhost:8085/rest")
                .attributes(clientRegistrationId("spring"))
                .retrieve()
                .body(Response.class);
    }

    @GetMapping("/header")
    Response header(@RegisteredOAuth2AuthorizedClient("spring") OAuth2AuthorizedClient authorizedClient) {
        return this.restClient
                .get()
                .uri("http://localhost:8085/rest")
                .headers(h -> h.setBearerAuth(
                        authorizedClient.getAccessToken().getTokenValue()))
                .retrieve()
                .body(Response.class);
    }

}