package com.synq.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

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
		// 같은 회의의 순서는 MeetingTaskExecutor가 보장하고, 서로 다른 회의는 병렬 처리한다.
		executor.setCorePoolSize(3);
		executor.setMaxPoolSize(6);
		executor.setQueueCapacity(50);
		// 큐가 포화되어도 전사 반영 작업을 유실하지 않도록 호출 스레드에서 처리한다.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setThreadNamePrefix("live-context-");
		executor.initialize();
		return executor;
	}

	/**
	 * 중요 발화의 참여자별 힌트 생성 전용 풀. Live Context의 회의별 순차 처리를 막지 않는다.
	 */
	@Bean(name = "autoHintExecutor")
	public Executor autoHintExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(3);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(100);
		// 보조 기능이 포화되더라도 Live Context 처리 스레드를 점유하지 않는다.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.setThreadNamePrefix("auto-hint-");
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

	/**
	 * 녹음 세그먼트 S3 업로드 전용 풀. 회의 수만큼만 동시에 발생하므로 크지 않아도 된다.
	 */
	@Bean(name = "recordingExecutor")
	public Executor recordingExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("recording-");
		executor.initialize();
		return executor;
	}
}
