package com.hedgecourt.auth.api.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String> {
  @Query("SELECT u from User u  JOIN u.scopes s WHERE s.name = :scopeName")
  List<User> findAllByScopeName(@Param("scopeName") String scopeName);
}
