package com.synq.backend.domain.ai.context.application;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * meetingId별 실행 순서를 유지하면서, 서로 다른 회의의 작업은 공용 스레드 풀에서 병렬 처리한다.
 */
@Component
public class MeetingTaskExecutor {

	private final Executor delegate;
	private final Map<Long, SerialQueue> queues = new HashMap<>();

	public MeetingTaskExecutor(@Qualifier("liveContextExecutor") Executor delegate) {
		this.delegate = delegate;
	}

	public void execute(Long meetingId, Runnable task) {
		synchronized (queues) {
			SerialQueue queue = queues.computeIfAbsent(meetingId, SerialQueue::new);
			queue.enqueue(task);
		}
	}

	private final class SerialQueue {

		private final Long meetingId;
		private final Queue<Runnable> tasks = new ArrayDeque<>();
		private boolean running;

		private SerialQueue(Long meetingId) {
			this.meetingId = meetingId;
		}

		private synchronized void enqueue(Runnable task) {
			tasks.offer(task);
			if (running) {
				return;
			}
			running = true;
			try {
				delegate.execute(this::drain);
			} catch (RejectedExecutionException exception) {
				running = false;
				tasks.remove(task);
				throw exception;
			}
		}

		private void drain() {
			while (true) {
				Runnable task = poll();
				if (task == null) {
					removeWhenIdle();
					return;
				}

				try {
					task.run();
				} catch (RuntimeException ignored) {
					// 개별 작업 실패는 호출부가 기록하고, 다음 회의 작업은 계속 처리한다.
				}
			}
		}

		private synchronized Runnable poll() {
			Runnable task = tasks.poll();
			if (task == null) {
				running = false;
			}
			return task;
		}

		private void removeWhenIdle() {
			synchronized (queues) {
				if (queues.get(meetingId) == this && isIdle()) {
					queues.remove(meetingId);
				}
			}
		}

		private synchronized boolean isIdle() {
			return !running && tasks.isEmpty();
		}
	}
}
