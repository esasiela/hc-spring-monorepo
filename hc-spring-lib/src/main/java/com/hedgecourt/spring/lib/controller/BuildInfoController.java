package com.hedgecourt.spring.lib.controller;

import com.hedgecourt.spring.lib.error.BuildInfoException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provide JSON formatted information about the build for each of the HC modules included with this
 * project. The purpose is to aid diagnosis of environmental issues. All HC modules are expected to
 * include a META-INF/build-info.[ARTIFACT_ID].properties file with relevant details.
 */
@RestController
@RequestMapping("/build-info")
public class BuildInfoController {

  private final Logger log = LoggerFactory.getLogger(BuildInfoController.class);

  @GetMapping
  public Map<String, Map<String, String>> getBuildInfo() {

    Map<String, Map<String, String>> buildInfoMap = new HashMap<>();

    try {
      Pattern pattern = Pattern.compile("META-INF/build-info\\.(.*)\\.properties");

      ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath*:META-INF/build-info*properties");
      for (Resource resource : resources) {

        Matcher matcher = pattern.matcher(resource.toString());
        if (matcher.find()) {
          String hcModule = matcher.group(1);

          Properties properties = new Properties();
          properties.load(resource.getInputStream());

          Map<String, String> propertiesMap = new HashMap<>();
          for (String key : properties.stringPropertyNames()) {
            propertiesMap.put(key, properties.getProperty(key));
          }

          buildInfoMap.put(hcModule, propertiesMap);
        }
      }
    } catch (IOException e) {
      log.error("Failed scanning for META-INF/build-info.(.*).properties", e);
      throw new BuildInfoException(e);
    }

    return buildInfoMap;
  }
}
