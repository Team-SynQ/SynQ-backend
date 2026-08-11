package com.synq.backend.domain.ai.context.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class MeetingTaskExecutorTest {

	@Test
	void 같은_회의의_작업은_순서를_유지하고_다른_회의는_별도_작업으로_제출한다() {
		QueueExecutor delegate = new QueueExecutor();
		MeetingTaskExecutor executor = new MeetingTaskExecutor(delegate);
		List<String> executionOrder = new ArrayList<>();

		executor.execute(1L, () -> executionOrder.add("meeting-1-first"));
		executor.execute(1L, () -> executionOrder.add("meeting-1-second"));
		executor.execute(2L, () -> executionOrder.add("meeting-2-first"));

		assertThat(delegate.queuedTaskCount()).isEqualTo(2);
		delegate.runNext();
		delegate.runNext();

		assertThat(executionOrder).containsExactly(
				"meeting-1-first", "meeting-1-second", "meeting-2-first");
	}

	private static final class QueueExecutor implements Executor {

		private final Queue<Runnable> tasks = new ArrayDeque<>();

		@Override
		public void execute(Runnable command) {
			tasks.offer(command);
		}

		private int queuedTaskCount() {
			return tasks.size();
		}

		private void runNext() {
			tasks.remove().run();
		}
	}
}
