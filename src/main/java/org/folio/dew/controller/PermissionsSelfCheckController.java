package org.folio.dew.controller;

import lombok.RequiredArgsConstructor;
import org.folio.dew.service.UserPermissionsService;
import org.openapitools.api.PermissionsSelfCheckApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bulk-edit")
@RequiredArgsConstructor
public class PermissionsSelfCheckController implements PermissionsSelfCheckApi {

  private final UserPermissionsService userPermissionsService;

  @Override
  public ResponseEntity<List<String>> getUsersPermissions() {
    return new ResponseEntity<>(userPermissionsService.getPermissions(), HttpStatus.OK);
  }
}
