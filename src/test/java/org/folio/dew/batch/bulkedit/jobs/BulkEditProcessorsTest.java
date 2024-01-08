package org.folio.dew.batch.bulkedit.jobs;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.Instance;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.User;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

class BulkEditProcessorsTest extends BaseBatchTest {
  @Autowired
  private BulkEditItemProcessor bulkEditItemProcessor;

  @Autowired
  private BulkEditUserProcessor bulkEditUserProcessor;

  @Autowired
  private BulkEditInstanceProcessor bulkEditInstanceProcessor;

  @Test
  @SneakyThrows
  void shouldIgnoreListsWithNullsAndNullObjectsForItems() {
    var item = objectMapper.readValue(Path.of("src/test/resources/upload/item_with_nulls.json").toFile(), Item.class);
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters());
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var itemFormat = bulkEditItemProcessor.process(item);
      assertEquals("name1;name2", itemFormat.getContributorNames());
      assertEquals("0e40884c-3523-4c6d-8187-d578e3d2794e;note;|0e40884c-3523-4c6d-8187-d578e3d2794e;note;false", itemFormat.getNotes());
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
  @Test
  @SneakyThrows
  void processInstance() {
    var instance = objectMapper.readValue(Path.of("src/test/resources/upload/instance.json").toFile(), Instance.class);
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters());
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var instanceFormat = bulkEditInstanceProcessor.process(instance);

      assert instanceFormat != null;
      assertEquals(EMPTY, instanceFormat.getNatureOfContentTermIds());
      assertEquals(EMPTY, instanceFormat.getInstanceFormatIds());
      assertEquals(EMPTY, instanceFormat.getAdministrativeNotes());

      assertEquals("in00000000004", instanceFormat.getHrid());
      assertEquals("Batch Loaded", instanceFormat.getStatusId());
      assertEquals("single unit", instanceFormat.getModeOfIssuanceId());
      assertEquals("unspecified", instanceFormat.getInstanceTypeId());
      System.out.println(instanceFormat);
      return null;
    });
  }
}
