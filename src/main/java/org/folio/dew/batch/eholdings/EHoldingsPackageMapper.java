package org.folio.dew.batch.eholdings;

import org.folio.de.entity.EHoldingsPackage;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackageDTO;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.folio.dew.domain.dto.eholdings.EProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EHoldingsPackageMapper {

  private EHoldingsPackageMapper(){}

  private static final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static EHoldingsPackage convertToEntity(EHoldingsPackageDTO dto) {
    var entity = new EHoldingsPackage();
    try {
      entity.setId(dto.getEPackage().getData().getId());
      entity.setEPackage(objectMapper.writeValueAsString(dto.getEPackage()));
      entity.setEProvider(objectMapper.writeValueAsString(dto.getEProvider()));
      entity.setAgreements(objectMapper.writeValueAsString(dto.getAgreements()));
      entity.setNotes(objectMapper.writeValueAsString(dto.getNotes()));
    } catch (JsonProcessingException e) {
      log.error("An error occurred during parsing of EHoldingsPackageDTO with id: " +
        dto.getEPackage().getData().getId(), e);
    }
    return entity;
  }

  public static EHoldingsPackageDTO convertToDTO(EHoldingsPackage entity) {
    try {
      return EHoldingsPackageDTO.builder()
        .ePackage(objectMapper.readValue(entity.getEPackage(), EPackage.class))
        .eProvider(objectMapper.readValue(entity.getEProvider(), EProvider.class))
        .notes(objectMapper.readValue(entity.getNotes(), new TypeReference<>() {
        }))
        .agreements(objectMapper.readValue(entity.getAgreements(), new TypeReference<>() {
        }))
        .build();
    } catch (JsonProcessingException e) {
      log.error("An error occurred during parsing of EHoldingsPackage with id: " +
        entity.getId(), e);
    }
    return EHoldingsPackageDTO.builder().build();
  }
}
