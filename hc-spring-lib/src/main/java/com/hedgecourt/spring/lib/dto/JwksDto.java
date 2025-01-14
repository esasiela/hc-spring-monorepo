package com.hedgecourt.spring.lib.dto;

import java.util.Collection;
import java.util.HashSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwksDto {
  private Collection<JwkDto> keys = new HashSet<>();

  public JwksDto addJwk(JwkDto jwk) {
    this.keys.add(jwk);
    return this;
  }
}
