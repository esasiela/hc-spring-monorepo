package com.hedgecourt.auth.api.service;

import com.hedgecourt.auth.api.dto.ScopeCreateDto;
import com.hedgecourt.auth.api.error.DuplicateScopeException;
import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.model.ScopeRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ScopeService {

  private static final Logger log = LoggerFactory.getLogger(ScopeService.class);

  private final ScopeRepository scopeRepository;

  public ScopeService(ScopeRepository scopeRepository) {
    this.scopeRepository = scopeRepository;
  }

  public List<Scope> list() {
    return scopeRepository.findAll();
  }

  public List<Scope> createBulk(List<ScopeCreateDto> scopeDtos) throws DuplicateScopeException {
    try {
      return scopeRepository.saveAll(
          scopeDtos.stream()
              .map(
                  dto ->
                      Scope.builder().name(dto.getName()).description(dto.getDescription()).build())
              .collect(Collectors.toList()));
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateScopeException("One or more scopes already exist.", e);
    }
  }
}
