package org.folio.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class StartJobCommandDto extends StartJobRequestDto {

    private UUID id;
}
