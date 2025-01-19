package com.hedgecourt.auth.api.model;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NavItemRepository extends JpaRepository<NavItem, Long> {
  @Query("SELECT n FROM NavItem n ORDER BY n.sortOrder ASC")
  List<NavItem> findAllOrderedBySortOrder();
}
