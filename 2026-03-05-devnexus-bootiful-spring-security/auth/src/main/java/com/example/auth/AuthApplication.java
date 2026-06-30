package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Map;

import static org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer.mcpAuthorizationServer;

@EnableMultiFactorAuthentication(authorities = {})
@SpringBootApplication
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	InMemoryUserDetailsManager inMemoryUserDetailsManager(PasswordEncoder pw) {
		return new InMemoryUserDetailsManager(
				User.withUsername("josh").password(pw.encode("pw")).authorities("USER").build(),
				User.withUsername("rob").password(pw.encode("pw")).authorities("ADMIN", "USER").build());
	}

	@Bean
	SecurityFilterChain springSecurity(HttpSecurity http) {
		var adm = AuthorizationManagerFactories.multiFactor()
			.requireFactors(FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.OTT_AUTHORITY)
			.build();
		http.authorizeHttpRequests(
				x -> x.requestMatchers("/oauth2/authorize").access(adm.authenticated()).anyRequest().authenticated())
			.formLogin(Customizer.withDefaults())
			.with(mcpAuthorizationServer(), Customizer.withDefaults())
			.oauth2AuthorizationServer(c -> c.oidc(Customizer.withDefaults())
				.deviceAuthorizationEndpoint(Customizer.withDefaults())
				.deviceVerificationEndpoint(Customizer.withDefaults()))
			.webAuthn(w -> w.rpId("localhost").rpName("bootiful").allowedOrigins("http://localhost:9090"))
			.oneTimeTokenLogin(a -> a.tokenGenerationSuccessHandler((_, response, oneTimeToken) -> {
				response.getWriter().println("you've got console mail!");
				response.setContentType(MediaType.TEXT_PLAIN_VALUE);
				IO.println("please go to http://localhost:9090/login/ott?token=" + oneTimeToken.getTokenValue());
			}));
		return http.build();
	}

}
