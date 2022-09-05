package org.folio.dew.service.validation;

import static java.util.Objects.nonNull;
import static org.folio.dew.domain.dto.UserContentUpdateAction.NameEnum.REPLACE_WITH;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.folio.dew.client.GroupClient;
import org.folio.dew.domain.dto.UserContentUpdate;
import org.folio.dew.error.ContentUpdateValidationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatronGroupUpdateValidator implements ContentUpdateValidator<UserContentUpdate> {
  private final GroupClient groupClient;
  @Override
  public boolean isValid(UserContentUpdate update) {
    String errorMessage = null;
    if (update.getActions().size() != 1) {
      errorMessage = "Patron group update should consist of single REPLACE_WITH action";
    } else {
      var action = update.getActions().get(0);
      if (REPLACE_WITH != action.getName()) {
        errorMessage = action.getName() + " cannot be applied to Patron group";
      } else if (ObjectUtils.isEmpty(action.getValue())) {
        errorMessage = "REPLACE_WITH value cannot be null or empty";
      } else if (groupClient.getGroupByQuery(String.format("group==\"%s\"", action.getValue().toString())).getUsergroups().isEmpty()) {
        errorMessage = "Non-existing patron group: " + action.getValue().toString();
      }
    }
    if (nonNull(errorMessage)) {
      throw new ContentUpdateValidationException(errorMessage);
    }
    return true;
  }
}
