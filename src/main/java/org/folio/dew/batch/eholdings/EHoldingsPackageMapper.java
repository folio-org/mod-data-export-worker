package org.folio.dew.batch.eholdings;

import org.folio.de.entity.EHoldingsPackage;
import org.folio.dew.domain.dto.eholdings.EHoldingsPackageDTO;
import org.folio.dew.domain.dto.eholdings.EPackage;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EHoldingsPackageMapper {

  private static final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static EHoldingsPackage convertToEntity(EHoldingsPackageDTO dto) {
    var entity = new EHoldingsPackage();
    try {
      entity.setId(dto.getEPackage().getData().getId());
      entity.setEPackage(objectMapper.writeValueAsString(dto.getEPackage()));
      entity.setAgreements(objectMapper.writeValueAsString(dto.getAgreements()));
      entity.setNotes(objectMapper.writeValueAsString(dto.getNotes()));
    } catch (JsonProcessingException e) {

    }
    return entity;
  }

  public static EHoldingsPackageDTO convertToDTO(EHoldingsPackage entity) {
    var dto = new EHoldingsPackageDTO();
    try {
      dto.setEPackage(objectMapper.readValue(entity.getEPackage(), EPackage.class));
      dto.setNotes(objectMapper.readValue(entity.getNotes(), new TypeReference<>() {
      }));
      dto.setAgreements(
        objectMapper.readValue(entity.getAgreements(), new TypeReference<>() {
        }));
    } catch (JsonProcessingException e) {

    }
    return dto;
  }
}
