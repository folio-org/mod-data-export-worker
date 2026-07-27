package org.folio.dew.batch.acquisitions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.folio.dew.client.ContributorNameTypeClient;
import org.folio.dew.domain.dto.acquisitions.edifact.ContributorNameType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ContributorNameTypeServiceTest {

  @InjectMocks
  private ContributorNameTypeService contributorNameTypeService;
  @Mock
  private ContributorNameTypeClient client;
  @Spy
  private ObjectMapper objectMapper;

  @Test
  void getContributorNameTypeName() {
    String contributorNameTypeName = contributorNameTypeService.getContributorNameTypeName("2b94c631-fca9-4892-a730-03ee529ffe2a");
    assertEquals("", contributorNameTypeName);
  }

  @Test
  void getContributorNameTypeNameFromJson() {
    var contributorNameType = new ContributorNameType();
    contributorNameType.setName("Personal name");
    when(client.getContributorNameType(anyString())).thenReturn(contributorNameType);
    String contributorNameTypeName = contributorNameTypeService.getContributorNameTypeName("2b94c631-fca9-4892-a730-03ee529ffe2a");
    assertEquals("Personal name", contributorNameTypeName);
  }
}
