package com.hedgecourt.spring.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class HcSpringSecurityConfiguration {
  private static final Logger log = LoggerFactory.getLogger(HcSpringSecurityConfiguration.class);

  @Bean
  @ConditionalOnMissingBean(SecurityFilterChain.class)
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    log.info("Setting up HC Spring filter chain");

    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests((authorizedRequests) -> authorizedRequests.anyRequest().permitAll());

    return http.build();
  }
}
