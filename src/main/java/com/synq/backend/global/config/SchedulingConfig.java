package com.synq.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 활성화. 현재 사용처는 전사 세그먼트 유휴 flush 안전망(SttIdleFlushScheduler)이다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
