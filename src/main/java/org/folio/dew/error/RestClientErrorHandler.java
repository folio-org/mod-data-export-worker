package org.folio.dew.error;

import java.io.IOException;

import org.folio.spring.exception.NotFoundException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class RestClientErrorHandler {

  public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
    int status = response.getStatusCode().value();

    if (status == 404) {
      handle404(request);
    } else {
      handleOtherError(request, response);
    }
  }

  private void handle404(HttpRequest request) {
    throw new NotFoundException("Not found: " + request.getURI());
  }

  private void handleOtherError(HttpRequest request, ClientHttpResponse response) {
    try (var bodyIs = response.getBody()) {
      var msg = new String(bodyIs.readAllBytes());
      String reason = !msg.isBlank() ? msg : "Unknown error";
      throw new RuntimeException("Error for " + request.getURI() + " because of " + reason);
    } catch (IOException e) {
      throw new RuntimeException("Unable to get reason for error: " + e.getMessage());
    }
  }
}
