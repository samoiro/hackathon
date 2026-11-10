package com.hackathon.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Samoiro AI Hackathon API")
						.description("해커톤 백엔드 API 명세서")
						.version("v1"))
				// 상대 경로로 두면 로컬/배포 어디서든 Swagger UI의 "Try it out"이 현재 도메인으로 호출됩니다.
				.addServersItem(new Server().url("/"));
	}
}
