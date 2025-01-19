package com.hedgecourt.auth.api.service;

import com.hedgecourt.auth.api.dto.NavItemDto;
import com.hedgecourt.auth.api.model.NavItem;
import com.hedgecourt.auth.api.model.NavItemRepository;
import com.hedgecourt.auth.api.model.Scope;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NavService {
  private static final Logger log = LoggerFactory.getLogger(NavService.class);

  private final NavItemRepository navItemRepository;

  public NavService(NavItemRepository navItemRepository) {
    this.navItemRepository = navItemRepository;
  }

  private NavItemDto mapToNavItemDto(NavItem navItem) {
    return new NavItemDto(
        navItem.getId(),
        (navItem.getChildOf() == null ? 0 : navItem.getChildOf().getId()),
        navItem.getTitle(),
        navItem.getDescription(),
        navItem.getPublicUrl(),
        navItem.getPath(),
        navItem.getSortOrder());
  }

  /**
   * Returns all NavItems in the repository, regardless of the authorities of the user making the
   * request. This is an admin function.
   *
   * @return all NavItems in the repository
   */
  public List<NavItemDto> list() {
    return navItemRepository.findAll().stream().map(this::mapToNavItemDto).toList();
  }

  /**
   * Returns a list of NavItems the authUser has access to.
   *
   * @param scopes list of Scopes assigned to the authUser
   * @return list of NavItems the authUser has access to
   */
  public List<NavItemDto> listFilteredByAuthorities(List<Scope> scopes) {
    // TODO implement nav item filtering by scopes
    return list();
  }

  @Transactional
  public List<NavItemDto> bulkDelete() {
    if (log.isInfoEnabled()) log.info("bulk deleting all nav items");
    List<NavItemDto> existingItems = list();

    // Break child references for clean deletes (no fk violations)
    navItemRepository
        .findAll()
        .forEach(
            navItem -> {
              navItem.setChildOf(null);
              navItemRepository.save(navItem);
            });

    navItemRepository.deleteAll();

    return existingItems;
  }

  @Transactional
  public List<NavItemDto> bulkAdd(List<NavItemDto> navItemDtos, boolean deleteEnabled) {
    if (log.isInfoEnabled()) log.info("bulk adding nav items");

    if (deleteEnabled) {
      bulkDelete();
    } else {
      List<NavItemDto> existingItems = list();
      if (!existingItems.isEmpty()) {
        throw new IllegalStateException(
            String.format(
                "Cannot bulk add nav items, there are %d existing items.", existingItems.size()));
      }
    }

    // Validate no circular references
    for (NavItemDto dto : navItemDtos) {
      if (dto.getChildOf() != null && dto.getChildOf().equals(dto.getId()))
        throw new IllegalArgumentException(
            String.format("NavItem child cannot reference self: %d", dto.getId()));
    }

    for (NavItemDto dto : navItemDtos) {
      NavItem savedItem =
          navItemRepository.save(
              NavItem.builder()
                  .id(dto.getId())
                  .childOf(
                      dto.getChildOf() == null || dto.getChildOf() == 0
                          ? null
                          : navItemRepository
                              .findById(dto.getChildOf())
                              .orElseThrow(
                                  () ->
                                      new IllegalArgumentException(
                                          String.format(
                                              "Invalid child: id=%d childOf=%d",
                                              dto.getId(), dto.getChildOf()))))
                  .title(dto.getTitle())
                  .description(dto.getDescription())
                  .publicUrl(dto.getPublicUrl())
                  .path(dto.getPath())
                  .sortOrder(dto.getSortOrder())
                  .build());

      if (log.isDebugEnabled()) log.debug("Saved NavItem: {}", savedItem);
    }

    return list();
  }
}
