package org.folio.dew.batch.bulkedit.jobs;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.BulkEditProcessorHelper.getMatchPattern;
import static org.folio.dew.utils.BulkEditProcessorHelper.resolveIdentifier;
import static org.folio.dew.utils.Constants.MULTIPLE_MATCHES_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.bulkedit.jobs.processidentifiers.InstanceFetcher;
import org.folio.dew.batch.bulkedit.jobs.processidentifiers.ItemFetcher;
import org.folio.dew.batch.bulkedit.jobs.processidentifiers.UserFetcher;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.InventoryInstancesClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.ExtendedItem;
import org.folio.dew.domain.dto.HoldingsRecord;
import org.folio.dew.domain.dto.HoldingsRecordCollection;
import org.folio.dew.domain.dto.InstanceCollection;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ItemCollection;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserCollection;
import org.folio.dew.error.BulkEditException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

class BulkEditProcessorsTest extends BaseBatchTest {
  @Autowired
  private BulkEditItemProcessor bulkEditItemProcessor;

  @Autowired
  private BulkEditUserProcessor bulkEditUserProcessor;

  @Autowired
  private InstanceFetcher instanceFetcher;
  @MockBean
  private InventoryInstancesClient inventoryInstancesClient;
  @Autowired
  private UserFetcher userFetcher;
  @MockBean
  private UserClient userClient;
  @Autowired
  private ItemFetcher itemFetcher;
  @MockBean
  private InventoryClient inventoryClient;
  @Autowired
  private BulkEditHoldingsProcessor holdingsProcessor;
  @MockBean
  private HoldingClient holdingClient;

  @Test
  @SneakyThrows
  void shouldIgnoreListsWithNullsAndNullObjectsForItems() {
    var item = new ExtendedItem().tenantId("tenant").entity(objectMapper.readValue(Path.of("src/test/resources/upload/item_with_nulls.json").toFile(), Item.class));
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters());
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var itemFormat = bulkEditItemProcessor.process(item);
      assertEquals("0e40884c-3523-4c6d-8187-d578e3d2794e;note;;tenant;0e40884c-3523-4c6d-8187-d578e3d2794e|0e40884c-3523-4c6d-8187-d578e3d2794e;note;false;tenant;0e40884c-3523-4c6d-8187-d578e3d2794e", itemFormat.getNotes());
      assertEquals("check in (staff only) | check in", itemFormat.getCheckInNotes());
      assertEquals("books;be53b4c9-6eb8-4bdf-a785-904cccd04146", itemFormat.getStatisticalCodes());
      assertEquals("hrid;hrid;title|;;", itemFormat.getBoundWithTitles());
      return null;
    });
  }

  @Test
  @SneakyThrows
  void shouldIgnoreListsWithNullsAndNullObjectsForUsers() {
    var user = objectMapper.readValue(Path.of("src/test/resources/upload/user_with_nulls.json").toFile(), User.class);
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters());
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var userFormat = bulkEditUserProcessor.process(user);
      assertEquals(EMPTY, userFormat.getDepartments());
      assertEquals(EMPTY, userFormat.getAddresses());
      assertEquals("TestMultiSelect:", userFormat.getCustomFields());
      System.out.println(userFormat);
      return null;
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"ID", "HRID"})
  @SneakyThrows
  void shouldNotIncludeDuplicatedInstances(String identifierType) {
    when(inventoryInstancesClient.getInstanceByQuery(String.format("%s==duplicateIdentifier", resolveIdentifier(identifierType)), 1)).thenReturn(new InstanceCollection().instances(Collections.emptyList()).totalRecords(2));
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters(Collections.singletonMap("identifierType", new JobParameter<>(identifierType, String.class))));
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var identifier = new ItemIdentifier("duplicateIdentifier");
      var throwable = assertThrows(BulkEditException.class, () -> instanceFetcher.process(identifier));
      assertEquals(MULTIPLE_MATCHES_MESSAGE, throwable.getMessage());
      return null;
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"ID", "HRID", "EXTERNAL_SYSTEM_ID", "USER_NAME"})
  @SneakyThrows
  void shouldNotIncludeDuplicatedUsers(String identifierType) {
    when(userClient.getUserByQuery(String.format("%s==\"duplicateIdentifier\"", resolveIdentifier(identifierType)), 1)).thenReturn(new UserCollection().users(Collections.singletonList(new User())).totalRecords(2));
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters(Collections.singletonMap("identifierType", new JobParameter<>(identifierType, String.class))));
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var identifier = new ItemIdentifier("duplicateIdentifier");
      var throwable = assertThrows(BulkEditException.class, () -> userFetcher.process(identifier));
      assertEquals(MULTIPLE_MATCHES_MESSAGE, throwable.getMessage());
      return null;
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"ID", "HRID", "BARCODE", "FORMER_IDS", "ACCESSION_NUMBER"})
  @SneakyThrows
  void shouldNotIncludeDuplicatedItems(String identifierType) {
    when(inventoryClient.getItemByQuery(String.format(getMatchPattern(identifierType), resolveIdentifier(identifierType), "duplicateIdentifier"), Integer.MAX_VALUE)).thenReturn(new ItemCollection().items(List.of(new Item(), new Item())).totalRecords(2));
    when(inventoryClient.getItemByQuery("barcode==\"duplicateIdentifier\"", Integer.MAX_VALUE)).thenReturn(new ItemCollection().items(List.of(new Item(), new Item())).totalRecords(2));
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters(Collections.singletonMap("identifierType", new JobParameter<>(identifierType, String.class))));
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var identifier = new ItemIdentifier("duplicateIdentifier");
      var throwable = assertThrows(BulkEditException.class, () -> itemFetcher.process(identifier));
      assertEquals(MULTIPLE_MATCHES_MESSAGE, throwable.getMessage());
      return null;
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"ID", "HRID"})
  @SneakyThrows
  void shouldNotIncludeDuplicatedHoldings(String identifierType) {
    when(holdingClient.getHoldingsByQuery(String.format("%s==duplicateIdentifier", resolveIdentifier(identifierType)))).thenReturn(new HoldingsRecordCollection().holdingsRecords(Collections.singletonList(new HoldingsRecord())).totalRecords(2));
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters(Collections.singletonMap("identifierType", new JobParameter<>(identifierType, String.class))));
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var identifier = new ItemIdentifier("duplicateIdentifier");
      var throwable = assertThrows(BulkEditException.class, () -> holdingsProcessor.process(identifier));
      assertEquals(MULTIPLE_MATCHES_MESSAGE, throwable.getMessage());
      return null;
    });
  }
}
