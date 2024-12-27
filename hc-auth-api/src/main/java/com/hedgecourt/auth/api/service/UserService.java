package com.hedgecourt.auth.api.service;

import com.hedgecourt.auth.api.dto.UserCreateDto;
import com.hedgecourt.auth.api.dto.UserUpdateDto;
import com.hedgecourt.auth.api.error.DuplicateUsernameException;
import com.hedgecourt.auth.api.error.InvalidScopeException;
import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.model.ScopeRepository;
import com.hedgecourt.auth.api.model.User;
import com.hedgecourt.auth.api.model.UserRepository;
import com.hedgecourt.spring.lib.dto.UserDto;
import com.hedgecourt.spring.lib.error.UserNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final ScopeRepository scopeRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(
      UserRepository userRepository,
      ScopeRepository scopeRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.scopeRepository = scopeRepository;
    this.passwordEncoder = passwordEncoder;
  }

  private UserDto mapToUserResponseDto(User user) {
    return new UserDto(
        user.getUsername(),
        user.getFirstname(),
        user.getLastname(),
        user.getEmail(),
        user.getScopes().stream().map(Scope::getName).collect(Collectors.toSet()));
  }

  private Set<Scope> validateUserScopes(Set<String> requestedUserScopes)
      throws InvalidScopeException {
    Set<Scope> validUserScopes =
        scopeRepository.findAll().stream()
            .filter(scope -> requestedUserScopes.contains(scope.getName()))
            .collect(Collectors.toSet());

    Set<String> invalidUserScopes = new HashSet<>(requestedUserScopes);
    invalidUserScopes.removeAll(
        validUserScopes.stream().map(Scope::getName).collect(Collectors.toSet()));
    if (!invalidUserScopes.isEmpty()) {
      throw new InvalidScopeException(invalidUserScopes);
    }

    return validUserScopes;
  }

  public List<UserDto> list() {
    return userRepository.findAll().stream()
        .map(this::mapToUserResponseDto)
        .collect(Collectors.toList());
  }

  public UserDto retrieve(String username) throws UserNotFoundException {
    return mapToUserResponseDto(
        userRepository.findById(username).orElseThrow(() -> new UserNotFoundException(username)));
  }

  /**
   * Data validation is assumed. Business cases for the service method: 1) Duplicate username 2)
   * Scopes are valid
   *
   * @param userDto validated user data
   * @return the user data that was just inserted
   */
  public UserDto create(UserCreateDto userDto) throws DuplicateUsernameException {
    userRepository
        .findById(userDto.getUsername())
        .ifPresent(
            existingUser -> {
              throw new DuplicateUsernameException(userDto.getUsername());
            });

    User user =
        User.builder()
            .username(userDto.getUsername())
            .firstname(userDto.getFirstname())
            .lastname(userDto.getLastname())
            .email(userDto.getEmail())
            .password(passwordEncoder.encode(userDto.getPlaintextPassword()))
            .scopes(validateUserScopes(userDto.getScopes()))
            .build();
    return mapToUserResponseDto(userRepository.save(user));
  }

  public UserDto update(String username, UserUpdateDto userDto) throws UserNotFoundException {

    User u =
        userRepository.findById(username).orElseThrow(() -> new UserNotFoundException(username));

    u.setFirstname(userDto.getFirstname());
    u.setLastname(userDto.getLastname());
    u.setEmail(userDto.getEmail());

    return mapToUserResponseDto(userRepository.save(u));
  }

  public UserDto delete(String username) throws UserNotFoundException {
    User u =
        userRepository.findById(username).orElseThrow(() -> new UserNotFoundException(username));

    userRepository.deleteById(username);
    return mapToUserResponseDto(u);
  }

  // TODO implement modify scopes

  // TODO implement change password

}
