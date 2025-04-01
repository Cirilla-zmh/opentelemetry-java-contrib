package io.opentelemetry.contrib.messaging.wrappers;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

import javax.annotation.Nullable;
import java.util.List;

public class MessagingProcessWrapper<REQUEST extends MessagingProcessRequest> {

  private static final String INSTRUMENTATION_SCOPE = "messaging-process-wrapper";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private static final String OPERATION_NAME = "process";

  private final TextMapPropagator textMapPropagator;

  private final Tracer tracer;

  private final TextMapGetter<REQUEST> textMapGetter;

  // no attributes need to be extracted from responses in process operations
  private final List<AttributesExtractor<REQUEST, Void>> attributesExtractors;

  public static <REQUEST extends MessagingProcessRequest> DefaultMessagingProcessWrapperBuilder<REQUEST> defaultBuilder() {
    return new DefaultMessagingProcessWrapperBuilder<>();
  }

  public <E extends Throwable> void doProcess(REQUEST request, ThrowingRunnable<E> runnable) throws E {
    Span span = handleStart(request);

    try (Scope scope = span.makeCurrent()) {
      runnable.run();
    } catch (Throwable t) {
      handleEnd(span, request, t);
      throw t;
    }

    handleEnd(span, request, null);
  }

  public <R, E extends Throwable> R doProcess(REQUEST request, ThrowingSupplier<R, E> supplier) throws E {
    Span span = handleStart(request);

    R result = null;
    try (Scope scope = span.makeCurrent()) {
      result = supplier.get();
    } catch (Throwable t) {
      handleEnd(span, request, t);
      throw t;
    }

    handleEnd(span, request, null);
    return result;
  }

  protected Span handleStart(REQUEST request) {
    Context context = this.textMapPropagator.extract(Context.current(), request, this.textMapGetter);
    SpanBuilder spanBuilder = this.tracer.spanBuilder(getDefaultSpanName(request.getDestination()));
    spanBuilder.setParent(context);

    AttributesBuilder builder = Attributes.builder();
    for (AttributesExtractor<REQUEST, Void> extractor : this.attributesExtractors) {
      extractor.onStart(builder, context, request);
    }
    return spanBuilder.setAllAttributes(builder.build()).startSpan();
  }

  protected void handleEnd(Span span, REQUEST request, Throwable t) {
    AttributesBuilder builder = Attributes.builder();
    for (AttributesExtractor<REQUEST, Void> extractor : this.attributesExtractors) {
      extractor.onEnd(builder, Context.current(), request, null, t);
    }
    span.end();
  }

  protected String getDefaultSpanName(String destination) {
    if (destination == null) {
      destination = "unknown";
    }
    return OPERATION_NAME + " " + destination;
  }

  protected MessagingProcessWrapper(OpenTelemetry openTelemetry,
                          @Nullable TextMapGetter<REQUEST> textMapGetter,
                          List<AttributesExtractor<REQUEST, Void>> attributesExtractors) {
    this.textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
    this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE + "-" + INSTRUMENTATION_VERSION);
    this.textMapGetter = textMapGetter;
    this.attributesExtractors = attributesExtractors;
  }
}
