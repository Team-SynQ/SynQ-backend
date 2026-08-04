package com.synq.backend.domain.reference.link;

import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 링크 등록 시점의 1차 필터. HEAD 한 번으로 명백한 불량만 거른다.
 *
 * <p>본문 추출까지 동기로 하면 등록이 네트워크 응답만큼 느려지고, 전부 비동기로 미루면 오타 URL 조차
 * 일단 등록되었다가 조용히 실패한다. 그 사이를 잡는다. 애매한 응답은 통과시키고 판정은 비동기 추출에 맡긴다.
 */
@Component
public class LinkPreflightChecker {

	private static final Set<String> SUPPORTED_TYPES = Set.of("text/html", "text/plain");
	// HEAD 미구현(405/501)이거나 봇 차단(403)인 서버가 흔하다. 거부하면 멀쩡한 링크를 반려한다.
	private static final Set<Integer> PASS_THROUGH_CODES = Set.of(403, 405, 501);

	private final OkHttpClient client;
	private final long maxContentBytes;

	public LinkPreflightChecker(
			@Qualifier("linkHttpClient") OkHttpClient linkHttpClient,
			LinkProperties properties
	) {
		// 트랜잭션 안에서 도는 호출이라 본문 fetch 보다 짧게 끊는다.
		// newBuilder 는 커넥션 풀과 디스패처를 공유하므로 새 클라이언트를 만드는 비용이 아니다.
		this.client = linkHttpClient.newBuilder()
				.callTimeout(properties.preflightCallTimeout())
				.build();
		this.maxContentBytes = properties.maxContentBytes();
	}

	public void check(String url) {
		Request request;
		try {
			request = new Request.Builder().url(url).head().build();
		} catch (IllegalArgumentException e) {
			// OkHttp 가 파싱하지 못하는 URL. DTO 검증을 통과해도 여기서 걸릴 수 있다.
			throw new GeneralException(ReferenceErrorCode.LINK_UNREACHABLE);
		}

		try (Response response = client.newCall(request).execute()) {
			if (PASS_THROUGH_CODES.contains(response.code())) {
				return;
			}
			if (!response.isSuccessful()) {
				throw new GeneralException(ReferenceErrorCode.LINK_UNREACHABLE);
			}
			validateContentType(response.header("Content-Type"));
			validateContentLength(response.header("Content-Length"));
		} catch (IOException e) {
			throw new GeneralException(isBlocked(e)
					? ReferenceErrorCode.LINK_ADDRESS_NOT_ALLOWED
					: ReferenceErrorCode.LINK_UNREACHABLE);
		}
	}

	private void validateContentType(String contentType) {
		if (contentType == null) {
			// 헤더가 없으면 판정을 미룬다. 본문을 받아본 뒤 LinkTextExtractor 가 다시 본다.
			return;
		}
		String mediaType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
		if (!SUPPORTED_TYPES.contains(mediaType)) {
			throw new GeneralException(ReferenceErrorCode.LINK_UNSUPPORTED_CONTENT_TYPE);
		}
	}

	private void validateContentLength(String contentLength) {
		if (contentLength == null) {
			// 청크 전송이면 헤더가 없다. 비동기 추출이 스트리밍 중에 상한으로 자른다.
			return;
		}
		long length;
		try {
			length = Long.parseLong(contentLength.trim());
		} catch (NumberFormatException e) {
			// 망가진 헤더 하나로 멀쩡한 링크를 막지 않는다.
			return;
		}
		if (length > maxContentBytes) {
			throw new GeneralException(ReferenceErrorCode.LINK_TOO_LARGE);
		}
	}

	/** SafeDns 의 차단인지 단순 도달 불가인지 가른다. OkHttp 가 예외를 감쌀 수 있어 사슬을 훑는다. */
	private boolean isBlocked(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof BlockedAddressException) {
				return true;
			}
			if (current.getCause() == current) {
				return false;
			}
			current = current.getCause();
		}
		return false;
	}
}
