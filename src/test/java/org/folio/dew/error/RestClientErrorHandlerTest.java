package org.folio.dew.error;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.folio.dew.CopilotGenerated;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

@CopilotGenerated
@ExtendWith(MockitoExtension.class)
class RestClientErrorHandlerTest {

  @InjectMocks
  private RestClientErrorHandler handler;

  @Mock
  private HttpRequest request;

  @Mock
  private ClientHttpResponse response;

  @Test
  void handle_404_throwsNotFoundException() throws IOException {
    when(request.getURI()).thenReturn(URI.create("http://localhost/some-resource"));
    when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);

    var ex = assertThrows(NotFoundException.class, () -> handler.handle(request, response));

    assertTrue(ex.getMessage().contains("http://localhost/some-resource"));
  }

  @Test
  void handle_500_withBody_throwsRuntimeExceptionWithBody() throws IOException {
    String errorBody = "Internal Server Error details";
    when(request.getURI()).thenReturn(URI.create("http://localhost/some-resource"));
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(errorBody.getBytes()));

    var ex = assertThrows(RuntimeException.class, () -> handler.handle(request, response));

    assertTrue(ex.getMessage().contains("http://localhost/some-resource"));
    assertTrue(ex.getMessage().contains(errorBody));
  }

  @Test
  void handle_500_withEmptyBody_throwsRuntimeExceptionWithUnknownError() throws IOException {
    when(request.getURI()).thenReturn(URI.create("http://localhost/some-resource"));
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));

    var ex = assertThrows(RuntimeException.class, () -> handler.handle(request, response));

    assertTrue(ex.getMessage().contains("Unknown error"));
  }

  @Test
  void handle_500_whenGetBodyThrowsIOException_throwsRuntimeExceptionWithReason() throws IOException {
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(response.getBody()).thenThrow(new IOException("stream error"));

    var ex = assertThrows(RuntimeException.class, () -> handler.handle(request, response));

    assertTrue(ex.getMessage().contains("Unable to get reason for error"));
    assertTrue(ex.getMessage().contains("stream error"));
  }
}

