package com.example.grpc_service;

import com.example.service.grpc.GreetingRequest;
import com.example.service.grpc.GreetingResponse;
import com.example.service.grpc.GreetingServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Service;

import java.util.Objects;

@SpringBootApplication
public class GrpcServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServiceApplication.class, args);
	}

	@Bean
	@GlobalServerInterceptor
	AuthenticationProcessInterceptor authenticationProcessInterceptor(GrpcSecurity grpcSecurity) throws Exception {
		return grpcSecurity
			.authorizeRequests(requests -> requests.methods("GreetingService/Greet")
				.authenticated()
				.methods("grpc.*/*")
				.permitAll()
				.allRequests()
				.denyAll())
			.oauth2ResourceServer(c -> c.jwt(Customizer.withDefaults()))
			.build();
	}

}

@Service
class DefaultGreetingService extends GreetingServiceGrpc.GreetingServiceImplBase {

	private final SecurityContextHolderStrategy securityContextHolder = SecurityContextHolder
		.getContextHolderStrategy();

	@Override
	public void greet(GreetingRequest request, StreamObserver<GreetingResponse> responseObserver) {
		var authentication = this.securityContextHolder.getContext().getAuthentication();
		var name = Objects.requireNonNull(authentication).getName();
		var message = "Hello, " + name + "!";
		var response = GreetingResponse.newBuilder().setMessage(message).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

}
