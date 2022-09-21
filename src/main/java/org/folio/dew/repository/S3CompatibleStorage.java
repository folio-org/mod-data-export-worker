package org.folio.dew.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface S3CompatibleStorage {
  String upload(String path, String filename) throws IOException;
  void append(String path, InputStream is) throws IOException;
  String write(String path, InputStream is) throws IOException;
  String write(String path, InputStream is, Map<String, String> headers) throws IOException;
  boolean exists(String path);
  InputStream newInputStream(String path) throws IOException;
  byte[] readAllBytes(String path) throws IOException;
}
