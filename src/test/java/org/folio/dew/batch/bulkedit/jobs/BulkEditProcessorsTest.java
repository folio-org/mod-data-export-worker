package org.folio.dew.batch.bulkedit.jobs;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
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

  @Test
  @SneakyThrows
  void shouldIgnoreListsWithNullsAndNullObjectsForItems() {
    var item = objectMapper.readValue(Path.of("src/test/resources/upload/item_with_nulls.json").toFile(), Item.class);
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(new JobParameters());
    StepScopeTestUtils.doInStepScope(stepExecution, () -> {
      var itemFormat = bulkEditItemProcessor.process(item);
      assertEquals("name1;name2", itemFormat.getContributorNames());
      assertEquals("0e40884c-3523-4c6d-8187-d578e3d2794e;note;|0e40884c-3523-4c6d-8187-d578e3d2794e;note;false", itemFormat.getNotes());
      assertEquals("id_1;Check in;check in;false;;last;first;|id_2;;check in;;;;;|id_3;Check in;check in;false;source_id;;;", itemFormat.getCirculationNotes());
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
}
