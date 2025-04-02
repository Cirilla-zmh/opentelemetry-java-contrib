package io.opentelemetry.contrib.messaging.wrappers.semconv;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * An interface to expose messaging properties for the pre-defined process wrapper.
 *
 * <p>Inspired from <a href=https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation-api-incubator/src/main/java/io/opentelemetry/instrumentation/api/incubator/semconv/messaging/MessagingAttributesGetter.java>MessagingAttributesGetter</a>.
 */
public interface MessagingProcessRequest {

  String getSystem();

  @Nullable
  String getDestination();

  @Nullable
  String getDestinationTemplate();

  boolean isTemporaryDestination();

  boolean isAnonymousDestination();

  @Nullable
  String getConversationId();

  @Nullable
  Long getMessageBodySize();

  @Nullable
  Long getMessageEnvelopeSize();

  @Nullable
  String getMessageId();

  @Nullable
  default String getClientId() {
    return null;
  }

  @Nullable
  default Long getBatchMessageCount() {
    return null;
  }

  @Nullable
  default String getDestinationPartitionId() {
    return null;
  }

  /**
   * Extracts all values of header named {@code name} from the request, or an empty list if there
   * were none.
   *
   * <p>Implementations of this method <b>must not</b> return a null value; an empty list should be
   * returned instead.
   */
  default List<String> getMessageHeader(String name) {
    return emptyList();
  }
}
