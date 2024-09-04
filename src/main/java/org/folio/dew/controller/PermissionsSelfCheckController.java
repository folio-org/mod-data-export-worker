package org.folio.dew.controller;

import lombok.RequiredArgsConstructor;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.openapitools.api.PermissionsSelfCheckApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class PermissionsSelfCheckController implements PermissionsSelfCheckApi {

  private final FolioExecutionContext folioExecutionContext;

  @Override
  public ResponseEntity<List<String>> getDesiredPermissions() {
    var okapiHeaders = folioExecutionContext.getOkapiHeaders();
    var permissions = new ArrayList<>(okapiHeaders.get(XOkapiHeaders.PERMISSIONS));
    return new ResponseEntity<>(permissions, HttpStatus.OK);
  }
}
