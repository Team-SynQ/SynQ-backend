package com.synq.backend.domain.reference.link;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.synq.backend.domain.reference.code.ReferenceErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import java.io.IOException;
import java.time.Duration;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkPreflightCheckerTest {

	private MockWebServer server;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();
	}

	@AfterEach
	void tearDown() throws IOException {
		server.shutdown();
	}

	@Test
	void HTML_응답은_통과시킨다() {
		server.enqueue(html(200));

		assertThatCode(() -> checker(true).check(url())).doesNotThrowAnyException();
	}

	@Test
	void 텍스트_응답도_통과시킨다() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/plain"));

		assertThatCode(() -> checker(true).check(url())).doesNotThrowAnyException();
	}

	@Test
	void HEAD_를_지원하지_않는_응답은_통과시킨다() {
		// 405/501 을 거부하면 HEAD 미구현 서버의 멀쩡한 링크를 반려하게 된다.
		for (int code : new int[]{405, 501}) {
			server.enqueue(new MockResponse().setResponseCode(code));

			assertThatCode(() -> checker(true).check(url())).doesNotThrowAnyException();
		}
	}

	@Test
	void 봇_차단으로_보이는_403_은_통과시킨다() {
		// HEAD 에는 403, GET 에는 200 을 주는 설정이 흔하다. 판정은 비동기 추출에 맡긴다.
		server.enqueue(new MockResponse().setResponseCode(403));

		assertThatCode(() -> checker(true).check(url())).doesNotThrowAnyException();
	}

	@Test
	void Content_Type_이_없으면_통과시킨다() {
		server.enqueue(new MockResponse().setResponseCode(200).removeHeader("Content-Type"));

		assertThatCode(() -> checker(true).check(url())).doesNotThrowAnyException();
	}

	@Test
	void HTML_이_아니면_거부한다() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/pdf"));

		assertError(ReferenceErrorCode.LINK_UNSUPPORTED_CONTENT_TYPE);
	}

	@Test
	void 크기_상한을_넘으면_거부한다() {
		server.enqueue(html(200).setHeader("Content-Length", "99999999"));

		assertError(ReferenceErrorCode.LINK_TOO_LARGE);
	}

	@Test
	void 없는_페이지는_거부한다() {
		server.enqueue(new MockResponse().setResponseCode(404));

		assertError(ReferenceErrorCode.LINK_UNREACHABLE);
	}

	@Test
	void 서버_오류도_거부한다() {
		server.enqueue(new MockResponse().setResponseCode(500));

		assertError(ReferenceErrorCode.LINK_UNREACHABLE);
	}

	@Test
	void 차단된_주소는_ADDRESS_NOT_ALLOWED_다() {
		// MockWebServer 는 루프백에 뜬다. 사설망 허용을 끄면 SafeDns 가 막는다.
		server.enqueue(html(200));

		assertThatThrownBy(() -> checker(false).check(url()))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(ReferenceErrorCode.LINK_ADDRESS_NOT_ALLOWED);
	}

	@Test
	void 파싱할_수_없는_URL_은_거부한다() {
		assertThatThrownBy(() -> checker(true).check("h!tp://broken"))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(ReferenceErrorCode.LINK_UNREACHABLE);
	}

	private void assertError(ReferenceErrorCode expected) {
		assertThatThrownBy(() -> checker(true).check(url()))
				.isInstanceOf(GeneralException.class)
				.extracting("code")
				.isEqualTo(expected);
	}

	private MockResponse html(int code) {
		return new MockResponse().setResponseCode(code)
				.setHeader("Content-Type", "text/html; charset=utf-8");
	}

	private String url() {
		return server.url("/doc").toString();
	}

	private LinkPreflightChecker checker(boolean allowPrivateNetwork) {
		LinkProperties properties = new LinkProperties(
				Duration.ofSeconds(2),
				Duration.ofSeconds(2),
				Duration.ofSeconds(3),
				Duration.ofSeconds(3),
				1_000_000L,
				50,
				allowPrivateNetwork);
		OkHttpClient client = new OkHttpClient.Builder()
				.dns(new SafeDns(allowPrivateNetwork))
				.callTimeout(properties.callTimeout())
				.build();
		return new LinkPreflightChecker(client, properties);
	}
}
