package org.folio.dew.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bulkedit.jobs.permissions.check.DesiredPermissionsUtil;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.openapitools.api.PermissionsSelfCheckApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Log4j2
public class PermissionsSelfCheckController implements PermissionsSelfCheckApi {

  private final FolioExecutionContext folioExecutionContext;

  @Override
  public ResponseEntity<List<String>> getDesiredPermissions() {
    var okapiHeaders = folioExecutionContext.getOkapiHeaders();
    var permissionsAsStr = okapiHeaders.get(XOkapiHeaders.PERMISSIONS).stream().findFirst();
    log.debug("getDesiredPermissions:: {}", permissionsAsStr);
    return permissionsAsStr.map(s -> new ResponseEntity<>(DesiredPermissionsUtil.convertPermissionsToList(s), HttpStatus.OK))
      .orElseGet(() -> new ResponseEntity<>(List.of(), HttpStatus.OK));
  }
}
