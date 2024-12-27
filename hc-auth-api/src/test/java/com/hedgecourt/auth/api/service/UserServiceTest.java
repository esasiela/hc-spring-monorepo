package com.hedgecourt.auth.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class UserServiceTest {

  @Autowired private UserService userService;

  @Autowired private UserRepository userRepository;

  @Autowired private ScopeRepository scopeRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User alice;
  private User bob;
  private User chuck;
  private User dave;
  private Scope userScope;
  private Scope adminScope;
  private Scope rootScope;

  @BeforeEach
  void setUp() {
    // Create scope objects
    userScope =
        scopeRepository.save(
            Scope.builder().name("user").description("User with basic access").build());
    adminScope =
        scopeRepository.save(
            Scope.builder()
                .name("admin")
                .description("Administrator with elevated privileges")
                .build());
    rootScope =
        scopeRepository.save(
            Scope.builder().name("root").description("Superuser with supreme privileges").build());

    // Create users with different scopes
    alice =
        userRepository.save(
            User.builder()
                .username("alice")
                .firstname("Alice")
                .lastname("Smith")
                .email("alice@example.com")
                .password("password1")
                .scopes(Set.of(userScope))
                .build());
    bob =
        userRepository.save(
            User.builder()
                .username("bob")
                .firstname("Bob")
                .lastname("Johnson")
                .email("bob@example.com")
                .password("password2")
                .scopes(Set.of(adminScope))
                .build());
    chuck =
        userRepository.save(
            User.builder()
                .username("chuck")
                .firstname("Chuck")
                .lastname("Davis")
                .email("chuck@example.com")
                .password("password3")
                .scopes(Set.of(userScope, adminScope))
                .build());
    dave =
        userRepository.save(
            User.builder()
                .username("dave")
                .firstname("Dave")
                .lastname("Wilson")
                .email("dave@example.com")
                .password("password4")
                .scopes(Set.of())
                .build());
  }

  @AfterEach
  void tearDown() {
    // Clean up the database after each test
    userRepository.deleteAll();
    scopeRepository.deleteAll();
  }

  @Test
  void retrieve_ShouldReturnCorrectUserWithScopes_WhenUserExists() {
    // Act
    UserDto retrievedBob = userService.retrieve("bob");

    // Assert
    assertNotNull(retrievedBob, "Bob should not be null.");
    assertEquals("bob", retrievedBob.getUsername(), "Username should match.");
    assertEquals("Bob", retrievedBob.getFirstname(), "Firstname should match.");
    assertEquals("Johnson", retrievedBob.getLastname(), "Lastname should match.");
    assertEquals("bob@example.com", retrievedBob.getEmail(), "Email should match.");

    // Assert that Bob has the 'admin' scope and not 'user'
    assertNotNull(retrievedBob.getScopes(), "Scopes should be non-null.");
    assertEquals(1, retrievedBob.getScopes().size(), "Bob should only have 1 scope.");
    assertTrue(
        retrievedBob.getScopes().contains(adminScope.getName()), "Bob should have 'admin' scope.");
    assertFalse(
        retrievedBob.getScopes().contains(userScope.getName()),
        "Bob should NOT have 'user' scope.");
  }

  @Test
  void retrieve_ShouldReturnUserWithMultipleScopes_WhenUserHasMultipleScopes() {
    // Act
    UserDto retrievedChuck = userService.retrieve("chuck");

    // Assert
    assertNotNull(retrievedChuck, "Chuck should not be null.");
    assertTrue(
        retrievedChuck.getScopes().contains(userScope.getName()),
        "Chuck should have 'user' scope.");
    assertTrue(
        retrievedChuck.getScopes().contains(adminScope.getName()),
        "Chuck should have 'admin' scope.");
    assertEquals(2, retrievedChuck.getScopes().size(), "Chuck should have 2 scopes.");
  }

  @Test
  void retrieve_ShouldReturnUserWithNoScopes_WhenUserHasNoScopes() {
    // Act
    UserDto retrievedDave = userService.retrieve("dave");

    // Assert
    assertNotNull(retrievedDave, "Dave should not be null.");
    assertNotNull(retrievedDave.getScopes(), "Scopes should be non-null.");
    assertEquals(0, retrievedDave.getScopes().size(), "Dave should have no scopes.");
  }

  @Test
  void retrieve_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
    // Act & Assert
    assertThrows(
        UserNotFoundException.class,
        () -> userService.retrieve("nonexistentuser"),
        "Expected UserNotFoundException when the user does not exist.");
  }

  @Test
  void retrieve_ShouldBeCaseSensitive_WhenRetrievingUser() {
    // Act & Assert
    assertNotNull(userService.retrieve("alice"), "User 'alice' should exist.");
    assertThrows(
        UserNotFoundException.class,
        () -> userService.retrieve("Alice"),
        "User lookup should be case-sensitive.");
  }

  @Test
  void list_ShouldReturnAllUsers_WhenUsersExist() {
    // Act
    List<UserDto> userDtolist = userService.list();

    // Assert
    assertNotNull(userDtolist, "The returned user list should not be null.");
    assertEquals(4, userDtolist.size(), "There should be 4 users in the list.");

    // Verify that all users are in the list
    for (User user : List.of(alice, bob, chuck, dave)) {
      // Find UserDto by username in the list
      UserDto retrievedUser =
          userDtolist.stream()
              .filter(u -> u.getUsername().equals(user.getUsername()))
              .findFirst()
              .orElse(null);

      assertNotNull(retrievedUser, "User " + user.getUsername() + " should be in the list.");

      // Check that the fields in UserDto match the corresponding fields in User
      assertEquals(user.getFirstname(), retrievedUser.getFirstname(), "Firstname should match.");
      assertEquals(user.getLastname(), retrievedUser.getLastname(), "Lastname should match.");
      assertEquals(user.getEmail(), retrievedUser.getEmail(), "Email should match.");

      // Check that scopes are the same
      assertEquals(
          user.getScopes().size(),
          retrievedUser.getScopes().size(),
          "Number of scopes should match.");
      for (Scope scope : user.getScopes()) {
        assertTrue(
            retrievedUser.getScopes().contains(scope.getName()),
            "Scope " + scope.getName() + " should be included.");
      }
    }
  }

  @Test
  void list_ShouldReturnAnEmptyList_WhenNoUsersExist() {
    // Arrange: Clear all users
    userRepository.deleteAll();

    // Act
    List<UserDto> users = userService.list();

    // Assert
    assertNotNull(users, "The returned list should not be null.");
    assertTrue(users.isEmpty(), "The returned list should be empty when all users are deleted.");
  }

  @Test
  void list_ShouldIncludeScopesForEachUser_WhenUsersHaveScopes() {
    // Act
    List<UserDto> users = userService.list();

    // Assert
    for (UserDto user : users) {
      assertNotNull(user.getScopes(), "Each user should have a scopes collection.");
    }

    // Additional checks for Alice
    UserDto retrievedAlice =
        users.stream().filter(user -> "alice".equals(user.getUsername())).findFirst().orElse(null);

    assertNotNull(retrievedAlice, "Alice should be present in the user list.");
    assertTrue(
        retrievedAlice.getScopes().contains(userScope.getName()),
        "Alice should have 'user' scope.");
    assertFalse(
        retrievedAlice.getScopes().contains(adminScope.getName()),
        "Alice should NOT have 'admin' scope.");
  }

  @Test
  void create_ShouldSaveNewUser_WhenValidDataIsProvided() {
    // Arrange
    UserCreateDto userDto =
        UserCreateDto.builder()
            .username("john")
            .firstname("John")
            .lastname("Doe")
            .email("john.doe@example.com")
            .plaintextPassword("password123")
            .scopes(Set.of(userScope.getName()))
            .build();

    // Act
    UserDto createdUser = userService.create(userDto);
    // retrieve the new user, so we can validate the encoded password
    User entityUser = userRepository.findById(userDto.getUsername()).orElse(null);

    // Assert
    assertNotNull(createdUser);
    assertEquals("john", createdUser.getUsername());
    assertEquals("John", createdUser.getFirstname());
    assertEquals("Doe", createdUser.getLastname());
    assertEquals("john.doe@example.com", createdUser.getEmail());
    assertTrue(
        createdUser.getScopes().contains(userScope.getName()),
        "Created user should have assigned scope");
    assertFalse(
        createdUser.getScopes().contains(adminScope.getName()),
        "Created user should not have unassigned scope");

    assertNotNull(entityUser, "Retrieved new user must be non-null");
    assertTrue(
        passwordEncoder.matches(userDto.getPlaintextPassword(), entityUser.getPassword()),
        "Encoded password must match plaintext password");
  }

  @Test
  void create_ShouldThrowException_WhenUsernameAlreadyExists() {
    // Arrange
    UserCreateDto dupeUser =
        UserCreateDto.builder()
            .username("alice")
            .firstname("Betsy")
            .lastname("Ross")
            .email("br@flags.com")
            .plaintextPassword("password456")
            .scopes(Set.of(userScope.getName()))
            .build();

    // Act & Assert
    assertThrows(DuplicateUsernameException.class, () -> userService.create(dupeUser));
  }

  @Test
  void create_ShouldThrowException_WhenInvalidScopeAssigned() {
    // Arrange
    UserCreateDto userDto =
        UserCreateDto.builder()
            .username("duke")
            .firstname("Duke")
            .lastname("of Soul")
            .email("dos@ms.com")
            .plaintextPassword("password987")
            .scopes(Set.of("fakeScope"))
            .build();

    // Act & Assert
    assertThrows(InvalidScopeException.class, () -> userService.create(userDto));
  }

  @Test
  void create_ShouldCreateUserWithMultipleScopes_WhenScopesAreProvided() {
    // Arrange
    UserCreateDto userDto =
        UserCreateDto.builder()
            .username("jill")
            .firstname("Jill")
            .lastname("Taylor")
            .email("jill.taylor@example.com")
            .plaintextPassword("password202")
            .scopes(Set.of(userScope.getName(), adminScope.getName()))
            .build();

    // Act
    UserDto createdUser = userService.create(userDto);
    // retrieve the new user, so we can validate the encoded password
    User entityUser = userRepository.findById(userDto.getUsername()).orElse(null);

    // Assert
    assertNotNull(createdUser);
    assertEquals(2, createdUser.getScopes().size());
    assertTrue(
        createdUser.getScopes().contains(userScope.getName()),
        "Created user should have assigned scope: user");
    assertTrue(
        createdUser.getScopes().contains(adminScope.getName()),
        "Created user should have assigned scope: admin");
    assertFalse(
        createdUser.getScopes().contains(rootScope.getName()),
        "Created user should not have unassigned scope: root");

    assertNotNull(entityUser, "Retrieved new user must be non-null");
    assertTrue(
        passwordEncoder.matches(userDto.getPlaintextPassword(), entityUser.getPassword()),
        "Encoded password must match plaintext password");
  }

  @Test
  void update_ShouldUpdateUserDetails_WhenValidDataIsProvided() {
    // Arrange: Update user 'alice'
    UserUpdateDto updateDto =
        UserUpdateDto.builder()
            .firstname("Alice Updated")
            .lastname("Smith Updated")
            .email("alice.updated@example.com")
            .build();

    // Act: Update user
    UserDto updatedUser = userService.update("alice", updateDto);

    // Assert: Ensure the update took place
    assertNotNull(updatedUser, "Updated user should not be null.");
    assertEquals(
        updateDto.getFirstname(), updatedUser.getFirstname(), "Firstname should be updated.");
    assertEquals(updateDto.getLastname(), updatedUser.getLastname(), "Lastname should be updated.");
    assertEquals(updateDto.getEmail(), updatedUser.getEmail(), "Email should be updated.");

    assertTrue(
        updatedUser.getScopes().contains(userScope.getName()),
        "User should have 'user' scope after update.");
    assertFalse(
        Set.of(adminScope.getName(), rootScope.getName()).stream()
            .anyMatch(updatedUser.getScopes()::contains),
        "User should NOT have 'admin' or 'root' scope after update.");
  }

  @Test
  void update_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
    // Arrange: Prepare an update for a non-existent user
    UserUpdateDto updateDto =
        UserUpdateDto.builder()
            .firstname("Nonexistent")
            .lastname("User")
            .email("nonexistent@example.com")
            .build();

    String nonExistentUsername = "nonexistentuser";

    // Act & Assert: Ensure UserNotFoundException is thrown
    assertThrows(
        UserNotFoundException.class,
        () -> userService.update("nonexistentuser", updateDto),
        "Expected UserNotFoundException when trying to update a non-existent user.");

    assertThrows(
        UserNotFoundException.class,
        () -> userService.retrieve(nonExistentUsername),
        "Expected UserNotFoundException to ensure non-existent update did not create user.");
  }

  @Test
  void delete_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
    // Arrange: Prepare to delete a non-existent user
    String username = "nobodyhome";

    // Act & Assert: Ensure UserNotFoundException is thrown
    assertThrows(
        UserNotFoundException.class,
        () -> userService.delete(username),
        "Expected UserNotFoundException when trying to delete a non-existent user.");
  }

  @Test
  void delete_ShouldDeleteAndReturnUser_WhenValidUser() {
    // Arrange: Retrieve the current user for comparison later
    String username = "alice";
    UserDto pre = userService.retrieve(username);

    // Act
    UserDto post = userService.delete(username);

    // Assert
    assertThrows(
        UserNotFoundException.class,
        () -> userService.retrieve(username),
        "Expected UserNotFoundException when trying to retrieve a deleted user.");
    assertEquals(
        pre, post, "The user returned by delete should match the user retrieved before delete.");
  }
}
