package org.folio.dew.batch.bulkedit.jobs.updatejob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserFormat;
import org.folio.dew.service.UserReferenceService;
import org.json.JSONObject;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BulkEditUpdateRecordsProcessor implements ItemProcessor<UserFormat, User> {

  private static final String ARRAY_DELIMITER = ";";
  private static final String ITEM_DELIMITER = "|";
  private static final String KEY_VALUE_DELIMITER = ":";

  private UserReferenceService userReferenceService;

  @Override
  public User process(UserFormat userFormat) throws Exception {
    User user = new User();
    return null;
  }

//  private User setPersonal(User user, UserFormat userFormat) {
//
//  }
//
//  private User setAddresses(User user, UserFormat userFormat) {
//
//  }

  private User setPlainFields(User user, UserFormat userFormat) {
    user.setId(userFormat.getId());
    user.setUsername(userFormat.getUsername());
    user.setExternalSystemId(userFormat.getExternalSystemId());
    user.setBarcode(userFormat.getBarcode());
    user.setActive(Boolean.valueOf(userFormat.getActive())); //TODO обработать ошибку парсинга, потому что ерунда превратится в false
    user.setType(userFormat.getType());                      //TODO Boolean.parseBoolean("truuueeee") = false
    user.setPatronGroup(getPatronGroupId(userFormat));
    user.setDepartments(getUserDepartments(userFormat));
    user.setProxyFor();
  }

  private String getPatronGroupId(UserFormat userFormat) {
    String patronGroup = userFormat.getPatronGroup();
    JSONObject patronGroupJson = new JSONObject(patronGroup);
    return patronGroupJson.getString("id");
  }

  private List<String> getUserDepartments(UserFormat userFormat) {
    String[] departmentNames = userFormat.getDepartments().split(";");
    return Arrays.stream(departmentNames).parallel()
      .map(name -> userReferenceService.getDepartmentByName(name))
      .map(Department::getId)
      .collect(Collectors.toList());
  }

  private List<String> getProxyFor(UserFormat userFormat) {

  }

}
