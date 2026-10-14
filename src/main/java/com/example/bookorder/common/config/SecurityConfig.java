package com.example.bookorder.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import com.example.bookorder.common.exception.enums.ErrorCodeEnum;
import com.example.bookorder.controller.support.SecurityErrorWriter;

@Configuration
public class SecurityConfig {
	/**
	 * Keep authentication enforced while serializing security errors as HTTP-200
	 * API failures.
	 */
	@Bean
	SecurityFilterChain security(HttpSecurity http, SecurityErrorWriter errors) throws Exception {
		// (0) Entry point: renders every auth failure as the HTTP-200 API envelope.
		AuthenticationEntryPoint unauthorized = (request, response, exception) -> errors.write(request, response,
				ErrorCodeEnum.UNAUTHORIZED);

		// (1) CSRF: disabled so stateless Basic-auth calls are not blocked.
		http.csrf(csrf -> csrf.disable());

		// (2) Authorization rules: Swagger/OpenAPI docs are public, everything else
		// requires authentication. Rules are matched top-down, first match wins.
		http.authorizeHttpRequests(access -> access
				.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
				.anyRequest().authenticated());

		// (3) Authentication mechanism: HTTP Basic with the shared entry point.
		http.httpBasic(basic -> basic.authenticationEntryPoint(unauthorized));

		// (4) Exception handling: map unauthenticated / forbidden to API errors.
		http.exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorized)
				.accessDeniedHandler(
						(request, response, exception) -> errors.write(request, response, ErrorCodeEnum.FORBIDDEN)));

		// (5) Session policy: never create an HTTP session (fully stateless).
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		return http.build();
	}
}
