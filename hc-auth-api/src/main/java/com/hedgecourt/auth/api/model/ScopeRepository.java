package com.hedgecourt.auth.api.model;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScopeRepository extends JpaRepository<Scope, Long> {
  Optional<Scope> findByName(String name);
}
