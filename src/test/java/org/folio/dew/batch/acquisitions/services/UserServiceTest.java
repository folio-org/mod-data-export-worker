package org.folio.dew.batch.acquisitions.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Personal;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.templateengine.context.UserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String USER_ID = "7a626480-284e-5b55-9cf2-db32f93956cf";

  @Mock
  private UserClient userClient;

  @InjectMocks
  private UserService userService;

  @Test
  void getUserContext_userWithPersonal_returnsResolvedContext() {
    when(userClient.getUserById(USER_ID)).thenReturn(user("jdoe", "John", "Doe"));

    UserContext context = userService.getUserContext(USER_ID);

    assertThat(context.getId()).isEqualTo(USER_ID);
    assertThat(context.getFirstName()).isEqualTo("John");
    assertThat(context.getLastName()).isEqualTo("Doe");
    assertThat(context.getFullName()).isEqualTo("John Doe");
  }

  @Test
  void getUserContext_blankNames_fullNameFallsBackToUsername() {
    when(userClient.getUserById(USER_ID)).thenReturn(user("jdoe", " ", " "));

    UserContext context = userService.getUserContext(USER_ID);

    assertThat(context.getFullName()).isEqualTo("jdoe");
  }

  @Test
  void getUserContext_nullPersonal_fullNameFallsBackToUsername() {
    User user = new User();
    user.setId(USER_ID);
    user.setUsername("jdoe");
    when(userClient.getUserById(USER_ID)).thenReturn(user);

    UserContext context = userService.getUserContext(USER_ID);

    assertThat(context.getFirstName()).isEmpty();
    assertThat(context.getLastName()).isEmpty();
    assertThat(context.getFullName()).isEqualTo("jdoe");
  }

  @Test
  void getUserContext_nullResponse_returnsEmptyContext() {
    when(userClient.getUserById(USER_ID)).thenReturn(null);

    assertEmpty(userService.getUserContext(USER_ID));
  }

  @Test
  void getUserContext_clientThrowsException_returnsEmptyContext() {
    when(userClient.getUserById(USER_ID)).thenThrow(new RuntimeException("Connection error"));

    assertEmpty(userService.getUserContext(USER_ID));
  }

  private static void assertEmpty(UserContext context) {
    assertThat(context.getId()).isEmpty();
    assertThat(context.getFirstName()).isEmpty();
    assertThat(context.getLastName()).isEmpty();
    assertThat(context.getFullName()).isEmpty();
  }

  private static User user(String username, String firstName, String lastName) {
    Personal personal = new Personal();
    personal.setFirstName(firstName);
    personal.setLastName(lastName);

    User user = new User();
    user.setId(USER_ID);
    user.setUsername(username);
    user.setPersonal(personal);
    return user;
  }
}
