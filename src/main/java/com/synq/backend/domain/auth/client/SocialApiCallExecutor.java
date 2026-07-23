package com.synq.backend.domain.auth.client;

import com.synq.backend.domain.auth.code.AuthErrorCode;
import com.synq.backend.global.apipayload.exception.GeneralException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

@Component
public class SocialApiCallExecutor {

	public <T> T call(Supplier<T> request, AuthErrorCode invalidCode, AuthErrorCode unavailableCode) {
		try {
			return request.get();
		} catch (HttpClientErrorException e) {
			throw new GeneralException(invalidCode, e);
		} catch (HttpServerErrorException | ResourceAccessException | HttpMessageNotReadableException e) {
			throw new GeneralException(unavailableCode, e);
		} catch (RestClientException e) {
			throw new GeneralException(invalidCode, e);
		}
	}
}
