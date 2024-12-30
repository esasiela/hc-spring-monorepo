package com.hedgecourt.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgecourt.auth.api.dto.JwtAuthenticationErrorResponseDto;
import com.hedgecourt.spring.lib.service.HcJwtService;
import com.hedgecourt.spring.lib.service.HcPublicPathsMatcherService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HcAuthJwtAuthenticationFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(HcAuthJwtAuthenticationFilter.class);

  private final HcJwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final HcPublicPathsMatcherService publicPathsMatcherService;

  @Autowired ObjectMapper objectMapper;

  public HcAuthJwtAuthenticationFilter(
      HcJwtService jwtService,
      UserDetailsService userDetailsService,
      HcPublicPathsMatcherService publicPathsMatcherService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.publicPathsMatcherService = publicPathsMatcherService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (log.isDebugEnabled()) log.debug("doFilterInternal({})", request.getRequestURI());

    if (publicPathsMatcherService.getExcludedPathsMatcher().matches(request)) {
      // Skip JWT authentication if the request matches the excluded paths
      if (log.isDebugEnabled())
        log.debug(
            "request matches the exclude matcher, not applying HcAuth awesome sauce: {}",
            request.getRequestURI());

      filterChain.doFilter(request, response);
      if (log.isDebugEnabled())
        log.debug(
            "after filterChain.doFilter({}) - response {}",
            request.getRequestURI(),
            response.getStatus());
      return;
    }

    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      if (log.isDebugEnabled())
        log.debug("Authorization header is absent or does not begin with 'Bearer '");

      sendJwtErrorResponse(response, "Authorization header is missing or invalid");
      return;
    }

    try {
      final String jwt = authHeader.substring(7);
      final String username = jwtService.extractUsername(jwt);
      if (log.isDebugEnabled()) log.debug("request jwt username=[{}]", username);

      if (username == null) {
        if (log.isErrorEnabled())
          log.error("extracted null username from jwt, not processing filter chain");
        // TODO simulate signed JWTS that lack a username
        return;
      }

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication != null) {
        if (log.isErrorEnabled())
          log.error(
              "valid jwt username [{}] has non-null authentication object in security context [{}], not sure why this occurs but to be safe I'm not processing this request",
              username,
              authentication.getPrincipal());
        return;
      }

      UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
      if (log.isDebugEnabled()) log.debug("userDetails username=[{}]", userDetails.getUsername());

      if (!jwtService.isTokenValid(jwt, userDetails)) {
        if (log.isDebugEnabled()) log.debug("token is invalid");
        sendJwtErrorResponse(response, "Invalid or expired token");
        return;
      }

      if (log.isDebugEnabled())
        log.debug(
            "token is non-expired: username={} authorities={}",
            username,
            userDetails.getAuthorities());

      UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(
              userDetails,
              null, // TODO should credentials be based off of scopes (userdetails.roles)
              userDetails.getAuthorities());

      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authToken);

    } catch (Exception e) {
      if (log.isErrorEnabled()) log.error("error processing jwt", e);
      sendJwtErrorResponse(response, "Invalid or expired token");
      return;
    }

    // we made it here, the JWT is ok so allow the filter chain to continue
    filterChain.doFilter(request, response);
  }

  protected void sendJwtErrorResponse(HttpServletResponse response, String message)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader("WWW-Authenticate", "Bearer");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    response
        .getWriter()
        .write(objectMapper.writeValueAsString(new JwtAuthenticationErrorResponseDto(message)));
    response.getWriter().flush();
  }
}
