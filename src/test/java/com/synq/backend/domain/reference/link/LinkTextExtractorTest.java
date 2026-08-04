package com.synq.backend.domain.reference.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkTextExtractorTest {

	private static final String BODY_TEXT =
			"회의에서 다룰 내용을 정리한 문서입니다. 이 문단은 최소 길이 조건을 넉넉히 넘기기 위한 본문입니다.";

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
	void 스크립트와_스타일을_걷어내고_본문만_남긴다() {
		enqueueHtml("""
				<html><head><title>설계 문서</title>
				<style>body { color: red; }</style></head>
				<body>
				<nav>메뉴 메뉴 메뉴</nav>
				<p>%s</p>
				<script>alert('xss')</script>
				<footer>푸터 푸터 푸터</footer>
				</body></html>
				""".formatted(BODY_TEXT));

		String text = extractor(50).extract(url()).orElseThrow();

		assertThat(text).contains(BODY_TEXT);
		assertThat(text).doesNotContain("alert");
		assertThat(text).doesNotContain("color: red");
		assertThat(text).doesNotContain("메뉴");
		assertThat(text).doesNotContain("푸터");
	}

	@Test
	void 제목을_본문_앞에_붙인다() {
		// 제목은 문서 주제를 가장 압축적으로 담고 있어 검색 품질에 기여한다.
		enqueueHtml("<html><head><title>설계 문서</title></head><body><p>%s</p></body></html>"
				.formatted(BODY_TEXT));

		String text = extractor(50).extract(url()).orElseThrow();

		assertThat(text).startsWith("설계 문서");
	}

	@Test
	void 본문이_최소_길이보다_짧으면_비어_있다() {
		// JS 렌더링 페이지가 여기서 걸린다. 정적 HTML 에 본문이 없기 때문이다.
		enqueueHtml("<html><head><title>Notion</title></head><body><div id=\"root\"></div></body></html>");

		assertThat(extractor(50).extract(url())).isEmpty();
	}

	@Test
	void HTML_이_아니면_비어_있다() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "application/pdf")
				.setBody("%PDF-1.4 ..."));

		assertThat(extractor(50).extract(url())).isEmpty();
	}

	@Test
	void 응답이_실패면_비어_있다() {
		server.enqueue(new MockResponse().setResponseCode(500));

		assertThat(extractor(50).extract(url())).isEmpty();
	}

	@Test
	void 평문_응답도_처리한다() {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/plain; charset=utf-8")
				.setBody(BODY_TEXT));

		Optional<String> text = extractor(50).extract(url());

		assertThat(text).isPresent();
		assertThat(text.orElseThrow()).contains("최소 길이 조건");
	}

	@Test
	void 도달할_수_없으면_비어_있다() throws IOException {
		String deadUrl = url();
		server.shutdown();

		assertThat(extractor(50).extract(deadUrl)).isEmpty();
	}

	private void enqueueHtml(String html) {
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", "text/html; charset=utf-8")
				.setBody(html));
	}

	private String url() {
		return server.url("/doc").toString();
	}

	private LinkTextExtractor extractor(int minTextLength) {
		LinkProperties properties = new LinkProperties(
				Duration.ofSeconds(2),
				Duration.ofSeconds(2),
				Duration.ofSeconds(3),
				Duration.ofSeconds(3),
				1_000_000L,
				minTextLength,
				true);
		OkHttpClient client = new OkHttpClient.Builder()
				.dns(new SafeDns(true))
				.callTimeout(properties.callTimeout())
				.build();
		return new LinkTextExtractor(client, properties);
	}
}
