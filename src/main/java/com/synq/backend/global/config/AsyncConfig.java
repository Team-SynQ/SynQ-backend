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

	/**
	 * 링크 본문 fetch 전용 풀.
	 * 느린 외부 페이지가 임베딩 대기용 indexingExecutor 를 점유하면 다른 참고자료의 인덱싱이 밀린다.
	 */
	@Bean(name = "linkFetchExecutor")
	public Executor linkFetchExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("link-fetch-");
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

	/**
	 * SSE 응답 쓰기 전용 풀. 느린 클라이언트가 AI 처리 스레드를 점유하지 않게 분리한다.
	 */
	@Bean(name = "sseExecutor")
	public Executor sseExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(16);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("sse-");
		executor.initialize();
		return executor;
	}
}
