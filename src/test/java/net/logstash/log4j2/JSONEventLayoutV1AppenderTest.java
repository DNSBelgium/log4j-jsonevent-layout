package net.logstash.log4j2;


import junit.framework.Assert;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class JSONEventLayoutV1AppenderTest {
  public static final String PATH_CONFIG_FILE = "FileAppender-JSONEventLayout.xml";
  public static int count = 0;
  public String logFile = null;

  private boolean locationInfo = true;

  @Before
  public void setLoggerConfig() throws IOException {
    logFile = "test-logfile-" + count++ + ".log";
    System.setProperty("org.apache.logging.log4j.level", "DEBUG");
    System.setProperty("log4j.configurationFile", PATH_CONFIG_FILE);
    System.setProperty("log4j.locationInfo", "true");
    System.setProperty("log4j.messageParameters", "false");
    System.setProperty("test.logfile", logFile);

    LoggerContext.getContext(false).reconfigure();
  }

  @After
  public void cleanUp() {
    ThreadContext.clearAll();
    System.clearProperty("org.apache.logging.log4j.level");
    System.clearProperty("log4j.configurationFile");
    System.clearProperty("log4j.messageParameters");
    //FileUtils.deleteQuietly(new File(logFile));
  }

  @Test
  public void testAllLogEventsWithLocationInfo() throws IOException {
    testAllLogEvents();
  }

  @Test
  public void testAllLogEventsWithoutLocationInfo() throws IOException {
    locationInfo = false;
    System.setProperty("log4j.locationInfo", "false");
    LoggerContext loggerContext = LoggerContext.getContext(false);
    loggerContext.reconfigure();

    testAllLogEvents();
  }

  @Test
  public void testLogEventsWithParameters() throws IOException {
    System.setProperty("log4j.messageParameters", "true");

    LoggerContext loggerContext = LoggerContext.getContext(false);
    loggerContext.reconfigure();

    Logger logger = LogManager.getLogger("testLogEventsWithParameters");
    logger.info("This is {} info", "great");
    logger.info("The ultimate answer is {}", 42);
    logger.info("All together. This is {} info.The ultimate answer is {}. Today it is {}", new Object[]{"great", 42, Boolean.TRUE});


    List<String> lines = FileUtils.readLines(new File(logFile), Charset.forName("UTF-8"));
    assertEquals(3, lines.size());

    JSONObject json = (JSONObject) JSONValue.parse(lines.get(0));
    assertProperty(json, "message", "This is great info");
    JSONObject params = (JSONObject) json.get("message_parameters");
    assertProperty(params, "param_0", "great");

    json = (JSONObject) JSONValue.parse(lines.get(1));
    assertProperty(json, "message", "The ultimate answer is 42");
    params = (JSONObject) json.get("message_parameters");
    assertProperty(params, "param_0", Integer.valueOf(42));

    json = (JSONObject) JSONValue.parse(lines.get(2));
    assertProperty(json, "message", "All together. This is great info.The ultimate answer is 42. Today it is true");
    params = (JSONObject) json.get("message_parameters");
    assertProperty(params, "param_0", "great");
    assertProperty(params, "param_1", Integer.valueOf(42));
    assertProperty(params, "param_2", Boolean.TRUE);
  }

  @Test
  public void testLogMessageMapMessage() throws IOException {
    Logger logger = LogManager.getLogger("testLogMessageMapMessage");

    MessageMapMessage message = new MessageMapMessage("All together. This is {type} info.The ultimate answer is {answer}. Today it is {status}");
    message.add("type", "great").add("answer", 42).add("status", Boolean.FALSE);

    logger.info(message);

    List<String> lines = FileUtils.readLines(new File(logFile), Charset.forName("UTF-8"));
    assertEquals(1, lines.size());
    JSONObject json = (JSONObject) JSONValue.parse(lines.get(0));

    assertProperty(json, "message", "All together. This is great info.The ultimate answer is 42. Today it is false");
    JSONObject params = (JSONObject) json.get("message_parameters");
    assertProperty(params, "type", "great");
    assertProperty(params, "answer", Integer.valueOf(42));
    assertProperty(params, "status", Boolean.FALSE);
  }


  public void testAllLogEvents() throws IOException {
    new LoggerHelper().logSomeEvents();
    List<String> lines = FileUtils.readLines(new File(logFile), Charset.forName("UTF-8"));

    for (String line : lines) {
      assertTrue("Should be valid JSON", JSONValue.isValidJsonStrict(line));
      JSONObject json = (JSONObject) JSONValue.parse(line);

      if (locationInfo) {
        assertProperty(json, "file", "LoggerHelper.java");
        assertProperty(json, "method", "logSomeEvents");
      } else {
        assertProperty(json, "file", null);
        assertProperty(json, "method", null);
      }


      assertProperty(json, "thread_name", "main");
      assertProperty(json, "@version", 1);
      assertProperty(json, "logger_name", "com.example.Foo");

      assertTrue("Should contain @timestamp",  json.containsKey("@timestamp"));
      assertTrue("Should contain source_host", json.containsKey("source_host"));
      assertTrue("Should start with {", line.startsWith("{"));
      assertTrue("Should end with }", line.endsWith("}"));
    }

    Assert.assertEquals("Number of log events in the file", 6, lines.size());

    checkLine1 (lines.get(0));
    checkLine2 (lines.get(1));
    checkLine3 (lines.get(2));
    checkLine4 (lines.get(3));
    checkLine5 (lines.get(4));
    checkLine6 (lines.get(5));

  }

  private void checkLine1(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "INFO");
    if (locationInfo) {
      assertProperty(json, "line_number", 13);
    } else {
      assertProperty(json, "line_number", null);
    }
    assertProperty(json, "message", "This is info");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be empty", "{}", mdc.toString());
    assertTrue("MDC should be empty", mdc.isEmpty());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("NDC should be empty", "[]", ndc.toString());
    assertTrue("NDC should be empty", ndc.isEmpty());
  }

  private void checkLine2(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "DEBUG");
    if (locationInfo) {
      assertProperty(json, "line_number", 15);
    } else {
      assertProperty(json, "message", "Some debug message");
    }
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertTrue("MDC should be empty", mdc.isEmpty());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("NDC", "[\"my-ndc\"]", ndc.toString());
    assertTrue("NDC", ndc.contains("my-ndc"));
  }

  private void checkLine3(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "WARN");
    assertProperty(json, "message", "A warning about the bad guys !");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be Foo => Bar", "Bar", mdc.get("Foo"));
  }

  private void checkLine4(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "ERROR");
    assertProperty(json, "message", "Too late: they are here ...");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be Foo => Bar", "Bar", mdc.get("Foo"));
  }

  private void checkLine5(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "FATAL");
    assertProperty(json, "message", "WE ARE DEAD");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be empty", "{}", mdc.toString());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("NDC should be empty", "[]", ndc.toString());
    assertTrue("NDC should be empty", ndc.isEmpty());
  }

  private void checkLine6(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "INFO");
    assertProperty(json, "message", LoggerHelper.ANOTHER_ONE_WITH_QUOTES_AND_DOUBLE_QUOTES_AND_AND_NEWLINES);
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be empty", "{}", mdc.toString());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("foo: {bar=baz, user=mickey}", ndc.get(0));
  }

  private void assertProperty(JSONObject json, String key, Object expected) {
    assertEquals(key, expected, json.get(key));
  }

}
