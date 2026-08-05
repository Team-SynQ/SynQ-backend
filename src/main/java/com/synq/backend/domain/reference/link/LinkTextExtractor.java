package com.synq.backend.domain.reference.link;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 링크 본문에서 사람이 읽는 텍스트만 뽑는다.
 *
 * <p>정적 HTML 만 다룬다. JS 로 본문을 그리는 페이지(Notion, Google Docs)는 빈 껍데기가 나와
 * 최소 길이 조건에 걸리고, 호출자가 READ_FAILED 로 처리한다.
 *
 * <p>비동기 경로에서 호출되므로 예외를 던지지 않고 {@link Optional#empty()} 로 실패를 알린다.
 */
@Slf4j
@Component
public class LinkTextExtractor {

	private static final Set<String> SUPPORTED_TYPES = Set.of("text/html", "text/plain");
	private static final String REMOVED_TAGS =
			"script, style, noscript, nav, footer, header, aside, iframe";

	private final OkHttpClient client;
	private final LinkProperties properties;

	public LinkTextExtractor(
			@Qualifier("linkHttpClient") OkHttpClient linkHttpClient,
			LinkProperties properties
	) {
		this.client = linkHttpClient;
		this.properties = properties;
	}

	public Optional<String> extract(String url) {
		try {
			Request request = new Request.Builder().url(url).get().build();
			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful() || !isSupported(response.header("Content-Type"))) {
					return Optional.empty();
				}
				ResponseBody body = response.body();
				if (body == null) {
					return Optional.empty();
				}
				String text = parse(body, url);
				return text.length() < properties.minTextLength()
						? Optional.empty()
						: Optional.of(text);
			}
		} catch (IOException | RuntimeException e) {
			log.warn("링크 본문 추출 실패. url={}", url, e);
			return Optional.empty();
		}
	}

	private String parse(ResponseBody body, String url) throws IOException {
		// 상한까지만 읽는다. 무한 스트림을 통째로 물면 힙이 터진다.
		// 잘린 HTML 이어도 Jsoup 은 관대하게 파싱하므로 앞부분 본문은 그대로 살아난다.
		byte[] bytes = body.byteStream().readNBytes(Math.toIntExact(properties.maxContentBytes()));
		// charset 을 null 로 주면 Jsoup 이 meta 태그를 보고 스스로 판단한다.
		Document document = Jsoup.parse(new ByteArrayInputStream(bytes), null, url);
		document.select(REMOVED_TAGS).remove();

		String title = document.title().trim();
		String content = document.body() == null ? "" : document.body().text();
		return title.isBlank() ? content.trim() : (title + "\n\n" + content).trim();
	}

	private boolean isSupported(String contentType) {
		if (contentType == null) {
			// 프리플라이트에서 판정을 미룬 경우다. 여기서도 모르면 HTML 로 보고 시도한다.
			return true;
		}
		return SUPPORTED_TYPES.contains(contentType.split(";")[0].trim().toLowerCase(Locale.ROOT));
	}
}
