package com.hedgecourt.auth.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hedgecourt.auth.api.dto.UserCreateDto;
import com.hedgecourt.auth.api.service.UserService;
import com.hedgecourt.spring.lib.dto.UserDto;
import com.hedgecourt.spring.test.HcSpringBaseControllerTest;
import java.util.Set;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest extends HcSpringBaseControllerTest {
  private static final Logger log = LoggerFactory.getLogger(UserControllerTest.class);

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

  @MockBean protected UserService userService;

  @Test
  public void retrieveUserGivenExistingUserThenSuccess() throws Exception {
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
}
