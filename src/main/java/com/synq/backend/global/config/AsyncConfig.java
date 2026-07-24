package com.synq.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
// proxyTargetClass=true: @Async 빈을 CGLIB(클래스) 프록시로 감싼다.
@EnableAsync(proxyTargetClass = true)
public class AsyncConfig {

	/**
	 * 문서 인덱싱 전용 풀
	 * Gemini API 응답 대기용
	 */
	@Bean(name = "indexingExecutor")
	public Executor indexingExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("indexing-");
		executor.initialize();
		return executor;
	}

	@Bean(name = "summaryExecutor")
	public Executor summaryExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("summary-");
		executor.initialize();
		return executor;
	}

	@Bean(name = "liveContextExecutor")
	public Executor liveContextExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		// 확정 전사 순서대로 기존 맥락을 이어야 하므로 병렬 처리하지 않는다.
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("live-context-");
		executor.initialize();
		return executor;
	}
}
