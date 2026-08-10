package com.synq.backend.domain.ai.client.openai;

import com.synq.backend.domain.ai.summary.application.SummaryProperties;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
// OpenAiClient 의 조건과 반드시 같아야 한다. 어긋나면 클라이언트만 만들어지고 RestClient 가 없어 기동이 깨진다.
@ConditionalOnExpression("'${ai.summary.client:fake}' == 'openai' or '${ai.live-context.client:fake}' == 'openai' or '${ai.assistant.client:fake}' == 'openai' or '${ai.chat.client:fake}' == 'openai'")
public class OpenAiClientConfig {

	@Bean
	public RestClient openAiRestClient(RestClient.Builder builder, OpenAiProperties properties) {
		return createRestClient(builder, properties, properties.timeout());
	}

	@Bean("openAiSummaryRestClient")
	@ConditionalOnProperty(prefix = "ai.summary", name = "client", havingValue = "openai")
	public RestClient openAiSummaryRestClient(
			RestClient.Builder builder,
			OpenAiProperties properties,
			SummaryProperties summaryProperties
	) {
		return createRestClient(builder, properties, summaryProperties.timeout());
	}

	@Bean("openAiSummaryApiClient")
	@ConditionalOnProperty(prefix = "ai.summary", name = "client", havingValue = "openai")
	public OpenAiClient openAiSummaryApiClient(
			@Qualifier("openAiSummaryRestClient") RestClient openAiSummaryRestClient,
			OpenAiProperties properties
	) {
		return new OpenAiClient(openAiSummaryRestClient, properties);
	}

	private RestClient createRestClient(
			RestClient.Builder builder,
			OpenAiProperties properties,
			Duration timeout
	) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(timeout);
		requestFactory.setReadTimeout(timeout);

		return builder
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
