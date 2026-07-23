package com.synq.backend.domain.ai.client.openai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
@ConditionalOnExpression("'${ai.summary.client:fake}' == 'openai' or '${ai.live-context.client:fake}' == 'openai'")
public class OpenAiClientConfig {

	@Bean
	public RestClient openAiRestClient(RestClient.Builder builder, OpenAiProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.timeout());
		requestFactory.setReadTimeout(properties.timeout());

		return builder
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
