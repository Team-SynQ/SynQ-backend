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
}
