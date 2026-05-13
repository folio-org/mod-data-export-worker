package org.folio.dew.batch.acquisitions.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private static final Logger logger = LogManager.getLogger();

  private final UserClient userClient;

  @Cacheable(cacheNames = "users")
  public String getUserName(String userId) {
    if (StringUtils.isBlank(userId)) {
      return "";
    }
    try {
      User user = userClient.getUserById(userId);
      if (user == null) {
        logger.warn("getUserName:: No user found for id '{}'", userId);
        return "";
      }
      Personal personal = user.getPersonal();
      if (personal != null) {
        String name = (StringUtils.defaultString(personal.getFirstName()) + " "
          + StringUtils.defaultString(personal.getLastName())).trim();
        if (StringUtils.isNotBlank(name)) {
          return name;
        }
      }
      return StringUtils.defaultString(user.getUsername());
    } catch (Exception e) {
      logger.warn("getUserName:: Cannot find user by id: '{}'", userId, e);
      return "";
    }
  }
}
