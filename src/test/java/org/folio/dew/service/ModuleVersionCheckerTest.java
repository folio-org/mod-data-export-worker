package org.folio.dew.service;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleVersionCheckerTest {

  private static final String APPENDER_NAME = "moduleVersionCheckerTestAppender";

  private LoggerContext loggerContext;
  private LoggerConfig loggerConfig;
  private CapturingAppender appender;

  @BeforeEach
  void setUp() {
    loggerContext = (LoggerContext) LogManager.getContext(false);
    loggerConfig =
        loggerContext.getConfiguration().getLoggerConfig(ModuleVersionChecker.class.getName());
    appender = new CapturingAppender();
    appender.start();
    loggerConfig.addAppender(appender, Level.WARN, null);
    loggerContext.updateLoggers();
  }

  @AfterEach
  void tearDown() {
    loggerConfig.removeAppender(APPENDER_NAME);
    appender.stop();
    loggerContext.updateLoggers();
  }

  @Test
  void check_shouldLogWarning_whenModuleVersionIsMissing() {
    new ModuleVersionChecker(new MockEnvironment()).check();

    assertEquals(1, appender.messages.size());
    assertTrue(appender.messages.getFirst().contains("MODULE_VERSION is not set"));
  }

  @Test
  void check_shouldLogWarning_whenModuleVersionIsBlank() {
    var environment = new MockEnvironment().withProperty("MODULE_VERSION", " ");

    new ModuleVersionChecker(environment).check();

    assertEquals(1, appender.messages.size());
    assertTrue(appender.messages.getFirst().contains("MODULE_VERSION is not set"));
  }

  @Test
  void check_shouldNotLogWarning_whenModuleVersionIsSet() {
    var environment = new MockEnvironment().withProperty("MODULE_VERSION", "1.1.0.123");

    new ModuleVersionChecker(environment).check();

    assertTrue(appender.messages.isEmpty());
  }

  private static class CapturingAppender extends AbstractAppender {

    private final List<String> messages = new CopyOnWriteArrayList<>();

    CapturingAppender() {
      super(APPENDER_NAME, null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
      if (ModuleVersionChecker.class.getName().equals(event.getLoggerName())) {
        messages.add(event.getMessage().getFormattedMessage());
      }
    }
  }
}
