package com.hedgecourt.auth.api.model.init;

import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.model.ScopeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev|prod")
public class InitializedDatabase {

  private static final Logger log = LoggerFactory.getLogger(InitializedDatabase.class);

  @Bean
  CommandLineRunner initDatabase(ScopeRepository scopeRepository) {
    Scope superAdminScope =
        scopeRepository.save(
            Scope.builder().name("superadmin").description("Josh Allen #17").build());
    log.info("Initializing scope: {}", superAdminScope);

    Scope devReadScope =
        scopeRepository.save(
            Scope.builder().name("developer").description("Developer access").build());
    log.info("Initializing scope: {}", devReadScope);

    return args -> {
      Scope userReadScope =
          scopeRepository.save(
              Scope.builder().name("user:read").description("User list/read access").build());
      log.info("Initializing scope: {}", userReadScope);

      Scope userWriteScope =
          scopeRepository.save(
              Scope.builder()
                  .name("user:write")
                  .description("User create/update/delete access")
                  .build());
      log.info("Initializing scope: {}", userWriteScope);
    };
  }
}
