package com.hackathon.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** uvicorn은 h2c 업그레이드 요청을 400으로 거부하므로 ai-server 호출은 HTTP/1.1이어야 한다. */
class AiServerConfigTest {

	@Test
	void callsWithoutHttp2Upgrade() throws Exception {
		AtomicReference<String> upgrade = new AtomicReference<>("not called");
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/v1/analyze", exchange -> {
			upgrade.set(String.valueOf(exchange.getRequestHeaders().getFirst("Upgrade")));
			byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		try {
			var client = new AiServerConfig().aiServerRestClient("http://127.0.0.1:" + server.getAddress().getPort(), "t",
					Duration.ofSeconds(5));

			client.post().uri("/v1/analyze").body("{}").retrieve().toBodilessEntity();

			assertThat(upgrade.get()).isEqualTo("null");
		}
		finally {
			server.stop(0);
		}
	}
}
