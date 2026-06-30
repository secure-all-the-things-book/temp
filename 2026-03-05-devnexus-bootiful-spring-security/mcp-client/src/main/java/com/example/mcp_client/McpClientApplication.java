package com.example.mcp_client;

import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import org.springaicommunity.mcp.security.client.sync.oauth2.http.client.OAuth2AuthorizationCodeSyncHttpRequestCustomizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
public class McpClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}

}

@Configuration
class McpClientSecurityConfiguration {

	@Bean
	OAuth2AuthorizationCodeSyncHttpRequestCustomizer auth2AuthorizationCodeSyncHttpRequestCustomizer(
			OAuth2AuthorizedClientManager authorizedClientManager) {
		return new OAuth2AuthorizationCodeSyncHttpRequestCustomizer(authorizedClientManager, "spring");
	}

	@Bean
	McpSyncClientCustomizer mcpSyncClientCustomizer() {
		return (_, spec) -> spec.transportContextProvider(new AuthenticationMcpTransportContextProvider());
	}

}

@Controller
@ResponseBody
class SchedulingAssistantController {

	private final ChatClient ai;

	SchedulingAssistantController(ToolCallbackProvider toolCallbackProvider, ChatClient.Builder ai) {
		this.ai = ai
			.defaultSystem(
					"""

							You are an AI powered assistant to help people adopt a dog from the adoptions agency named Pooch Palace with locations in Atlanta, Seoul, Tokyo,
							Singapore, Paris, Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs availables will be presented below.
							If there is no information, then return a polite response suggesting wes don't have any dogs available.

							If somebody asks for a time to pick up the dog, don't ask other questions: simply provide a time by consulting the tools you have available.

							""")
			.defaultToolCallbacks(toolCallbackProvider)
			.build();
	}

	@GetMapping("/mcp")
	String ask() {
		return this.ai.prompt()
			.user("when can i pick up the Dog named Fido from the Atlanta, GA Pooch Palace location?")
			.call()
			.content();
	}

}
