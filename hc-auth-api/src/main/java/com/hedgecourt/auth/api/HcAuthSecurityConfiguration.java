package com.hedgecourt.auth.api;

import com.hedgecourt.spring.lib.service.HcPublicPathsMatcherService;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class HcAuthSecurityConfiguration {

  private static final Logger log = LoggerFactory.getLogger(HcAuthSecurityConfiguration.class);

  private final UserDetailsService userDetailsService;
  private final HcPublicPathsMatcherService publicPathsMatcherService;
  private final PasswordEncoder passwordEncoder;

  public HcAuthSecurityConfiguration(
      UserDetailsService userDetailsService,
      HcPublicPathsMatcherService publicPathsMatcherService,
      PasswordEncoder passwordEncoder) {
    this.userDetailsService = userDetailsService;
    this.publicPathsMatcherService = publicPathsMatcherService;
    this.passwordEncoder = passwordEncoder;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    if (log.isDebugEnabled())
      log.debug(
          "authenticationManager() {}", config.getAuthenticationManager().getClass().getName());
    return config.getAuthenticationManager();
  }

  @Bean
  AuthenticationProvider authenticationProvider() {
    if (log.isDebugEnabled())
      log.debug(
          "authenticateionProvider() userDetailsService={}",
          userDetailsService.getClass().getName());
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);

    return authProvider;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    if (log.isInfoEnabled()) log.info("setting up HC Auth filter chain");

    if (log.isDebugEnabled())
      log.debug(
          "securityFilterChain() userDetailsService={}", userDetailsService.getClass().getName());

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
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider());

    http.oauth2ResourceServer(
        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    // TODO add custom claims or role mapping here if needed
    if (log.isInfoEnabled())
      log.info("setting up HC Auth jwt authentication converter: {}", converter);
    return converter;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(
        List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:8080",
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083",
            "http://localhost:8084",
            "http://localhost:8085",
            "https://hedgecourt.com",
            "https://www.hedgecourt.com"));
    configuration.setAllowedMethods(List.of("GET", "PATCH", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  /**
   * Help swagger work behind a reverse proxy.
   *
   * @return
   */
  @Bean
  public ForwardedHeaderFilter forwardedHeaderFilter() {
    return new ForwardedHeaderFilter();
  }
}
