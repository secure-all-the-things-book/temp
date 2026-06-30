package com.example.grpc_client;

import com.example.client.grpc.GreetingRequest;
import com.example.client.grpc.GreetingServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.interceptor.security.BearerTokenAuthenticationInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootApplication
public class GrpcClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcClientApplication.class, args);
	}

	private String token(OAuth2AuthorizedClientManager authorizedClientManager) {
		if (SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication() instanceof OAuth2AuthenticationToken auth2AuthenticationToken) {
			var authRequest = OAuth2AuthorizeRequest
				.withClientRegistrationId(auth2AuthenticationToken.getAuthorizedClientRegistrationId())
				.principal(auth2AuthenticationToken)
				.build();
			var oAuth2AuthorizedClient = authorizedClientManager.authorize(authRequest);
			return Objects.requireNonNull(oAuth2AuthorizedClient).getAccessToken().getTokenValue();
		}
		throw new IllegalStateException("no authentication");
	}

	@Bean
	@Lazy
	GreetingServiceGrpc.GreetingServiceBlockingStub greetingServiceBlockingStub(GrpcChannelFactory channels,
			OAuth2AuthorizedClientManager authorizedClientManager) {
		var bearerTokenInterceptor = new BearerTokenAuthenticationInterceptor(() -> token(authorizedClientManager));
		var options = ChannelBuilderOptions.defaults().withInterceptors(List.of(bearerTokenInterceptor));
		var channel = channels.createChannel("localhost:8086", options);
		return GreetingServiceGrpc.newBlockingStub(channel);
	}

}

@Controller
@ResponseBody
class GreetingClient {

	private final GreetingServiceGrpc.GreetingServiceBlockingStub client;

	GreetingClient(GreetingServiceGrpc.GreetingServiceBlockingStub client) {
		this.client = client;
	}

	@GetMapping("/grpc")
	Map<String, String> message() {
		var request = GreetingRequest.newBuilder().setName("Spring fans").build();
		var msg = this.client.greet(request);
		return Map.of("message", msg.getMessage());
	}

}