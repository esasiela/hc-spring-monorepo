package com.hedgecourt.spring.lib.service;

import com.hedgecourt.spring.lib.annotation.HcPublicEndpoint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Service
public class HcPublicPathsMatcherService {

  private static final Logger log = LoggerFactory.getLogger(HcPublicPathsMatcherService.class);

  @Value("${hc.public-endpoints.swagger-enabled:true}")
  private boolean publicSwaggerEnabled;

  private final Set<String> publicPaths = new HashSet<>();

  @Autowired
  public HcPublicPathsMatcherService(RequestMappingHandlerMapping handlerMapping) {
    if (log.isDebugEnabled())
      log.debug("HcPublicPathsMatcherService() scanning for @HcPublicEndpoint");

    Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

    handlerMethods.forEach(
        (pathInfo, handlerMethod) -> {
          if (log.isTraceEnabled()) {
            log.trace("inspecting handler method [{}]", handlerMethod.getMethod().getName());
            pathInfo.getDirectPaths().forEach(path -> log.trace("path [{}]", path));
          }

          if (handlerMethod.getMethodAnnotation(HcPublicEndpoint.class) != null) {
            if (log.isDebugEnabled())
              log.debug(
                  "HcPublicEndpoint [{}.{}] [{}]",
                  handlerMethod.getMethod().getDeclaringClass().getName(),
                  handlerMethod.getMethod().getName(),
                  String.join(",", pathInfo.getDirectPaths()));

            if (!pathInfo.getDirectPaths().isEmpty()) {
              publicPaths.addAll(pathInfo.getDirectPaths());
            } else {
              if (log.isWarnEnabled())
                log.warn(
                    "Have public annotation on method [{}.{}] but no direct paths",
                    handlerMethod.getMethod().getDeclaringClass().getName(),
                    handlerMethod.getMethod().getName());
            }
          } else {
            if (log.isTraceEnabled())
              log.trace(
                  "dont have @HcPublicEndpoint annotation [{}]",
                  handlerMethod.getMethod().getName());
          }
        });
  }

  /**
   * Paths matched by the returned RequestMatcher will be excluded from JWT processing, which in
   * effect makes them public endpoints.
   *
   * @return paths to exclude from JWT processing
   */
  public RequestMatcher getExcludedPathsMatcher() {
    if (log.isDebugEnabled()) log.debug("getExcludedPathsMatcher()");

    List<RequestMatcher> paths = new ArrayList<>();

    if (publicSwaggerEnabled) {
      if (log.isDebugEnabled()) log.debug("enabling public swagger endpoints");
      publicPaths.add("/swagger-ui.html");
      publicPaths.add("/swagger-ui/**");
      publicPaths.add("/v3/api-docs/**");
    } else {
      if (log.isDebugEnabled()) log.debug("disabling public swagger endpoints, JWT required");
    }

    if (log.isInfoEnabled()) log.info("Hc Public Paths: {}", publicPaths);

    publicPaths.forEach(path -> paths.add(new AntPathRequestMatcher(path)));

    return new OrRequestMatcher(paths);
  }
}
