package org.folio.dew.repository;

import java.io.IOException;
import java.io.InputStream;

public interface S3CompatibleStorage {
  String write(String path, byte[] bytes) throws IOException;
  boolean exists(String path);
  InputStream newInputStream(String path) throws IOException;
  byte[] readAllBytes(String path) throws IOException;
}
