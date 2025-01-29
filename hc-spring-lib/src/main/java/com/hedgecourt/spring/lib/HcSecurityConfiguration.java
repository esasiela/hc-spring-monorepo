package com.hedgecourt.spring.lib;

import com.hedgecourt.spring.lib.service.HcPublicPathsMatcherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class HcSecurityConfiguration {
  private static final Logger log = LoggerFactory.getLogger(HcSecurityConfiguration.class);

  private final HcPublicPathsMatcherService publicPathsMatcherService;

  public HcSecurityConfiguration(HcPublicPathsMatcherService publicPathsMatcherService) {
    this.publicPathsMatcherService = publicPathsMatcherService;
  }

  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    if (log.isInfoEnabled()) log.info("Setting up HC Sandbox security filter chain");

    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            (authorizedRequests) ->
                authorizedRequests
                    .requestMatchers(publicPathsMatcherService.getExcludedPathsMatcher())
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            (sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    http.oauth2ResourceServer(
        oauth2 ->
            oauth2.jwt(
                jwt -> {
                  // jwt.decoder(JwtDecoders.fromIssuerLocation(issuerUri));
                  jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                }));

    return http.build();
  }

  @Bean
  @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    if (log.isInfoEnabled())
      log.info("Setting up HC Sandbox jwt authentication converter: {}", converter);

    return converter;
  }
}
