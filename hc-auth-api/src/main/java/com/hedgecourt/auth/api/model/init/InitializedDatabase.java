package com.hedgecourt.auth.api.model.init;

import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.model.ScopeRepository;
import com.hedgecourt.auth.api.model.User;
import com.hedgecourt.auth.api.model.UserRepository;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@Profile("dev|prod")
public class InitializedDatabase {

  private static final Logger log = LoggerFactory.getLogger(InitializedDatabase.class);

  @Bean
  CommandLineRunner initDatabase(ScopeRepository scopeRepository, UserRepository userRepository) {
    return args -> {
      Scope superAdminScope =
          scopeRepository.save(
              Scope.builder().name("superadmin").description("Josh Allen #17").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", superAdminScope);

      Scope devReadScope =
          scopeRepository.save(
              Scope.builder().name("developer").description("Developer access").build());
      if (log.isInfoEnabled()) log.info("Initializing scope: {}", devReadScope);

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
      BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

      if (log.isInfoEnabled())
        log.info(
            "Preloading {}",
            userRepository.save(
                User.builder()
                    .username("bilbo")
                    .firstname("Bilbo")
                    .lastname("Baggins")
                    .email("bbaggins@shire.io")
                    .password(passwordEncoder.encode("mypass"))
                    .scopes(Set.of(userWriteScope, userReadScope, superAdminScope, devReadScope))
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
                    .password(passwordEncoder.encode("mypass"))
                    .scopes(Set.of(userReadScope))
                    .build()));
    };
  }
}
