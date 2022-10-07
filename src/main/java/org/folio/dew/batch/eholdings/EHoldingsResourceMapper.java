package org.folio.dew.batch.eholdings;

import java.util.List;

import org.folio.de.entity.EHoldingsResource;
import org.folio.dew.client.AgreementClient;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.Note;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EHoldingsResourceMapper {

  private EHoldingsResourceMapper(){}
  private static final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static EHoldingsResource convertToEntity(EHoldingsResourceDTO dto) {
    var entity = new EHoldingsResource();
    try {
      entity.setId(dto.getResourcesData().getId());
      entity.setResourcesData(objectMapper.writeValueAsString(dto.getResourcesData()));
      entity.setAgreements(objectMapper.writeValueAsString(dto.getAgreements()));
      entity.setNotes(objectMapper.writeValueAsString(dto.getNotes()));
    } catch (JsonProcessingException e) {
      log.error("An error occurred during parsing of EHoldingsResourceDTO with id: " +
        dto.getResourcesData().getId(), e);
    }
    return entity;
  }

  public static EHoldingsResourceDTO convertToDTO(EHoldingsResource entity) {
    try {
      return EHoldingsResourceDTO.builder()
        .resourcesData(objectMapper.readValue(entity.getResourcesData(), ResourcesData.class))
        .notes(objectMapper.readValue(entity.getNotes(), new TypeReference<List<Note>>() {}))
        .agreements(objectMapper.readValue(entity.getAgreements(), new TypeReference<List<AgreementClient.Agreement>>() {}))
        .build();
    } catch (JsonProcessingException e) {
      log.error("An error occurred during parsing of EHoldingsResource with id: " +
        entity.getId(), e);
    }
    return EHoldingsResourceDTO.builder().build();
  }
}
