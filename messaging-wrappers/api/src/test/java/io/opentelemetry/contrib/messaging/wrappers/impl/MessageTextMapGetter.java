package io.opentelemetry.contrib.messaging.wrappers.impl;

import io.opentelemetry.context.propagation.TextMapGetter;

import javax.annotation.Nullable;
import java.util.Collections;

public class MessageTextMapGetter implements TextMapGetter<MessageRequest> {

  public static TextMapGetter<MessageRequest> create() {
    return new MessageTextMapGetter();
  }

  @Override
  public Iterable<String> keys(MessageRequest carrier) {
    if (carrier == null || carrier.getMessage() == null) {
      return Collections.emptyList();
    }
    return carrier.getMessage().getHeaders().keySet();
  }

  @Nullable
  @Override
  public String get(@Nullable MessageRequest carrier, String key) {
    if (carrier == null || carrier.getMessage() == null) {
      return null;
    }
    return carrier.getMessage().getHeaders().get(key);
  }
}
