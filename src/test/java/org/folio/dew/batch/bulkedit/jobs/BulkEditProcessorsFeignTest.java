package org.folio.dew.batch.bulkedit.jobs;

import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.PermissionsValidator;
import org.folio.dew.batch.bulkedit.jobs.processidentifiers.ItemFetcher;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.EntityType;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.error.BulkEditException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;

import static org.folio.dew.utils.Constants.CANNOT_GET_RECORD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

class BulkEditProcessorsFeignTest extends BaseBatchTest {

  @Autowired
  private ItemFetcher itemFetcher;
  @Autowired
  private InventoryClient inventoryClient;

  @MockBean
  private PermissionsValidator permissionsValidator;

  @ParameterizedTest
  @ValueSource(strings = {"ID", "HRID", "BARCODE", "FORMER_IDS", "ACCESSION_NUMBER", "HOLDINGS_RECORD_ID"})
  @SneakyThrows
  void shouldThrowErrorWhenItemIsCorrupted(String identifierType) {
    when(permissionsValidator.isBulkEditReadPermissionExists(isA(String.class), eq(EntityType.ITEM))).thenReturn(true);

    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters(Collections.singletonMap("identifierType", new JobParameter<>(identifierType, String.class))));
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var validIdentifier = new ItemIdentifier("validItemIdentifier");
      var actualItemCollection = itemFetcher.process(validIdentifier);
      assertNotNull(actualItemCollection);
      assertThat(actualItemCollection.getExtendedItems(), hasSize(1));
      var corruptedIdentifier = new ItemIdentifier("corruptedItemIdentifier");
      var throwable = assertThrows(BulkEditException.class, () -> itemFetcher.process(corruptedIdentifier));
      assertEquals(CANNOT_GET_RECORD.formatted("http://inventory/items?query=%s%%3D%s%scorruptedItemIdentifier%s&limit=2147483647"
        .formatted(getIdentifierTypeForQuery(identifierType), identifierType.equals("FORMER_IDS") ? "" : "%3D",
          identifierType.equals("BARCODE") ? "%22" : "", identifierType.equals("BARCODE") ? "%22" : ""), "Server Error"), throwable.getMessage());
      return null;
    });
  }

  private static String getIdentifierTypeForQuery(String identifierType) {
    return switch (identifierType) {
      case "ID" -> "id";
      case "HRID" -> "hrid";
      case "BARCODE" -> "barcode";
      case "FORMER_IDS" -> "formerIds";
      case "ACCESSION_NUMBER" -> "accessionNumber";
      case "HOLDINGS_RECORD_ID" -> "holdingsRecordId";
      default -> throw new IllegalStateException("Unexpected value: " + identifierType);
    };
  }
}
