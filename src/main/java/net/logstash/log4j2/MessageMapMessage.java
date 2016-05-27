package net.logstash.log4j2;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.message.Message;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Message implementation that combines a String message with a map of fields.
 *
 *
 */
public class MessageMapMessage implements Message{
  private static final Pattern fieldPattern = Pattern.compile("\\{(.+?)\\}");

  private String message;

  private Map<String, Object> fields = new TreeMap<>();

  public MessageMapMessage(String message){
    this.message = message;
  }

  public MessageMapMessage add(String field, Object value){
    this.fields.put(field, value);
    return this;
  }

  @Override
  public String getFormattedMessage() {
    if (StringUtils.isBlank(message)){
      return message;
    }

    StringBuffer sb = new StringBuffer(message.length());
    Matcher m = fieldPattern.matcher(message);
    while (m.find()){
      String found = m.group(1);

      String replacement = null;
      if (fields.containsKey(found)){
        replacement = Objects.toString(fields.get(found));
      } else {
        replacement = "{"+found+"}";
      }

      m.appendReplacement(sb, replacement);
    }
    m.appendTail(sb);

    return sb.toString();
  }

  @Override
  public String getFormat() {
    return message;
  }

  @Override
  public Object[] getParameters() {
    return fields.values().toArray();
  }

  @Override
  public Throwable getThrowable() {
    return null;// TODO
  }

  public Map<String, Object> getFields() {
    return fields;
  }
}
