/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.TlsKeyPair;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.SelfSignedCertificateExtension;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import com.linecorp.armeria.testing.junit5.server.mock.MockWebServerExtension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

class SimpleHttpClientTest {

  @RegisterExtension
  public static final MockWebServerExtension server = new MockWebServerExtension();

  @Test
  void testFetchString() {
    server.enqueue(HttpResponse.of("expected result"));

    ImmutableMap<String, String> requestPropertyMap =
        ImmutableMap.of("key1", "value1", "key2", "value2");
    String urlStr = String.format("http://localhost:%s%s", server.httpPort(), "/path");
    SimpleHttpClient httpClient = new SimpleHttpClient();
    String result = httpClient.fetchString("GET", urlStr, requestPropertyMap, null);

    assertThat(result).isEqualTo("expected result");

    AggregatedHttpRequest request1 = server.takeRequest().request();
    assertThat(request1.path()).isEqualTo("/path");
    assertThat(request1.headers().get("key1")).isEqualTo("value1");
    assertThat(request1.headers().get("key2")).isEqualTo("value2");
  }

  @Test
  void testFailedFetchString() {
    ImmutableMap<String, String> requestPropertyMap =
        ImmutableMap.of("key1", "value1", "key2", "value2");
    String urlStr = String.format("http://localhost:%s%s", server.httpPort(), "/path");
    SimpleHttpClient httpClient = new SimpleHttpClient();
    String result = httpClient.fetchString("GET", urlStr, requestPropertyMap, null);
    assertThat(result).isEmpty();
  }

  static class HttpsServerTest {
    @RegisterExtension
    @Order(1)
    public static final SelfSignedCertificateExtension certificate =
        new SelfSignedCertificateExtension();

    @RegisterExtension
    @Order(2)
    public static final ServerExtension server =
        new ServerExtension() {
          @Override
          protected void configure(ServerBuilder sb) {
            sb.tls(TlsKeyPair.of(certificate.privateKeyFile(), certificate.certificateFile()));

            sb.service("/", (ctx, req) -> HttpResponse.of("Thanks for trusting me"));
          }
        };

    @Test
    void goodCert() {
      SimpleHttpClient httpClient = new SimpleHttpClient();
      String result =
          httpClient.fetchString(
              "GET",
              "https://localhost:" + server.httpsPort() + "/",
              Collections.emptyMap(),
              certificate.certificateFile().getAbsolutePath());
      assertThat(result).isEqualTo("Thanks for trusting me");
    }

    @Test
    void missingCert() {
      SimpleHttpClient httpClient = new SimpleHttpClient();
      String result =
          httpClient.fetchString(
              "GET",
              "https://localhost:" + server.httpsPort() + "/",
              Collections.emptyMap(),
              "/foo/bar/bad");
      assertThat(result).isEmpty();
    }

    @Test
    void badCert(@TempDir Path tempDir) throws Exception {
      Path certFile = tempDir.resolve("test.crt");
      Files.write(certFile, "bad cert".getBytes(StandardCharsets.UTF_8));
      SimpleHttpClient httpClient = new SimpleHttpClient();
      String result =
          httpClient.fetchString(
              "GET",
              "https://localhost:" + server.httpsPort() + "/",
              Collections.emptyMap(),
              certFile.toString());
      assertThat(result).isEmpty();
    }
  }
}
