package com.hedgecourt.auth.api.model.init;

import com.hedgecourt.auth.api.model.NavItem;
import com.hedgecourt.auth.api.model.NavItemRepository;
import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.model.ScopeRepository;
import com.hedgecourt.auth.api.model.User;
import com.hedgecourt.auth.api.model.UserRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev|prod")
public class InitializeDatabase {

  private static final Logger log = LoggerFactory.getLogger(InitializeDatabase.class);

  @Value("${hc.auth.init.password}")
  private String initPassword;

  @Value("${hc.auth.init.navEnabled:false}")
  private Boolean navEnabled;

  @Bean
  CommandLineRunner initDatabase(
      ScopeRepository scopeRepository,
      NavItemRepository navItemRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    return args -> {
      /*
      Scopes
       */
      Scope superAdminScope =
          scopeRepository.save(
              Scope.builder().name("superadmin").description("Josh Allen #17").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", superAdminScope);

      Scope devReadScope =
          scopeRepository.save(
              Scope.builder().name("dev:read").description("Developer read access").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", devReadScope);

      Scope devWriteScope =
          scopeRepository.save(
              Scope.builder().name("dev:write").description("Developer write access").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", devWriteScope);

      Scope userReadScope =
          scopeRepository.save(
              Scope.builder().name("user:read").description("User list/read access").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", userReadScope);

      Scope userWriteScope =
          scopeRepository.save(
              Scope.builder()
                  .name("user:write")
                  .description("User create/update/delete access")
                  .build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", userWriteScope);

      Scope userPublicScope =
          scopeRepository.save(
              Scope.builder().name("user:public").description("Flag a user as public").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", userPublicScope);

      /*
      Nav
       */
      if (navEnabled) {
        NavItem appsHomeNav =
            navItemRepository.save(
                NavItem.builder()
                    .id(1L)
                    .childOf(null)
                    .title("Apps")
                    .description("HedgeCourt Apps Home")
                    .publicUrl("http://localhost:3000")
                    .path("/")
                    .sortOrder(1)
                    .build());
        if (log.isInfoEnabled()) log.info("Initializing nav: {}", appsHomeNav);

        if (log.isInfoEnabled())
          log.info(
              "Initializing nav: {}",
              navItemRepository.save(
                  NavItem.builder()
                      .id(2L)
                      .childOf(appsHomeNav)
                      .title("Test Page")
                      .description("Apps Test Page")
                      .publicUrl("http://localhost:3000")
                      .path("/test")
                      .sortOrder(2)
                      .build()));

        if (log.isInfoEnabled())
          log.info(
              "Initializing nav: {}",
              navItemRepository.save(
                  NavItem.builder()
                      .id(3L)
                      .childOf(appsHomeNav)
                      .title("Build Info")
                      .description("Apps Build Info")
                      .publicUrl("http://localhost:3000")
                      .path("/build-info")
                      .sortOrder(3)
                      .build()));

        if (log.isInfoEnabled())
          log.info(
              "Initializing nav: {}",
              navItemRepository.save(
                  NavItem.builder()
                      .id(4L)
                      .childOf(appsHomeNav)
                      .title("Nav Info")
                      .description("Apps Nav Info")
                      .publicUrl("http://localhost:3000")
                      .path("/nav-info")
                      .sortOrder(4)
                      .build()));

        NavItem sandboxHomeNav =
            navItemRepository.save(
                NavItem.builder()
                    .id(5L)
                    .childOf(null)
                    .title("Sandbox")
                    .description("HedgeCourt Sandbox Home")
                    .publicUrl("http://localhost:3000/sandbox")
                    .path("/")
                    .sortOrder(100)
                    .build());
        if (log.isInfoEnabled()) log.info("Initializing nav: {}", sandboxHomeNav);

        if (log.isInfoEnabled())
          log.info(
              "Initializing nav: {}",
              navItemRepository.save(
                  NavItem.builder()
                      .id(6L)
                      .childOf(sandboxHomeNav)
                      .title("Funny Page")
                      .description("A funny page in the sandbox")
                      .publicUrl("http://localhost:3000/sandbox")
                      .path("/funny")
                      .sortOrder(101)
                      .build()));
      } else {
        if (log.isInfoEnabled()) log.info("Nav init is disabled, not adding any nav items");
      }

      /*
      Users
       */
      if (log.isInfoEnabled())
        log.info(
            "Preloading {}",
            userRepository.save(
                User.builder()
                    .username("bilbo")
                    .firstname("Bilbo")
                    .lastname("Baggins")
                    .email("bbaggins@shire.io")
                    .password(passwordEncoder.encode(initPassword))
                    .scopes(
                        Set.of(
                            userWriteScope,
                            userReadScope,
                            superAdminScope,
                            devReadScope,
                            devWriteScope))
                    .build()));
      if (log.isInfoEnabled())
        log.info(
            "Preloading {}",
            userRepository.save(
                User.builder()
                    .username("frodo")
                    .firstname("Frodo")
                    .lastname("Baggins")
                    .email("mrunderhill@prancingpony.com")
                    .password(passwordEncoder.encode(initPassword))
                    .scopes(Set.of(userReadScope))
                    .build()));
    };
  }
}
