package com.synq.backend.domain.ai.event.api;

import com.synq.backend.domain.ai.event.AiEventSubscriptionService;
import com.synq.backend.domain.auth.jwt.CurrentUserIdResolver;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/meetings/{meetingId}/ai-events")
public class AiEventController implements AiEventControllerDocs {

	private final AiEventSubscriptionService subscriptionService;
	private final CurrentUserIdResolver currentUserIdResolver;

	public AiEventController(
			AiEventSubscriptionService subscriptionService,
			CurrentUserIdResolver currentUserIdResolver
	) {
		this.subscriptionService = subscriptionService;
		this.currentUserIdResolver = currentUserIdResolver;
	}

	@Override
	public ResponseEntity<SseEmitter> subscribe(Long meetingId, String authorization) {
		Long userId = currentUserIdResolver.resolve(authorization);
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noCache())
				.header("X-Accel-Buffering", "no")
				.header(HttpHeaders.CONNECTION, "keep-alive")
				.body(subscriptionService.subscribe(meetingId, userId));
	}
}
