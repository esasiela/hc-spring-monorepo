package com.hedgecourt.auth.api.model.init;

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
public class InitializeDatabaseDevProd {

  private static final Logger log = LoggerFactory.getLogger(InitializeDatabaseDevProd.class);

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
      Scope adminReadScope =
          scopeRepository.save(
              Scope.builder().name("admin:read").description("Admin read access").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", adminReadScope);

      Scope adminWriteScope =
          scopeRepository.save(
              Scope.builder().name("admin:write").description("Admin write access").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", adminWriteScope);

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

      /*
      Users
       */
      User mvpUser =
          userRepository.save(
              User.builder()
                  .username("mvp")
                  .firstname("Josh")
                  .lastname("Allen")
                  .email("number17@buffalobills.com")
                  .password(passwordEncoder.encode(initPassword))
                  .scopes(Set.of(adminReadScope, adminWriteScope, userReadScope, userWriteScope))
                  .build());
      if (log.isInfoEnabled()) log.info("Initializing {}", mvpUser);
    };
  }
}
