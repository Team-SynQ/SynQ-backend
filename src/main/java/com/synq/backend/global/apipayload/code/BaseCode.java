package com.synq.backend.global.apipayload.code;

import org.springframework.http.HttpStatus;

public interface BaseCode {

	HttpStatus getStatus();
	String getCode();
	String getMessage();
}
