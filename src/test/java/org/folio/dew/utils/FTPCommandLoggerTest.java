package org.folio.dew.utils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FTPCommandLoggerTest {

  Logger log = mock(Logger.class);

  @BeforeEach
  void prepare() {
    doNothing().when(log).debug(anyString());
    doNothing().when(log).info(anyString());
    doNothing().when(log).warn(anyString());
    doNothing().when(log).error(anyString());
  }

  @Test
  void testLogInfo() {
    FTPCommandLogger obj = new FTPCommandLogger(log);
    obj.write('\n');
    verify(log).info(anyString());
  }

  @Test
  void testLogInfoPass() {
    FTPCommandLogger obj = new FTPCommandLogger(log);
    obj.write('P');
    obj.write('A');
    obj.write('S');
    obj.write('S');
    obj.write('T');
    obj.write('E');
    obj.write('S');
    obj.write('T');
    obj.write('\n');
    verify(log).info(anyString());
  }


  @Test
  void testLogInfoSkipp() {
    FTPCommandLogger obj = new FTPCommandLogger(log);
    obj.write('x');
    verify(log, never()).info(anyString());
  }

  @Test
  void testPassLogInfo() {
    FTPCommandLogger obj = new FTPCommandLogger(log);
    obj.write("PASS".getBytes(), 0, "PASS".length() - 1);
    verify(log, never()).info(anyString());
  }
}
