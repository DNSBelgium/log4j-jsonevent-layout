package net.logstash.log4j2;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessageMapMessageTest {
  private MessageMapMessage message;

  @Test
  public void testReplaceOne(){
    message = new MessageMapMessage("today is a {type} day");
    message.add("type", "good");

    String formatted = message.getFormattedMessage();
    assertEquals("today is a good day", formatted);
  }

  @Test
  public void testReplaceSeveral(){
    message = new MessageMapMessage("{begin} today is a {type} day, tomorrow is a {weather} day, after that {end}");
    message.add("type", "good");
    message.add("begin", "Hello");
    message.add("weather", "sunny");
    message.add("end", "it is week-end");

    String formatted = message.getFormattedMessage();
    assertEquals("Hello today is a good day, tomorrow is a sunny day, after that it is week-end", formatted);
  }

  @Test
  public void testNonExisting(){
    message = new MessageMapMessage("this {doesnotexist} and that");
    message.add("exist", "yes");
    assertEquals("this {doesnotexist} and that", message.getFormattedMessage());
  }

  @Test
  public void testMatchStart(){
    message = new MessageMapMessage("{start} in the beginning");
    message.add("start", "early");
    assertEquals("early in the beginning", message.getFormattedMessage());
  }

  @Test
  public void testMatchEnd(){
    message = new MessageMapMessage("in the end {end}");
    message.add("end", "it's over");
    assertEquals("in the end it's over", message.getFormattedMessage());
  }

  @Test
  public void testMatchSameSeveralTimes(){
    message = new MessageMapMessage("we need {needed}, {needed}, {needed} and more {needed}");
    message.add("needed", "developers");

    String expected = "we need developers, developers, developers and more developers";
    assertEquals(expected, message.getFormattedMessage());
  }

  @Test
  public void testMatchNotGreedy(){
    message = new MessageMapMessage("matching should not be too {test} xxxx}");
    message.add("test", "greedy");

    assertEquals("matching should not be too greedy xxxx}", message.getFormattedMessage());
  }
}
