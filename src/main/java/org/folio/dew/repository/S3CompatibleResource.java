package org.folio.dew.repository;

import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

@AllArgsConstructor
public class S3CompatibleResource<R extends S3CompatibleStorage> implements WritableResource {

  private String path;
  private R storage;

  @Override
  public OutputStream getOutputStream() throws IOException {
    var out = new ByteArrayOutputStream();
    var bytes = storage.readAllBytes(path);
    out.writeBytes(bytes);
    return out;
  }

  @Override
  public boolean exists() {
    return storage.exists(path);
  }

  @Override
  public URL getURL() {
    return null;
  }

  @Override
  public URI getURI() {
    return null;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public File getFile() {
    return new File(path);
  }

  @Override
  public long contentLength() {
    return 0;
  }

  @Override
  public long lastModified() {
    return 0;
  }

  @Override
  public Resource createRelative(String relativePath) {
    return null;
  }

  @Override
  public String getFilename() {
    return path;
  }

  @Override
  public String getDescription() {
    return "Remote S3 resource";
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return storage.newInputStream(path);
  }
}
