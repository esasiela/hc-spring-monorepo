package com.hedgecourt.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavItemDto {
  private Long id;
  private Long childOf;
  private String title;
  private String description;
  private String publicUrl;
  private String path;
  private Integer sortOrder;
}
