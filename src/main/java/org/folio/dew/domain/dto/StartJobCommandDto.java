package org.folio.dew.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class StartJobCommandDto extends StartJobRequestDto {

  private UUID id;

}
