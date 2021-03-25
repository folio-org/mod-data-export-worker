package org.folio.dew.batch.bursarfeesfines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.User;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class AccountItemReader implements ItemReader<Account> {

  private final BursarExportService exportService;
  private final ObjectMapper objectMapper;

  @Value("#{jobParameters['bursarFeeFines']}")
  private String bursarFeeFinesStr;

  private StepExecution stepExecution;
  private List<Account> accounts = new ArrayList<>();
  private Map<String, String> userIdMap = new HashMap<>();
  private int nextIndex = 0;

  @Override
  public Account read() {
    ExecutionContext stepContext = stepExecution.getExecutionContext();
    stepContext.put("accounts", accounts);
    stepContext.put("userIdMap", userIdMap);

    Account next = null;
    if (nextIndex < accounts.size()) {
      next = accounts.get(nextIndex);
      nextIndex++;
    } else {
      nextIndex = 0;
    }
    return next;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) throws JsonProcessingException {
    this.stepExecution = stepExecution;

    BursarFeeFines bursarFeeFines = objectMapper.readValue(bursarFeeFinesStr, BursarFeeFines.class);
    stepExecution.getJobExecution().getExecutionContext().put("bursarFeeFines", bursarFeeFines);

    List<User> users = exportService.findUsers(bursarFeeFines.getPatronGroups());
    if (CollectionUtils.isEmpty(users)) {
      throw new IllegalArgumentException(
          String.format("Users not found for patron group(s) %s", StringUtils.join(bursarFeeFines.getPatronGroups(), ",")));
    }

    userIdMap = users.stream()
        .collect(Collectors.toMap(User::getId, user -> Optional.ofNullable(user.getExternalSystemId()).orElse("")));
    accounts = exportService.findAccounts(bursarFeeFines.getDaysOutstanding().longValue(), users);

    stepExecution.getExecutionContext().put("userIdMap", userIdMap);
  }

}
