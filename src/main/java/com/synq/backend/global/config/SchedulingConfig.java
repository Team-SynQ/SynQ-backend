package com.synq.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 활성화. 현재 사용처는 전사 세그먼트 유휴 flush 안전망(SttIdleFlushScheduler)이다. */
/**
 * SSE 연결의 주기적 하트비트처럼, 서버가 예약 작업을 실행할 수 있게 한다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
