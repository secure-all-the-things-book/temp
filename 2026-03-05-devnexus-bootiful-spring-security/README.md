# Secure all the things! 

## Talk

* Skip to JdbcUserDetailsManager
* Password Encoding (slide but short)
* Password4j - it exists
* Password Migration
* Passwords are bad (slide but short)
* OTT
* WebAuthn
* AuthZ - roles and such
* MFA
* AuthZ Server
* HTTP Client / Service
** Use Spring MVC integration (registered oauth2)
** Interceptor 
** Declarative Interface
** MVC Gateway
* MCP Client / Server
* GRPC Client / Server
* Integration
* CLI

## TODO

What _can't_ you secure with Spring Security? I'll answer for you: nothing! 
* authentication/authorization
* users/pw
* password migration
* password encoders
* default password encoders 
* password4j
* customizers for authorization
* one time token
* webauthn 
* global MFA
- customizing MFA with custom authorization rules (`AuthorizationManagerFactories`)
- oauth
- auth server
- oauth client talking to a backend resource server with..
	- `RestClient` and injected `@AuthenticatedPrincipal`
	- RestClient with interceptor 
	- declarative interface client `@RegisterdClient`
	- gateway and token relay 
- new MCP client talking to an MCP service
- new gRPC client talking to a gRPC service
- new graphql client to a graphql service (method security)
- CLIs
- messaging / integration with spring integration
- these are all on the web, but what about offline messaging wth Spring Integration (e.g. Kafka or RabbitMQ?)
