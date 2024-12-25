package com.hedgecourt.auth.api.service;

import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.model.ScopeRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
}
