package com.hedgecourt.spring.test;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgecourt.spring.lib.model.HcUserDetails;
import com.hedgecourt.spring.lib.service.HcJwtService;
import com.hedgecourt.spring.lib.service.HcJwtService.JwtPart;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class HcSpringBaseControllerTest {
  private static final Logger log = LoggerFactory.getLogger(HcSpringBaseControllerTest.class);

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected HcJwtService jwtService;

  @MockBean protected UserDetailsService userDetailsService;

  public record EndpointUseCase(
      String requiredScope,
      HttpMethod httpMethod,
      String endpointUri,
      Object bodyBean,
      Object... uriVariables) {

    @Override
    public String toString() {
      return String.format(
          "requiredScope=%s, httpMethod=%s, endpointUri=%s, uriVariables=%s",
          requiredScope, httpMethod, endpointUri, Arrays.toString(uriVariables));
    }
  }

  /**
   * Implement this method to provide a stream of EndpointUseCase records to run.
   *
   * @return stream of EndpointUseCase records
   */
  public abstract Stream<Arguments> getEndpointUseCases();

  protected HcUserDetails authUser = new HcUserDetails("authuser", Collections.emptySet());

  protected final Map<HttpMethod, BiFunction<String, Object[], RequestBuilder>>
      methodToRequestBuilder =
          Map.of(
              HttpMethod.GET,
              MockMvcRequestBuilders::get,
              HttpMethod.POST,
              MockMvcRequestBuilders::post,
              HttpMethod.PUT,
              MockMvcRequestBuilders::put,
              HttpMethod.DELETE,
              MockMvcRequestBuilders::delete,
              HttpMethod.PATCH,
              MockMvcRequestBuilders::patch);

  public String generateJwt(Set<String> authorities) {
    authUser.setScopes(authorities);
    return jwtService.generateToken(authUser);
  }

  protected String generateCorruptJwt(JwtPart jwtPart) {
    return generateCorruptJwt(jwtPart, Set.of("itjustdoesnt:matter"));
  }

  private String generateCorruptJwt(JwtPart jwtPart, Set<String> authScopes) {
    String validJwt = generateJwt(authScopes);
    String[] parts = validJwt.split("\\.");
    String part = parts[jwtPart.ordinal()];

    if (jwtPart == JwtPart.HEADER || jwtPart == JwtPart.PAYLOAD) {
      try {
        Map<String, Object> partMap =
            objectMapper.readValue(
                new String(Base64.getUrlDecoder().decode(part)),
                new TypeReference<Map<String, Object>>() {});
        partMap.put("corrupt", "value");
        parts[jwtPart.ordinal()] =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsString(partMap).getBytes());
      } catch (IOException e) {
        if (log.isErrorEnabled()) log.error("Error corrupting Jwt {}", jwtPart);
        throw new RuntimeException(e);
      }
    } else {
      int idxToTamper = 4;
      if (part.length() < idxToTamper + 1) {
        throw new RuntimeException(
            "I call shenanigans on a JWT part shorter than " + (idxToTamper + 1) + " chars");
      }

      char untamperedChar = part.charAt(part.length() - idxToTamper);
      String tamperedPart =
          part.substring(0, part.length() - idxToTamper)
              + (untamperedChar != 'X' ? 'X' : 'Z')
              + part.substring(part.length() - (idxToTamper - 1));

      parts[jwtPart.ordinal()] = tamperedPart;
    }
    return String.join(".", parts);
  }

  @BeforeEach
  public void beforeEach() {
    // RECALL: child beforeEach runs first, parent beforeEach runs next
    when(userDetailsService.loadUserByUsername(authUser.getUsername())).thenReturn(authUser);
  }

  protected MockHttpServletRequestBuilder getAndApplyMockRequest(
      HttpMethod httpMethod, String endpoint, Object... uriVariables) {
    BiFunction<String, Object[], RequestBuilder> requestBuilder =
        methodToRequestBuilder.get(httpMethod);

    if (requestBuilder == null) {
      throw new IllegalArgumentException("Test suite does not support HTTP method: " + httpMethod);
    }

    return (MockHttpServletRequestBuilder) requestBuilder.apply(endpoint, uriVariables);
  }

  @ParameterizedTest
  @MethodSource("getEndpointUseCases")
  void invalidJwtAbsentAuthorizationHeader(EndpointUseCase useCase) throws Exception {
    if (log.isDebugEnabled()) log.debug(String.valueOf(useCase));

    mockMvc
        .perform(
            getAndApplyMockRequest(useCase.httpMethod, useCase.endpointUri, useCase.uriVariables))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("getEndpointUseCases")
  void invalidJwt_AbsentBearerPrefix(EndpointUseCase useCase) throws Exception {
    if (log.isDebugEnabled()) log.debug(String.valueOf(useCase));

    Set<String> authScopes = Set.of(useCase.requiredScope);
    mockMvc
        .perform(
            getAndApplyMockRequest(useCase.httpMethod, useCase.endpointUri, useCase.uriVariables)
                .header(HttpHeaders.AUTHORIZATION, generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("getEndpointUseCases")
  void invalidJwt_InvalidBearerPrefix(EndpointUseCase useCase) throws Exception {
    if (log.isDebugEnabled()) log.debug(String.valueOf(useCase));

    Set<String> authScopes = Set.of(useCase.requiredScope);
    mockMvc
        .perform(
            getAndApplyMockRequest(useCase.httpMethod, useCase.endpointUri, useCase.uriVariables)
                .header(HttpHeaders.AUTHORIZATION, "Xearer " + generateJwt(authScopes))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("getEndpointUseCases")
  void invalidJwt_tamperedJwtHeader(EndpointUseCase useCase) throws Exception {
    if (log.isDebugEnabled()) log.debug(String.valueOf(useCase));

    mockMvc
        .perform(
            getAndApplyMockRequest(useCase.httpMethod, useCase.endpointUri, useCase.uriVariables)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateCorruptJwt(JwtPart.HEADER))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("getEndpointUseCases")
  void invalidJwt_tamperedJwtPayload(EndpointUseCase useCase) throws Exception {
    if (log.isDebugEnabled()) log.debug(String.valueOf(useCase));

    mockMvc
        .perform(
            getAndApplyMockRequest(useCase.httpMethod, useCase.endpointUri, useCase.uriVariables)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateCorruptJwt(JwtPart.PAYLOAD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("getEndpointUseCases")
  void invalidJwt_tamperedJwtSignature(EndpointUseCase useCase) throws Exception {
    if (log.isDebugEnabled()) log.debug(String.valueOf(useCase));

    mockMvc
        .perform(
            getAndApplyMockRequest(useCase.httpMethod, useCase.endpointUri, useCase.uriVariables)
                .header(
                    HttpHeaders.AUTHORIZATION, "Bearer " + generateCorruptJwt(JwtPart.SIGNATURE))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("getEndpointUseCases")
  void invalidJwt_InsufficientAuthority(EndpointUseCase useCase) throws Exception {
    if (log.isDebugEnabled()) log.debug(String.valueOf(useCase));

    Set<String> authScopes = Set.of("insufficient:authority");
    MockHttpServletRequestBuilder request =
        getAndApplyMockRequest(useCase.httpMethod, useCase.endpointUri, useCase.uriVariables)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateJwt(authScopes))
            .contentType(MediaType.APPLICATION_JSON);

    if (useCase.bodyBean != null) {
      request.content(objectMapper.writeValueAsString(useCase.bodyBean));
    }

    mockMvc.perform(request).andExpect(status().isForbidden());
  }
}
