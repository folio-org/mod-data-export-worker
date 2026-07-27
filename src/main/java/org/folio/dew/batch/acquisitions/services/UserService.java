package org.folio.dew.batch.acquisitions.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.templateengine.context.UserContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserService {

  private final UserClient userClient;

  @Cacheable(cacheNames = "users")
  public UserContext getUserContext(String userId) {
    if (StringUtils.isBlank(userId)) {
      return emptyUserContext();
    }
    try {
      User user = userClient.getUserById(userId);
      if (user == null) {
        log.warn("getUserContext:: No user found for id '{}'", userId);
        return emptyUserContext();
      }
      return toUserContext(user);
    } catch (Exception e) {
      log.warn("getUserContext:: Cannot find user by id: '{}'", userId, e);
      return emptyUserContext();
    }
  }

  private UserContext toUserContext(User user) {
    Personal personal = user.getPersonal();
    String firstName = personal != null ? StringUtils.defaultString(personal.getFirstName()) : "";
    String lastName = personal != null ? StringUtils.defaultString(personal.getLastName()) : "";
    String fullName = (firstName + " " + lastName).trim();
    if (StringUtils.isBlank(fullName)) {
      fullName = StringUtils.defaultString(user.getUsername());
    }
    return UserContext.builder()
      .id(StringUtils.defaultString(user.getId()))
      .firstName(firstName)
      .lastName(lastName)
      .fullName(fullName)
      .build();
  }

  private UserContext emptyUserContext() {
    return UserContext.builder()
      .id("")
      .firstName("")
      .lastName("")
      .fullName("")
      .build();
  }
}
