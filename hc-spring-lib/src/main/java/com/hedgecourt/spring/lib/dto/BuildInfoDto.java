package com.hedgecourt.spring.lib.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildInfoDto {
  @JsonProperty("hc.env")
  String hcEnv;

  @JsonProperty("spring.application.name")
  String springApplicationName;

  Map<String, Map<String, String>> buildInfo;
}
