package com.hedgecourt.auth.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class HcAuthSecurityConfiguration {

  private static final Logger log = LoggerFactory.getLogger(HcAuthSecurityConfiguration.class);

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    log.info("setting up HC Auth filter chain");

    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests((authorizedRequests) -> authorizedRequests.anyRequest().permitAll());

    return http.build();
  }
}
