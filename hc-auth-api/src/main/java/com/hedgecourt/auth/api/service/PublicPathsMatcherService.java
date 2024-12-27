package com.hedgecourt.auth.api.service;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Service;

@Service
public class PublicPathsMatcherService {

  public RequestMatcher getExcludedPathsMatcher() {
    // Define the paths you want to exclude from the JWT filter
    List<RequestMatcher> paths =
        Arrays.asList(
            new AntPathRequestMatcher("/login"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"));

    return new OrRequestMatcher(paths);
  }
}
