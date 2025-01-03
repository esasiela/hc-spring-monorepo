package com.hedgecourt.auth.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hedgecourt.auth.api.dto.UserCreateDto;
import com.hedgecourt.auth.api.dto.UserUpdateDto;
import com.hedgecourt.auth.api.error.DuplicateUsernameException;
import com.hedgecourt.auth.api.service.UserService;
import com.hedgecourt.spring.lib.dto.UserDto;
import com.hedgecourt.spring.lib.error.UserNotFoundException;
import com.hedgecourt.spring.test.HcSpringBaseControllerTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest extends HcSpringBaseControllerTest {
  private static final Logger log = LoggerFactory.getLogger(UserControllerTest.class);

  @MockBean protected UserService userService;

  @Override
  public Stream<Arguments> getEndpointUseCases() {
    return Stream.of(
        Arguments.of(
            Named.of(
                "Retrieve User",
                new EndpointUseCase(
                    "user:read", HttpMethod.GET, "/users/{username}", null, "testuser"))),
        Arguments.of(
            Named.of(
                "List Users", new EndpointUseCase("user:read", HttpMethod.GET, "/users", null))),
        Arguments.of(
            Named.of(
                "Create User",
                new EndpointUseCase(
                    "user:write",
                    HttpMethod.POST,
                    "/users",
                    UserCreateDto.builder()
                        .username("testuser")
                        .firstname("fname")
                        .lastname("lname")
                        .email("fname.lname@domain.com")
                        .plaintextPassword("security;-)")
                        .scopes(Set.of())
                        .build()))),
        Arguments.of(
            Named.of(
                "Update User",
                new EndpointUseCase(
                    "user:write",
                    HttpMethod.PUT,
                    "/users/{username}",
                    new UserDto("testuser", "fname", "lname", "test@email.com", Set.of()),
                    "testuser"))),
        Arguments.of(
            Named.of(
                "Delete User",
                new EndpointUseCase(
                    "user:write", HttpMethod.DELETE, "/users/{username}", null, "testuser"))));
  }

  @Test
  public void retrieveUser_GivenExistingUser_ThenSuccess() throws Exception {
    authUser.setScopes(Set.of("user:read"));

    UserDto expectedUser =
        new UserDto(
            "testuser", "fname", "lname", "test@email.com", Set.of("scope:one", "scope:two"));

    when(userService.retrieve(expectedUser.getUsername())).thenReturn(expectedUser);

    mockMvc
        .perform(
            get("/users/{username}", expectedUser.getUsername())
                .header("Authorization", "Bearer " + jwtService.generateToken(authUser))
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(expectedUser.getUsername()))
        .andExpect(jsonPath("$.firstname").value(expectedUser.getFirstname()))
        .andExpect(jsonPath("$.lastname").value(expectedUser.getLastname()))
        .andExpect(jsonPath("$.email").value(expectedUser.getEmail()))
        .andExpect(
            jsonPath(
                "$.scopes",
                Matchers.containsInAnyOrder(expectedUser.getScopes().toArray(new String[0]))));
  }

  @Test
  public void retrieveUser_givenUserDoesNotExist_thenError() throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:read");
    String fakeUsername = "fakeuser";

    // Mock the service methods
    when(userService.retrieve(fakeUsername)).thenThrow(new UserNotFoundException(fakeUsername));

    mockMvc
        .perform(
            get("/users/{username}", fakeUsername)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  public void listUsers_givenValidScope_thenSuccess() throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:read");
    List<UserDto> expectedUsers =
        List.of(
            new UserDto(
                "bilbo",
                "Bilbo",
                "Baggins",
                "bbaggins@underhill.com",
                Set.of("user:list", "user:read")),
            new UserDto("frodo", "Frodo", "Baggins", "elijah@wood.net", Set.of("user:read")));

    // Mock the service methods
    when(userService.list()).thenReturn(expectedUsers);

    // Perform the test
    mockMvc
        .perform(
            get("/users")
                .header("Authorization", "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(expectedUsers.size()))
        .andExpect(jsonPath("$[0].username").value(expectedUsers.get(0).getUsername()))
        .andExpect(jsonPath("$[0].firstname").value(expectedUsers.get(0).getFirstname()))
        .andExpect(jsonPath("$[0].lastname").value(expectedUsers.get(0).getLastname()))
        .andExpect(jsonPath("$[0].email").value(expectedUsers.get(0).getEmail()))
        .andExpect(
            jsonPath("$[0].scopes.length()")
                .value(expectedUsers.get(0).getScopes().size())) // Validate scope size
        .andExpect(
            jsonPath(
                "$[0].scopes",
                Matchers.containsInAnyOrder(
                    expectedUsers.get(0).getScopes().toArray(new String[0]))))
        .andExpect(jsonPath("$[1].username").value(expectedUsers.get(1).getUsername()))
        .andExpect(jsonPath("$[1].firstname").value(expectedUsers.get(1).getFirstname()))
        .andExpect(jsonPath("$[1].lastname").value(expectedUsers.get(1).getLastname()))
        .andExpect(jsonPath("$[1].email").value(expectedUsers.get(1).getEmail()))
        .andExpect(
            jsonPath("$[1].scopes.length()")
                .value(expectedUsers.get(1).getScopes().size())) // Validate scope size
        .andExpect(
            jsonPath(
                "$[1].scopes",
                Matchers.containsInAnyOrder(
                    expectedUsers.get(1).getScopes().toArray(new String[0]))));
  }

  @Test
  public void listUsers_givenNoUsers_thenEmptyList() throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:read");

    // Mock the service method to return an empty list
    when(userService.list()).thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            get("/users")
                .header("Authorization", "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("[]")) // Ensure the response body is an empty list
        .andExpect(jsonPath("$.length()").value(0)); // Verify that the list has 0 elements
  }

  @Test
  public void updateUser_givenNonExistingUser_thenError() throws Exception {
    Set<String> authScopes = Set.of("user:write");

    UserUpdateDto updatedUser = new UserUpdateDto("newFirst", "newLast", "new@email.com");

    UserDto expectedUser =
        new UserDto(
            "nobodyHome",
            updatedUser.getFirstname(),
            updatedUser.getLastname(),
            updatedUser.getEmail(),
            Set.of("scope:one", "scope:two"));

    when(userService.update(any(String.class), any(UserUpdateDto.class)))
        .thenThrow(new UserNotFoundException(expectedUser.getUsername()));

    mockMvc
        .perform(
            put("/users/{username}", expectedUser.getUsername())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
        .andDo(print())
        .andExpect(status().isNotFound());

    // ensure we didn't try to create the user
    verify(userService, never()).create(any(UserCreateDto.class));
  }

  @ParameterizedTest
  @CsvSource({
    // Invalid firstname cases
    "'ThisNameIsWayTooLongToBeValidAndExceedsThirtyChars', lastname, valid@email.com",
    // Firstname too long

    // Invalid lastname cases
    "firstname, 'ThisNameIsWayTooLongToBeValidAndExceedsThirtyChars', valid@email.com",
    // Lastname too long

    // Invalid email cases
    "firstname, lastname, ''", // Missing email
    "firstname, lastname, 'invalid-email'", // Invalid email format

    // Combination invalid cases
    "'ThisNameIsWayTooLongToBeValidAndExceedsThirtyChars', 'ThisNameIsWayTooLongToBeValidAndExceedsThirtyChars', 'invalid-email'",
    // All fields invalid
  })
  void updateUser_givenInvalidFieldInput_thenError(String firstname, String lastname, String email)
      throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:write");
    UserUpdateDto userUpdateDto = new UserUpdateDto(firstname, lastname, email);

    // we dont expect the service to run, so we dont expect this user to return
    UserDto unexpectedUser =
        new UserDto(
            "nobodyHome",
            userUpdateDto.getFirstname(),
            userUpdateDto.getLastname(),
            userUpdateDto.getEmail(),
            Set.of("scope:one", "scope:two"));

    // Mock the service methods
    when(userService.update(any(String.class), any(UserUpdateDto.class)))
        .thenReturn(unexpectedUser);

    mockMvc
        .perform(
            put("/users/{username}", unexpectedUser.getUsername())
                .header("Authorization", "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userUpdateDto)))
        .andDo(print())
        .andExpect(status().isBadRequest());

    verify(userService, never()).update(anyString(), any(UserUpdateDto.class));
  }

  @ParameterizedTest
  @CsvSource({
    // Valid cases
    "John, Doe, john.doe@example.com", // typical valid case
    "Alice, Smith, alice.smith@example.com", // another valid case
    "Bob, Brown, bob.brown@example.com", // another valid case

    // Edge cases for first/last names at max length (30 characters)
    "JohnDoeFirstnameIsExactl, Doe, john.doe@example.com", // max length firstname
    "John, DoeIsExactlyThirtyCha, john.doe@example.com", // max length lastname

    // Valid inputs with spaces in names
    "John Paul, Doe, jpd@domain.com", // first name with space
    "John, Paul Doe, jpd@domain.com", // last name with space
    "John Paul, Doe Smith, jpd@domain.com", // both names with spaces
    "12345, Doe, john.doe@example.com",
    "4, 'lname', 'numericfname@lname.com'",

    // Minimal valid inputs (empty strings for names)
    ", , john.doe@example.com", // minimal valid values (empty strings for names)

    // Edge case with extra spaces (leading or trailing)
    " John, Doe, john.doe@domain.com", // leading space in first name
    "John, Doe , john.doe@domain.com" // trailing space in last name
  })
  void updateUser_givenValidInput_thenSuccess(String firstname, String lastname, String email)
      throws Exception {
    Set<String> authScopes = Set.of("user:write");

    UserUpdateDto updatedUser = new UserUpdateDto(firstname, lastname, email);

    UserDto expectedUser =
        new UserDto(
            "iexist",
            updatedUser.getFirstname(),
            updatedUser.getLastname(),
            updatedUser.getEmail(),
            Set.of("scope:one", "scope:two"));

    when(userService.update(eq(expectedUser.getUsername()), any(UserUpdateDto.class)))
        .thenReturn(expectedUser);

    mockMvc
        .perform(
            put("/users/{username}", expectedUser.getUsername())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(expectedUser.getUsername()))
        .andExpect(jsonPath("$.firstname").value(expectedUser.getFirstname()))
        .andExpect(jsonPath("$.lastname").value(expectedUser.getLastname()))
        .andExpect(jsonPath("$.email").value(expectedUser.getEmail()))
        .andExpect(
            jsonPath(
                "$.scopes",
                Matchers.containsInAnyOrder(expectedUser.getScopes().toArray(new String[0]))));
  }

  @Test
  void updateUser_givenExtraFields_ignoresThem() throws Exception {
    // Arrange
    Set<String> authScopes = Set.of("user:write");

    String username = "test123";
    String requestBody =
        """
        {
            "firstname": "John",
            "lastname": "Doe",
            "email": "john.doe@example.com",
            "scopes": ["ADMIN"]
        }
        """;

    // Mock service behavior
    when(userService.update(eq(username), any(UserUpdateDto.class))).thenReturn(null);

    // Act
    mockMvc
        .perform(
            put("/users/{username}", username) // Example endpoint
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());

    // Assert that the service was called with the correct fields only
    ArgumentCaptor<UserUpdateDto> captor = ArgumentCaptor.forClass(UserUpdateDto.class);
    verify(userService).update(eq(username), captor.capture());

    UserUpdateDto capturedDto = captor.getValue();
    assertThat(capturedDto.getFirstname()).isEqualTo("John");
    assertThat(capturedDto.getLastname()).isEqualTo("Doe");
    assertThat(capturedDto.getEmail()).isEqualTo("john.doe@example.com");
    // Scopes field should not exist in the DTO
  }

  @Test
  public void deleteUser_givenExistingUser_thenSuccess() throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:write");
    UserDto expectedUser =
        new UserDto(
            "testuser", "fname", "lname", "test@email.com", Set.of("scope:one", "scope:two"));

    // Mock the service methods
    when(userService.delete(expectedUser.getUsername())).thenReturn(expectedUser);

    mockMvc
        .perform(
            delete("/users/{username}", expectedUser.getUsername())
                .header("Authorization", "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value(expectedUser.getUsername()))
        .andExpect(jsonPath("$.firstname").value(expectedUser.getFirstname()))
        .andExpect(jsonPath("$.lastname").value(expectedUser.getLastname()))
        .andExpect(jsonPath("$.email").value(expectedUser.getEmail()))
        .andExpect(
            jsonPath(
                "$.scopes",
                Matchers.containsInAnyOrder(expectedUser.getScopes().toArray(new String[0]))));
  }

  @Test
  public void deleteUser_givenUserDoesNotExist_thenError() throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:write");
    String fakeUsername = "fakeuser";

    // Mock the service methods
    when(userService.delete(fakeUsername)).thenThrow(new UserNotFoundException(fakeUsername));

    mockMvc
        .perform(
            delete("/users/{username}", fakeUsername)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  public void deleteUser_givenMissingUserParam_thenError() throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:write");
    String fakeUsername = "fakeuser";

    // Mock the service methods
    when(userService.delete(fakeUsername)).thenThrow(new UserNotFoundException(fakeUsername));

    mockMvc
        .perform(
            delete("/users/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @CsvSource({
    // --- Missing Required Fields (null values) ---
    "'null', validPassword, john.doe@example.com, John, Doe", // Username is null
    "validUser, 'null', john.doe@example.com, John, Doe", // Password is null
    "validUser, validPassword, 'null', John, Doe", // Email is null

    // --- Invalid Formats ---
    "validUser, validPassword, invalid-email, John, Doe", // Invalid email format
    "validUser, validPassword, , John, Doe",
    // Empty email (treated as blank)

    // --- Empty Strings ---
    "'', validPassword, john.doe@example.com, John, Doe", // Empty username
    "validUser, '', john.doe@example.com, John, Doe", // Empty password
    "validUser, validPassword, '', John, Doe", // Empty email

    // --- Leading or Trailing Spaces ---
    "' validUser', validPassword, john.doe@example.com, John, Doe", // Leading space in username
    "'validUser ', validPassword, john.doe@example.com, John, Doe", // Trailing space in username
    "validUser, ' validPassword', john.doe@example.com, John, Doe", // Leading space in password
    "validUser, 'validPassword ', john.doe@example.com, John, Doe", // Trailing space in password
    "validUser, validPassword, ' john.doe@example.com', John, Doe", // Leading space in email
    "validUser, validPassword, 'john.doe@example.com ', John, Doe", // Trailing space in email

    // --- Field Length Boundaries ---
    "shrt, validPassword, john.doe@example.com, John, Doe", // Username too short (min 6)
    "thisUsernameIsWayTooLongForValidation, validPassword, john.doe@example.com, John, Doe",
    // Username too long
    "validUser, short, john.doe@example.com, John, Doe", // Password too short (min 6)
    "validUser, thisPasswordIsWayTooLongToBeValidForThisField, john.doe@example.com, John, Doe",
    // Password too long

    // --- Special Characters ---
    "user@name, validPassword, john.doe@example.com, John, Doe", // Invalid '@' in username
    "user#name, validPassword, john.doe@example.com, John, Doe", // Invalid '#' in username
    "user name, validPassword, john.doe@example.com, John, Doe", // Space in username
    "validUser, validPassword, johndoe@ex*mple.com, John, Doe", // Invalid '*' in email
    "validUser, validPassword, john.doe@exam!ple.com, John, Doe" // Invalid '!' in email
  })
  void createUser_givenInvalidFieldInput_thenError(
      String username, String password, String email, String firstname, String lastname)
      throws Exception {
    // Prepare mock data
    Set<String> authScopes = Set.of("user:write");
    UserCreateDto dto =
        UserCreateDto.builder()
            .username("null".equals(username) ? null : username)
            .plaintextPassword("null".equals(password) ? null : password)
            .email("null".equals(email) ? null : email)
            .firstname(firstname)
            .lastname(lastname)
            .build();

    // we dont expect the service to run, so we dont expect this user to return
    UserDto unexpectedUser =
        new UserDto(
            dto.getUsername(), dto.getFirstname(), dto.getLastname(), dto.getEmail(), Set.of());

    // Mock the service methods
    when(userService.create(any(UserCreateDto.class))).thenReturn(unexpectedUser);

    mockMvc
        .perform(
            post("/users")
                .header("Authorization", "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andDo(print())
        .andExpect(status().isBadRequest());

    verify(userService, never()).create(any(UserCreateDto.class));
  }

  @Test
  public void createUser_givenExistingUser_thenError() throws Exception {
    Set<String> authScopes = Set.of("user:write");

    UserCreateDto dto =
        UserCreateDto.builder()
            .username("testuser")
            .plaintextPassword("qwerty90")
            .email("test@domain.com")
            .firstname("George")
            .lastname("Washington")
            .scopes(Set.of("scope:three", "scope:four"))
            .build();

    UserDto expectedUser =
        new UserDto(
            dto.getUsername(),
            dto.getFirstname(),
            dto.getLastname(),
            dto.getEmail(),
            Set.of("scope:one", "scope:two"));

    when(userService.create(any(UserCreateDto.class)))
        .thenThrow(new DuplicateUsernameException(expectedUser.getUsername()));

    mockMvc
        .perform(
            post("/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andDo(print())
        .andExpect(status().isConflict());
  }

  @ParameterizedTest
  @CsvSource({
    // Valid cases
    "john_doe, John, Doe, john.doe@example.com, password123, [scope:read,scope:write]",
    // typical valid case
    "alice_smith, Alice, Smith, alice.smith@example.com, passW0rd!23, [scope:admin,scope:read]",
    // another valid case
    "bob_brown, Bob, Brown, bob.brown@example.com, mypassword_123, [scope:write]",
    // another valid case

    // Edge cases for first/last names at max length (30 characters)
    "john_doe_maxlen, JohnDoeFirstnameIsExactl, Doe, john.doe@example.com, password123, [scope:read]",
    // max length firstname
    "bob_brown_maxlen, John, DoeIsExactlyThirtyCha, john.doe@example.com, password123, [scope:write]",
    // max length lastname

    // Valid inputs with spaces in names
    "john_paul, John Paul, Doe, jpd@domain.com, password123, [scope:read]",
    // first name with space
    "alice_smith_2, John, Paul Doe, jpd@domain.com, password123, [scope:write]",
    // last name with space
    "bob_smith, John Paul, Doe Smith, jpd@domain.com, pass123!, [scope:admin]"
    // both names with spaces
  })
  void createUser_givenValidInput_thenSuccess(
      String username,
      String firstname,
      String lastname,
      String email,
      String password,
      String scopes)
      throws Exception {
    Set<String> authScopes = Set.of("user:write");

    Set<String> scopeSet =
        Arrays.stream(scopes.replaceAll("[\\[\\] ]", "").split(",")).collect(Collectors.toSet());

    UserCreateDto newUser =
        UserCreateDto.builder()
            .username(username)
            .firstname(firstname)
            .lastname(lastname)
            .email(email)
            .plaintextPassword(password)
            .scopes(scopeSet)
            .build();

    UserDto expectedUser = new UserDto(username, firstname, lastname, email, Set.of(scopes));

    when(userService.create(any(UserCreateDto.class))).thenReturn(expectedUser);

    mockMvc
        .perform(
            post("/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUser)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value(expectedUser.getUsername()))
        .andExpect(jsonPath("$.firstname").value(expectedUser.getFirstname()))
        .andExpect(jsonPath("$.lastname").value(expectedUser.getLastname()))
        .andExpect(jsonPath("$.email").value(expectedUser.getEmail()))
        .andExpect(
            jsonPath(
                "$.scopes",
                Matchers.containsInAnyOrder(expectedUser.getScopes().toArray(new String[0]))));
  }
}
