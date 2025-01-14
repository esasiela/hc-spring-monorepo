package com.hedgecourt.spring.lib.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwkDto {
  private String kty;
  private String alg;
  private String use;
  private String kid;
  private String n;
  private String e;
}
