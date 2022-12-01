package org.folio.dew.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.PresignedUrl;
import org.folio.dew.repository.RemoteFilesStorage;
import org.openapitools.api.RefreshPresignedUrlApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Log4j2
@RequiredArgsConstructor
public class PresignedUrlController implements RefreshPresignedUrlApi {

  private final RemoteFilesStorage remoteFilesStorage;

  @Override
  public ResponseEntity<PresignedUrl> getRefreshedPresignedUrl(String filePath) {
    PresignedUrl presignedUrl = remoteFilesStorage.refreshPresignedUrl(filePath);
    return new ResponseEntity<>(presignedUrl, HttpStatus.OK);
  }
}
