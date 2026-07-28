package com.synq.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SSE 연결의 주기적 하트비트처럼, 서버가 예약 작업을 실행할 수 있게 한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
