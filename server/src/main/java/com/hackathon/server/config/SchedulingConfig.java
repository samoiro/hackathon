package com.hackathon.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 배치(ingredient/scheduler)를 켠다. 스레드 설정은 application.yml spring.task.scheduling. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
