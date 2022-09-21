package org.folio.dew.repository;

import java.io.IOException;
import java.io.InputStream;

public interface S3CompatibleStorage {
  void append(String path, InputStream is) throws IOException;
  String write(String path, InputStream is) throws IOException;
  boolean exists(String path);
  InputStream newInputStream(String path) throws IOException;
  byte[] readAllBytes(String path) throws IOException;
}
