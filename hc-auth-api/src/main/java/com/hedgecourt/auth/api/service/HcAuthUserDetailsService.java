package com.hedgecourt.auth.api.service;

import com.hedgecourt.auth.api.model.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Primary
public class HcAuthUserDetailsService implements UserDetailsService {
  private static final Logger log = LoggerFactory.getLogger(HcAuthUserDetailsService.class);

  private final UserRepository userRepository;

  public HcAuthUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (log.isDebugEnabled()) log.debug("loadUserByUsername({})", username);
    return userRepository
        .findById(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }
}
